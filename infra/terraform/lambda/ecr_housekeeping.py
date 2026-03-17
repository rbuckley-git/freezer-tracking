import json
import logging
import os
from typing import Dict, Iterable, List, Set, Tuple

import boto3


LOGGER = logging.getLogger()
LOGGER.setLevel(os.getenv("LOG_LEVEL", "INFO"))

ECR_CLIENT = boto3.client("ecr")
ECS_CLIENT = boto3.client("ecs")


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y", "on"}


def _chunks(items: List[str], size: int) -> Iterable[List[str]]:
    for i in range(0, len(items), size):
        yield items[i : i + size]


def _target_repositories() -> List[str]:
    raw = os.getenv("REPOSITORIES", "")
    repos = [repo.strip() for repo in raw.split(",") if repo.strip()]
    if not repos:
        raise ValueError("REPOSITORIES environment variable is empty")
    return repos


def _repo_uris_by_name(repositories: List[str]) -> Dict[str, str]:
    repo_uris = {}
    for batch in _chunks(repositories, 100):
        response = ECR_CLIENT.describe_repositories(repositoryNames=batch)
        for repo in response.get("repositories", []):
            repo_uris[repo["repositoryName"]] = repo["repositoryUri"]
    return repo_uris


def _active_task_definition_arns() -> List[str]:
    arns = []
    paginator = ECS_CLIENT.get_paginator("list_task_definitions")
    for page in paginator.paginate(status="ACTIVE", sort="DESC"):
        arns.extend(page.get("taskDefinitionArns", []))
    LOGGER.debug("Discovered %s active ECS task definitions", len(arns))
    return arns


def _used_images(repo_uri_by_name: Dict[str, str]) -> Dict[str, Tuple[Set[str], Set[str]]]:
    by_repo_name: Dict[str, Tuple[Set[str], Set[str]]] = {
        name: (set(), set()) for name in repo_uri_by_name
    }
    repo_uri_lookup = {uri: name for name, uri in repo_uri_by_name.items()}

    for task_definition_arn in _active_task_definition_arns():
        description = ECS_CLIENT.describe_task_definition(taskDefinition=task_definition_arn)
        for container in description["taskDefinition"].get("containerDefinitions", []):
            image = container.get("image", "")
            matched_repo_name = None
            for repo_uri, repo_name in repo_uri_lookup.items():
                if image.startswith(repo_uri):
                    matched_repo_name = repo_name
                    break
            if not matched_repo_name:
                continue

            used_digests, used_tags = by_repo_name[matched_repo_name]
            image_ref = image[len(repo_uri_by_name[matched_repo_name]) :]
            if image_ref.startswith("@"):
                used_digests.add(image_ref[1:])
            elif image_ref.startswith(":"):
                used_tags.add(image_ref[1:])

    for repo_name, (digests, tags) in by_repo_name.items():
        LOGGER.debug(
            "Repository %s has %s referenced digests and %s referenced tags from active ECS task definitions",
            repo_name,
            len(digests),
            len(tags),
        )

    return by_repo_name


def _describe_all_image_details(repository_name: str) -> List[dict]:
    all_ids = []
    paginator = ECR_CLIENT.get_paginator("list_images")
    for page in paginator.paginate(repositoryName=repository_name):
        all_ids.extend(page.get("imageIds", []))

    if not all_ids:
        return []

    digests = sorted({img["imageDigest"] for img in all_ids if "imageDigest" in img})

    details = []
    for batch in _chunks(digests, 100):
        response = ECR_CLIENT.describe_images(
            repositoryName=repository_name,
            imageIds=[{"imageDigest": digest} for digest in batch],
        )
        details.extend(response.get("imageDetails", []))
    LOGGER.debug("Repository %s has %s total image digests in ECR", repository_name, len(details))
    return details


def _digests_to_delete(
    image_details: List[dict], used_digests: Set[str], used_tags: Set[str], keep_count: int
) -> List[str]:
    keep_count = max(0, keep_count)
    unreferenced = []

    for detail in image_details:
        digest = detail["imageDigest"]
        tags = set(detail.get("imageTags", []))
        is_referenced = digest in used_digests or bool(tags & used_tags)
        if not is_referenced:
            unreferenced.append(detail)

    unreferenced.sort(
        key=lambda item: item.get("imagePushedAt").timestamp()
        if item.get("imagePushedAt")
        else 0,
        reverse=True,
    )
    to_delete = unreferenced[keep_count:]
    return [item["imageDigest"] for item in to_delete]


def _delete_digests(repository_name: str, digests: List[str], dry_run: bool) -> int:
    if not digests:
        return 0

    if dry_run:
        LOGGER.debug(
            "Dry run enabled: would delete %s digests from %s. Digests: %s",
            len(digests),
            repository_name,
            digests,
        )
        return len(digests)

    deleted = 0
    for batch in _chunks(digests, 100):
        LOGGER.debug(
            "Deleting %s digests from %s in this batch",
            len(batch),
            repository_name,
        )
        response = ECR_CLIENT.batch_delete_image(
            repositoryName=repository_name,
            imageIds=[{"imageDigest": digest} for digest in batch],
        )
        failures = response.get("failures", [])
        if failures:
            LOGGER.warning("Delete failures for %s: %s", repository_name, failures)
        deleted += len(response.get("imageIds", []))
    return deleted


def lambda_handler(_event, _context):
    repositories = _target_repositories()
    keep_count = int(os.getenv("KEEP_UNREFERENCED", "0"))
    dry_run = _env_bool("DRY_RUN", default=False)
    LOGGER.info(
        "Starting ECR housekeeping. repositories=%s, keep_unreferenced=%s, dry_run=%s",
        repositories,
        keep_count,
        dry_run,
    )

    repo_uri_by_name = _repo_uris_by_name(repositories)
    used_by_repo = _used_images(repo_uri_by_name)

    results = []
    total_deleted = 0
    for repository in repositories:
        used_digests, used_tags = used_by_repo.get(repository, (set(), set()))
        image_details = _describe_all_image_details(repository)
        digests = _digests_to_delete(image_details, used_digests, used_tags, keep_count)
        LOGGER.debug(
            "Repository %s has %s deletion candidates after keep_unreferenced=%s",
            repository,
            len(digests),
            keep_count,
        )
        deleted_count = _delete_digests(repository, digests, dry_run)
        total_deleted += deleted_count
        results.append(
            {
                "repository": repository,
                "totalImages": len(image_details),
                "usedDigests": len(used_digests),
                "usedTags": len(used_tags),
                "deleted": deleted_count,
            }
        )

    payload = {"dryRun": dry_run, "deletedTotal": total_deleted, "repositories": results}
    LOGGER.info("ECR housekeeping complete: %s", json.dumps(payload))
    return payload

resource "aws_ecr_repository" "api" {
  name                 = "${var.project_name}-api"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "web" {
  name                 = "${var.project_name}-web"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

locals {
  ecr_pull_principals = [
    aws_iam_role.ecs_task_execution_role.arn
  ]
}

data "aws_iam_policy_document" "api_ecr_pull" {
  statement {
    sid    = "AllowCrossAccountPull"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = local.ecr_pull_principals
    }

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer"
    ]
  }
}

data "aws_iam_policy_document" "web_ecr_pull" {
  statement {
    sid    = "AllowCrossAccountPull"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = local.ecr_pull_principals
    }

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer"
    ]
  }
}

resource "aws_ecr_repository_policy" "api_pull_access" {
  repository = aws_ecr_repository.api.name
  policy     = data.aws_iam_policy_document.api_ecr_pull.json
}

resource "aws_ecr_repository_policy" "web_pull_access" {
  repository = aws_ecr_repository.web.name
  policy     = data.aws_iam_policy_document.web_ecr_pull.json
}

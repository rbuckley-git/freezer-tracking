# Default region is eu-west-2; set aws_region only if you need to override it.
# api_image_tag and web_image_tag select tags from Terraform-managed ECR repositories.

# Optional overrides
# project_name        = "freezer-tracking"
# root_domain         = "learnsharegrow.io"
# web_subdomain       = "freezer"
# api_subdomain       = "freezer-api"
# ec2_instance_type   = "t4g.micro"
# db_instance_class   = "db.t4g.medium"
api_health_check_path       = "/health"
enable_https_only_listeners = true
# alb_https_security_policy = "ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09"
web_api_base_url    = "https://freezer-api.learnsharegrow.io"
api_image_tag       = "3.0.0"
web_image_tag       = "3.0.0"
enable_rds_instance = false
enable_rds_cluster  = false

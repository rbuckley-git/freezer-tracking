variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-west-2"
}

variable "project_name" {
  description = "Project prefix for resource names"
  type        = string
  default     = "freezer-tracking"
}

variable "root_domain" {
  description = "Root DNS domain for public service hostnames."
  type        = string
  default     = "learnsharegrow.io"
}

variable "web_subdomain" {
  description = "Web hostname label under the root domain."
  type        = string
  default     = "freezer"
}

variable "api_subdomain" {
  description = "API hostname label under the root domain."
  type        = string
  default     = "freezer-api"
}

variable "enable_https_only_listeners" {
  description = "Deprecated toggle retained for compatibility. The public ALB is now always HTTPS-only on port 443."
  type        = bool
  default     = true
}

variable "alb_https_security_policy" {
  description = "SSL policy for public ALB HTTPS listeners."
  type        = string
  default     = "ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09"
}

variable "enable_alb_waf" {
  description = "When true, attaches a WAFv2 Web ACL to the public ALB."
  type        = bool
  default     = true
}

variable "waf_rate_limit_per_5m" {
  description = "Per-IP request limit over 5 minutes for the WAF rate-based block rule."
  type        = number
  default     = 2000
}

variable "ecr_housekeeping_schedule_expression" {
  description = "EventBridge schedule expression for ECR housekeeping Lambda (for example: rate(1 day))."
  type        = string
  default     = "rate(1 day)"
}

variable "ecr_housekeeping_keep_unreferenced" {
  description = "Number of newest unreferenced images to keep per ECR repository for rollback convenience."
  type        = number
  default     = 2
}

variable "ecr_housekeeping_dry_run" {
  description = "When true, ECR housekeeping logs candidates but does not delete images."
  type        = bool
  default     = false
}

variable "ecr_housekeeping_log_level" {
  description = "Log level for ECR housekeeping Lambda."
  type        = string
  default     = "INFO"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the primary public subnet"
  type        = string
  default     = "10.20.0.0/24"
}

variable "public_subnet_cidr_2" {
  description = "CIDR block for the secondary public subnet (required by ALB)"
  type        = string
  default     = "10.20.3.0/24"
}

variable "private_subnet_cidr_1" {
  description = "CIDR block for private subnet in primary AZ"
  type        = string
  default     = "10.20.1.0/24"
}

variable "private_subnet_cidr_2" {
  description = "CIDR block for private subnet in secondary AZ (required by RDS subnet groups)"
  type        = string
  default     = "10.20.2.0/24"
}

variable "ec2_instance_type" {
  description = "EC2 instance type for ECS capacity"
  type        = string
  default     = "t4g.small"
}

variable "api_image_tag" {
  description = "Image tag for the API service in the Terraform-managed API ECR repository."
  type        = string
  default     = "latest"
}

variable "web_image_tag" {
  description = "Image tag for the web service in the Terraform-managed web ECR repository."
  type        = string
  default     = "latest"
}

variable "api_container_port" {
  description = "Container port for the API"
  type        = number
  default     = 9080
}

variable "web_container_port" {
  description = "Container port for the web app"
  type        = number
  default     = 10000
}

variable "api_container_memory_reservation" {
  description = "Soft memory reservation (MiB) for the API container"
  type        = number
  default     = 256
}

variable "web_container_memory_reservation" {
  description = "Soft memory reservation (MiB) for the web container"
  type        = number
  default     = 256
}

variable "web_api_base_url" {
  description = "Base URL used by the web container for server-side API calls. If null, uses the API ALB URL."
  type        = string
  default     = null
  nullable    = true
}

variable "api_desired_count" {
  description = "Number of API tasks"
  type        = number
  default     = 1
}

variable "web_desired_count" {
  description = "Number of web tasks"
  type        = number
  default     = 1
}

variable "api_health_check_path" {
  description = "ALB health check path for API"
  type        = string
  default     = "/"
}

variable "web_health_check_path" {
  description = "ALB health check path for the web service"
  type        = string
  default     = "/"
}

variable "db_backup_retention_period" {
  description = "Number of days to retain automated Aurora backups."
  type        = number
  default     = 7
}

variable "db_deletion_protection" {
  description = "When true, prevent accidental Aurora cluster deletion."
  type        = bool
  default     = true
}

variable "db_skip_final_snapshot" {
  description = "When true, skip final Aurora snapshot at destroy time."
  type        = bool
  default     = false
}

variable "db_final_snapshot_identifier" {
  description = "Final snapshot identifier used when db_skip_final_snapshot is false."
  type        = string
  default     = "freezer-tracking-final"
}

variable "db_apply_immediately" {
  description = "Whether Aurora modifications are applied immediately."
  type        = bool
  default     = false
}

variable "db_name" {
  description = "Postgres database name"
  type        = string
  default     = "freezertracking"
}

variable "db_username" {
  description = "Postgres admin username"
  type        = string
  default     = "freezer_admin"
}

variable "db_serverless_min_acu" {
  description = "Minimum Aurora Serverless v2 capacity in ACUs."
  type        = number
  default     = 0.5

  validation {
    condition     = var.db_serverless_min_acu >= 0
    error_message = "db_serverless_min_acu must be at least 0."
  }
}

variable "db_serverless_max_acu" {
  description = "Maximum Aurora Serverless v2 capacity in ACUs."
  type        = number
  default     = 2

  validation {
    condition     = var.db_serverless_max_acu >= var.db_serverless_min_acu
    error_message = "db_serverless_max_acu must be greater than or equal to db_serverless_min_acu."
  }
}

variable "enable_rds_instance" {
  description = "When false, removes Aurora DB compute instance(s) while retaining cluster storage and data."
  type        = bool
  default     = true
}

variable "enable_rds_cluster" {
  description = "When false, removes Aurora cluster resources entirely."
  type        = bool
  default     = true
}

variable "postgres_ec2_instance_type" {
  description = "EC2 instance type for the standalone PostgreSQL host."
  type        = string
  default     = "t4g.small"
}

variable "postgres_ec2_data_volume_size_gb" {
  description = "Size in GB of the persistent EBS data volume used for PostgreSQL data."
  type        = number
  default     = 100
}

variable "postgres_ec2_data_volume_type" {
  description = "EBS volume type used for PostgreSQL data."
  type        = string
  default     = "gp3"
}

variable "postgres_ec2_allowed_cidrs" {
  description = "Additional CIDR blocks allowed to connect to PostgreSQL on port 5432."
  type        = list(string)
  default     = []
}

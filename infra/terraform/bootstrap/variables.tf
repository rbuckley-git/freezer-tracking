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

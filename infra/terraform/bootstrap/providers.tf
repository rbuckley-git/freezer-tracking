terraform {
  required_version = "~> 1.14.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "= 6.27.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project      = var.project_name
      "Managed by" = "terraform"
    }
  }
}

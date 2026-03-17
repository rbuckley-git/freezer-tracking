output "backend_config_snippet" {
  description = "Backend config values for the main Terraform stack"
  value = {
    region       = var.aws_region
    bucket       = aws_s3_bucket.terraform_state.id
    key          = "freezer-tracking/terraform.tfstate"
    use_lockfile = true
  }
}

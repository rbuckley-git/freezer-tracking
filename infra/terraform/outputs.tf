output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = [aws_subnet.public.id, aws_subnet.public_2.id]
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

output "api_alb_dns_name" {
  description = "Public DNS name for the shared ALB (API hostname routes here)."
  value       = aws_lb.public.dns_name
}

output "api_url" {
  description = "Public API URL."
  value       = "https://${var.api_subdomain}.${var.root_domain}"
}

output "web_alb_dns_name" {
  description = "Public DNS name for the shared public ALB"
  value       = aws_lb.public.dns_name
}

output "web_url" {
  description = "Public web URL."
  value       = "https://${var.web_subdomain}.${var.root_domain}"
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_task_execution_role_arn" {
  description = "IAM role ARN used by ECS tasks to pull images and read configured secrets"
  value       = aws_iam_role.ecs_task_execution_role.arn
}

output "db_endpoint" {
  description = "Aurora PostgreSQL writer endpoint"
  value       = var.enable_rds_cluster ? aws_rds_cluster.main[0].endpoint : null
}

output "postgres_ec2_instance_id" {
  description = "Instance ID of the standalone PostgreSQL EC2 host."
  value       = aws_instance.postgres_ec2.id
}

output "postgres_ec2_private_ip" {
  description = "Private IP address of the standalone PostgreSQL EC2 host."
  value       = aws_instance.postgres_ec2.private_ip
}

output "postgres_ec2_connection_host" {
  description = "Hostname/IP to use when connecting to the standalone PostgreSQL host from within the VPC."
  value       = aws_instance.postgres_ec2.private_ip
}

output "postgres_ec2_data_volume_id" {
  description = "EBS volume ID used as the standalone PostgreSQL data volume."
  value       = aws_ebs_volume.postgres_data.id
}

output "db_password_secret_arn" {
  description = "Secrets Manager ARN storing the generated database password"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "api_password_pepper_secret_arn" {
  description = "Secrets Manager ARN storing the generated API password pepper"
  value       = aws_secretsmanager_secret.api_password_pepper.arn
}

output "web_cookie_key_secret_arn" {
  description = "Secrets Manager ARN storing the generated web cookie encryption key"
  value       = aws_secretsmanager_secret.web_cookie_key.arn
}

output "api_ecr_repository_url" {
  description = "ECR repository URL for the API image"
  value       = aws_ecr_repository.api.repository_url
}

output "web_ecr_repository_url" {
  description = "ECR repository URL for the web image"
  value       = aws_ecr_repository.web.repository_url
}

output "cloudflare_service_cnames" {
  description = "CNAME records to create in Cloudflare for public hostnames."
  value = [
    "${var.web_subdomain}.${var.root_domain} CNAME ${aws_lb.public.dns_name}",
    "${var.api_subdomain}.${var.root_domain} CNAME ${aws_lb.public.dns_name}"
  ]
}

output "cloudflare_acm_validation_cnames" {
  description = "ACM DNS validation CNAME records to create in Cloudflare."
  value = [
    for option in aws_acm_certificate.public_services.domain_validation_options : {
      domain_name = option.domain_name
      name        = option.resource_record_name
      type        = option.resource_record_type
      value       = option.resource_record_value
    }
  ]
}

output "public_alb_waf_web_acl_arn" {
  description = "ARN of the shared WAFv2 Web ACL attached to public ALBs (null when disabled)."
  value       = length(aws_wafv2_web_acl.public_albs) > 0 ? aws_wafv2_web_acl.public_albs[0].arn : null
}

output "ecr_housekeeping_lambda_name" {
  description = "Name of the ECR housekeeping Lambda."
  value       = aws_lambda_function.ecr_housekeeping.function_name
}

output "ecr_housekeeping_event_rule_name" {
  description = "EventBridge rule name for ECR housekeeping schedule."
  value       = aws_cloudwatch_event_rule.ecr_housekeeping_daily.name
}

locals {
  create_public_alb_waf = var.enable_alb_waf
}

resource "aws_wafv2_web_acl" "public_albs" {
  count = local.create_public_alb_waf ? 1 : 0

  name  = "${var.project_name}-public-albs"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${replace(var.project_name, "-", "")}PublicAlbsWaf"
    sampled_requests_enabled   = true
  }

  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 10

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AWSManagedRulesCommonRuleSet"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 20

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AWSManagedRulesKnownBadInputsRuleSet"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "AWSManagedRulesAmazonIpReputationList"
    priority = 30

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AWSManagedRulesAmazonIpReputationList"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "IpRateLimit"
    priority = 40

    action {
      block {}
    }

    statement {
      rate_based_statement {
        aggregate_key_type = "IP"
        limit              = var.waf_rate_limit_per_5m
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "IpRateLimit"
      sampled_requests_enabled   = true
    }
  }

  tags = {
    Name = "${var.project_name}-public-albs-waf"
  }
}

resource "aws_wafv2_web_acl_association" "public_alb" {
  count = local.create_public_alb_waf ? 1 : 0

  resource_arn = aws_lb.public.arn
  web_acl_arn  = aws_wafv2_web_acl.public_albs[0].arn
}

resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  tags = {
    Name = "${var.project_name}-cluster"
  }
}

locals {
  api_image_uri = "${aws_ecr_repository.api.repository_url}:${var.api_image_tag}"
  web_image_uri = "${aws_ecr_repository.web.repository_url}:${var.web_image_tag}"
  web_domain    = "${var.web_subdomain}.${var.root_domain}"
  api_domain    = "${var.api_subdomain}.${var.root_domain}"
  web_api_base_url_resolved = coalesce(
    var.web_api_base_url,
    "https://${local.api_domain}"
  )
}

moved {
  from = aws_lb.web
  to   = aws_lb.public
}

moved {
  from = aws_lb_listener.web_https
  to   = aws_lb_listener.public_https
}

moved {
  from = aws_lb_listener_rule.web_https_api_host
  to   = aws_lb_listener_rule.public_https_api_host
}

resource "aws_acm_certificate" "public_services" {
  domain_name               = local.web_domain
  subject_alternative_names = [local.api_domain]
  validation_method         = "DNS"
}

resource "aws_iam_role" "ecs_instance_role" {
  name = "${var.project_name}-ecs-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_instance_role_policy" {
  role       = aws_iam_role.ecs_instance_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_role_policy_attachment" "ecs_instance_role_ssm_policy" {
  role       = aws_iam_role.ecs_instance_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ecs" {
  name = "${var.project_name}-ecs-instance-profile"
  role = aws_iam_role.ecs_instance_role.name
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.project_name}-ecs-task-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_task_secrets_policy" {
  name = "${var.project_name}-ecs-task-secrets-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          aws_secretsmanager_secret.db_password.arn,
          aws_secretsmanager_secret.api_password_pepper.arn,
          aws_secretsmanager_secret.web_cookie_key.arn
        ]
      }
    ]
  })
}

data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/arm64/recommended/image_id"
}

resource "aws_launch_template" "ecs" {
  name_prefix   = "${var.project_name}-ecs-"
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = var.ec2_instance_type

  iam_instance_profile {
    name = aws_iam_instance_profile.ecs.name
  }

  vpc_security_group_ids = [aws_security_group.ecs.id]

  user_data = base64encode(<<-EOT
    #!/bin/bash
    echo ECS_CLUSTER=${aws_ecs_cluster.main.name} >> /etc/ecs/ecs.config
  EOT
  )

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.project_name}-ecs-host"
    }
  }

  tag_specifications {
    resource_type = "volume"

    tags = {
      Name = "${var.project_name}-ecs-host-root"
    }
  }
}

resource "aws_autoscaling_group" "ecs" {
  name                      = "${var.project_name}-ecs-asg"
  max_size                  = 1
  min_size                  = 1
  desired_capacity          = 1
  health_check_type         = "EC2"
  vpc_zone_identifier       = [aws_subnet.private_1.id, aws_subnet.private_2.id]
  protect_from_scale_in     = false
  force_delete              = true
  wait_for_capacity_timeout = "10m"

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${var.project_name}-ecs-host"
    propagate_at_launch = true
  }

  tag {
    key                 = "Project"
    value               = var.project_name
    propagate_at_launch = true
  }
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${var.project_name}/api"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "web" {
  name              = "/ecs/${var.project_name}/web"
  retention_in_days = 14
}

resource "aws_lb_target_group" "api" {
  name        = substr(replace("${var.project_name}-api-tg", "_", "-"), 0, 32)
  port        = var.api_container_port
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = aws_vpc.main.id

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
    path                = var.api_health_check_path
    matcher             = "200-399"
  }
}

resource "aws_lb" "public" {
  name               = substr(replace("${var.project_name}-public-alb", "_", "-"), 0, 32)
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.public_alb.id]
  subnets            = [aws_subnet.public.id, aws_subnet.public_2.id]

  tags = {
    Name = "${var.project_name}-public-alb"
  }
}

resource "aws_lb_target_group" "web" {
  name        = substr(replace("${var.project_name}-web-tg", "_", "-"), 0, 32)
  port        = var.web_container_port
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = aws_vpc.main.id

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
    path                = var.web_health_check_path
    matcher             = "200-399"
  }
}

resource "aws_lb_listener" "public_https" {
  load_balancer_arn = aws_lb.public.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = var.alb_https_security_policy
  certificate_arn   = aws_acm_certificate.public_services.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.web.arn
  }
}

resource "aws_lb_listener_rule" "public_https_api_host" {
  listener_arn = aws_lb_listener.public_https.arn
  priority     = 100

  condition {
    host_header {
      values = [local.api_domain]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

resource "aws_ecs_task_definition" "api" {
  family                   = "${var.project_name}-api"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name                   = "api"
      image                  = local.api_image_uri
      essential              = true
      privileged             = false
      readonlyRootFilesystem = true
      memoryReservation      = var.api_container_memory_reservation
      linuxParameters = {
        capabilities = {
          add  = []
          drop = ["ALL"]
        }
        tmpfs = [
          {
            containerPath = "/tmp"
            size          = 64
            mountOptions  = ["rw", "noexec", "nosuid", "nodev"]
          }
        ]
      }
      mountPoints    = []
      systemControls = []
      volumesFrom    = []
      portMappings = [
        {
          containerPort = var.api_container_port
          hostPort      = 0
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.api.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${aws_instance.postgres_ec2.private_ip}:5432/${var.db_name}"
        },
        {
          name  = "SPRING_DATASOURCE_USERNAME"
          value = var.db_username
        }
      ]
      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = aws_secretsmanager_secret.db_password.arn
        },
        {
          name      = "PASSWORD_PEPPER"
          valueFrom = aws_secretsmanager_secret.api_password_pepper.arn
        }
      ]
    }
  ])
}

resource "aws_ecs_task_definition" "api_bootstrap" {
  family                   = "${var.project_name}-api-bootstrap"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name                   = "api"
      image                  = local.api_image_uri
      essential              = true
      privileged             = false
      readonlyRootFilesystem = true
      memoryReservation      = var.api_container_memory_reservation
      linuxParameters = {
        capabilities = {
          add  = []
          drop = ["ALL"]
        }
        tmpfs = [
          {
            containerPath = "/tmp"
            size          = 64
            mountOptions  = ["rw", "noexec", "nosuid", "nodev"]
          }
        ]
      }
      mountPoints    = []
      portMappings   = []
      systemControls = []
      volumesFrom    = []
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.api.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs-bootstrap"
        }
      }
      environment = [
        {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql://${aws_instance.postgres_ec2.private_ip}:5432/${var.db_name}"
        },
        {
          name  = "SPRING_DATASOURCE_USERNAME"
          value = var.db_username
        }
      ]
      secrets = [
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = aws_secretsmanager_secret.db_password.arn
        },
        {
          name      = "PASSWORD_PEPPER"
          valueFrom = aws_secretsmanager_secret.api_password_pepper.arn
        }
      ]
    }
  ])
}

resource "aws_ecs_task_definition" "web" {
  family                   = "${var.project_name}-web"
  requires_compatibilities = ["EC2"]
  network_mode             = "bridge"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name                   = "web"
      image                  = local.web_image_uri
      essential              = true
      privileged             = false
      readonlyRootFilesystem = true
      memoryReservation      = var.web_container_memory_reservation
      linuxParameters = {
        capabilities = {
          add  = []
          drop = ["ALL"]
        }
        tmpfs = [
          {
            containerPath = "/tmp"
            size          = 64
            mountOptions  = ["rw", "noexec", "nosuid", "nodev"]
          }
        ]
      }
      mountPoints    = []
      systemControls = []
      volumesFrom    = []
      portMappings = [
        {
          containerPort = var.web_container_port
          hostPort      = 0
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.web.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        {
          name  = "API_BASE_URL"
          value = local.web_api_base_url_resolved
        }
      ]
      secrets = [
        {
          name      = "COOKIE_KEY"
          valueFrom = aws_secretsmanager_secret.web_cookie_key.arn
        }
      ]
    }
  ])
}

resource "aws_ecs_service" "api" {
  name                               = "${var.project_name}-api"
  cluster                            = aws_ecs_cluster.main.id
  task_definition                    = aws_ecs_task_definition.api.arn
  desired_count                      = var.api_desired_count
  launch_type                        = "EC2"
  health_check_grace_period_seconds  = 120
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = var.api_container_port
  }

  depends_on = [
    aws_autoscaling_group.ecs
  ]
}

resource "aws_ecs_service" "web" {
  name                               = "${var.project_name}-web"
  cluster                            = aws_ecs_cluster.main.id
  task_definition                    = aws_ecs_task_definition.web.arn
  desired_count                      = var.web_desired_count
  launch_type                        = "EC2"
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  load_balancer {
    target_group_arn = aws_lb_target_group.web.arn
    container_name   = "web"
    container_port   = var.web_container_port
  }

  depends_on = [aws_autoscaling_group.ecs]
}

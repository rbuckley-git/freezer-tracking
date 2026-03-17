locals {
  ecr_housekeeping_repositories = [
    aws_ecr_repository.api.name,
    aws_ecr_repository.web.name
  ]
}

data "archive_file" "ecr_housekeeping" {
  type        = "zip"
  source_file = "${path.module}/lambda/ecr_housekeeping.py"
  output_path = "${path.module}/lambda/ecr_housekeeping.zip"
}

resource "aws_iam_role" "ecr_housekeeping_lambda" {
  name = "${var.project_name}-ecr-housekeeping-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "ecr_housekeeping_lambda" {
  name = "${var.project_name}-ecr-housekeeping-lambda-policy"
  role = aws_iam_role.ecr_housekeeping_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = [
          aws_cloudwatch_log_group.ecr_housekeeping.arn,
          "${aws_cloudwatch_log_group.ecr_housekeeping.arn}:*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ecs:ListTaskDefinitions",
          "ecs:DescribeTaskDefinition"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:DescribeRepositories",
          "ecr:ListImages",
          "ecr:DescribeImages",
          "ecr:BatchDeleteImage"
        ]
        Resource = [
          aws_ecr_repository.api.arn,
          aws_ecr_repository.web.arn
        ]
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "ecr_housekeeping" {
  name              = "/aws/lambda/${var.project_name}-ecr-housekeeping"
  retention_in_days = 14
}

resource "aws_lambda_function" "ecr_housekeeping" {
  function_name    = "${var.project_name}-ecr-housekeeping"
  role             = aws_iam_role.ecr_housekeeping_lambda.arn
  filename         = data.archive_file.ecr_housekeeping.output_path
  source_code_hash = data.archive_file.ecr_housekeeping.output_base64sha256
  handler          = "ecr_housekeeping.lambda_handler"
  runtime          = "python3.12"
  timeout          = 300

  environment {
    variables = {
      REPOSITORIES      = join(",", local.ecr_housekeeping_repositories)
      KEEP_UNREFERENCED = tostring(var.ecr_housekeeping_keep_unreferenced)
      DRY_RUN           = tostring(var.ecr_housekeeping_dry_run)
      LOG_LEVEL         = var.ecr_housekeeping_log_level
    }
  }

  depends_on = [aws_cloudwatch_log_group.ecr_housekeeping]
}

resource "aws_cloudwatch_event_rule" "ecr_housekeeping_daily" {
  name                = "${var.project_name}-ecr-housekeeping-daily"
  description         = "Daily schedule for ECR housekeeping Lambda."
  schedule_expression = var.ecr_housekeeping_schedule_expression
}

resource "aws_cloudwatch_event_target" "ecr_housekeeping_daily" {
  rule      = aws_cloudwatch_event_rule.ecr_housekeeping_daily.name
  target_id = "ecr-housekeeping-lambda"
  arn       = aws_lambda_function.ecr_housekeeping.arn
}

resource "aws_lambda_permission" "allow_eventbridge_ecr_housekeeping" {
  statement_id  = "AllowExecutionFromEventBridgeEcrHousekeeping"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ecr_housekeeping.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.ecr_housekeeping_daily.arn
}

resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "random_string" "web_cookie_key" {
  length  = 16
  special = false
  upper   = true
  lower   = true
  numeric = true
}

resource "random_string" "api_password_pepper" {
  length  = 32
  special = false
  upper   = true
  lower   = true
  numeric = true
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.project_name}/database/password"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret" "web_cookie_key" {
  name                    = "${var.project_name}/web/cookie-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret" "api_password_pepper" {
  name                    = "${var.project_name}/api/password-pepper"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db_password.result
}

resource "aws_secretsmanager_secret_version" "web_cookie_key" {
  secret_id     = aws_secretsmanager_secret.web_cookie_key.id
  secret_string = random_string.web_cookie_key.result
}

resource "aws_secretsmanager_secret_version" "api_password_pepper" {
  secret_id     = aws_secretsmanager_secret.api_password_pepper.id
  secret_string = random_string.api_password_pepper.result
}

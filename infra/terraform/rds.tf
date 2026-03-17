resource "aws_db_subnet_group" "main" {
  count      = var.enable_rds_cluster ? 1 : 0
  name       = "${var.project_name}-db-subnets"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]

  tags = {
    Name = "${var.project_name}-db-subnets"
  }
}

resource "aws_rds_cluster" "main" {
  count                  = var.enable_rds_cluster ? 1 : 0
  cluster_identifier     = "${var.project_name}-aurora-postgres"
  engine                 = "aurora-postgresql"
  database_name          = var.db_name
  master_username        = var.db_username
  master_password        = random_password.db_password.result
  db_subnet_group_name   = aws_db_subnet_group.main[0].name
  vpc_security_group_ids = [aws_security_group.rds.id]
  storage_encrypted      = true

  backup_retention_period   = var.db_backup_retention_period
  deletion_protection       = var.db_deletion_protection
  skip_final_snapshot       = var.db_skip_final_snapshot
  final_snapshot_identifier = var.db_skip_final_snapshot ? null : var.db_final_snapshot_identifier
  apply_immediately         = var.db_apply_immediately

  serverlessv2_scaling_configuration {
    min_capacity = var.db_serverless_min_acu
    max_capacity = var.db_serverless_max_acu
  }

  tags = {
    Name = "${var.project_name}-aurora-postgres"
  }
}

resource "aws_rds_cluster_instance" "main" {
  count                = var.enable_rds_cluster && var.enable_rds_instance ? 1 : 0
  identifier           = "${var.project_name}-aurora-postgres-1"
  cluster_identifier   = aws_rds_cluster.main[0].id
  instance_class       = "db.serverless"
  engine               = aws_rds_cluster.main[0].engine
  db_subnet_group_name = aws_db_subnet_group.main[0].name
  publicly_accessible  = false
  apply_immediately    = var.db_apply_immediately

  tags = {
    Name = "${var.project_name}-aurora-postgres-1"
  }
}

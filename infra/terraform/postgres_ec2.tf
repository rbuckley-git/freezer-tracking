resource "aws_iam_role" "postgres_ec2_instance_role" {
  name = "${var.project_name}-postgres-ec2-role"

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

resource "aws_iam_role_policy_attachment" "postgres_ec2_ssm_policy" {
  role       = aws_iam_role.postgres_ec2_instance_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "postgres_ec2_secrets_policy" {
  name = "${var.project_name}-postgres-ec2-secrets-policy"
  role = aws_iam_role.postgres_ec2_instance_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          aws_secretsmanager_secret.db_password.arn
        ]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "postgres_ec2" {
  name = "${var.project_name}-postgres-ec2-profile"
  role = aws_iam_role.postgres_ec2_instance_role.name
}

locals {
  rds_source_host = var.enable_rds_cluster ? aws_rds_cluster.main[0].endpoint : ""
  rds_source_port = var.enable_rds_cluster ? aws_rds_cluster.main[0].port : 5432
}

resource "aws_ebs_volume" "postgres_data" {
  availability_zone = aws_subnet.private_1.availability_zone
  size              = var.postgres_ec2_data_volume_size_gb
  type              = var.postgres_ec2_data_volume_type
  encrypted         = true

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name   = "${var.project_name}-postgres-data"
    Backup = "Yes"
  }
}

resource "aws_iam_role" "postgres_data_snapshot_lifecycle" {
  name = "${var.project_name}-postgres-data-snapshot-lifecycle-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "dlm.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "postgres_data_snapshot_lifecycle" {
  role       = aws_iam_role.postgres_data_snapshot_lifecycle.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSDataLifecycleManagerServiceRole"
}

resource "aws_dlm_lifecycle_policy" "postgres_data_daily_snapshots" {
  description        = "Daily snapshots for the PostgreSQL EC2 data volume"
  execution_role_arn = aws_iam_role.postgres_data_snapshot_lifecycle.arn
  state              = "ENABLED"

  policy_details {
    resource_types = ["VOLUME"]
    target_tags = {
      Backup = "Yes"
    }

    schedule {
      name = "Daily PostgreSQL data snapshots"

      create_rule {
        interval      = 24
        interval_unit = "HOURS"
        times         = [var.postgres_ec2_snapshot_time_utc]
      }

      retain_rule {
        count = var.postgres_ec2_snapshot_retention_count
      }

      copy_tags = true

      tags_to_add = {
        Name      = "${var.project_name}-postgres-data-snapshot"
        ManagedBy = "terraform"
      }
    }
  }

  tags = {
    Name = "${var.project_name}-postgres-data-daily-snapshots"
  }
}

resource "aws_instance" "postgres_ec2" {
  ami                    = data.aws_ssm_parameter.ecs_ami.value
  instance_type          = var.postgres_ec2_instance_type
  subnet_id              = aws_subnet.private_1.id
  vpc_security_group_ids = [aws_security_group.postgres_ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.postgres_ec2.name

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    volume_size           = 30
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  user_data = <<-EOT
    #!/bin/bash
    set -euxo pipefail

    dnf update -y
    dnf install -y postgresql17 postgresql17-server jq awscli xfsprogs

    DEVICE=""
    for _ in $(seq 1 60); do
      for candidate in /dev/nvme1n1 /dev/xvdb; do
        if [ -b "$candidate" ]; then
          DEVICE="$candidate"
          break
        fi
      done
      if [ -n "$DEVICE" ]; then
        break
      fi
      sleep 2
    done

    if [ -z "$DEVICE" ]; then
      echo "Postgres data device not found" >&2
      exit 1
    fi

    if ! blkid "$DEVICE"; then
      mkfs -t xfs "$DEVICE"
    fi

    mkdir -p /data/postgresql
    grep -q "$DEVICE /data/postgresql" /etc/fstab || echo "$DEVICE /data/postgresql xfs defaults,nofail 0 2" >> /etc/fstab
    mount -a

    mkdir -p /data/postgresql/data
    chown -R postgres:postgres /data/postgresql
    chmod 700 /data/postgresql/data

    if [ ! -f /data/postgresql/data/PG_VERSION ]; then
      sudo -u postgres /usr/bin/initdb -D /data/postgresql/data
    fi

    mkdir -p /etc/systemd/system/postgresql.service.d
    cat > /etc/systemd/system/postgresql.service.d/override.conf <<'OVERRIDE'
    [Service]
    Environment=PGDATA=/data/postgresql/data
    OVERRIDE

    if grep -q '^#listen_addresses' /data/postgresql/data/postgresql.conf; then
      sed -i "s/^#listen_addresses.*/listen_addresses = '*'/" /data/postgresql/data/postgresql.conf
    elif grep -q '^listen_addresses' /data/postgresql/data/postgresql.conf; then
      sed -i "s/^listen_addresses.*/listen_addresses = '*'/" /data/postgresql/data/postgresql.conf
    else
      echo "listen_addresses = '*'" >> /data/postgresql/data/postgresql.conf
    fi

    grep -q "host all all ${var.vpc_cidr} scram-sha-256" /data/postgresql/data/pg_hba.conf || \
      echo "host all all ${var.vpc_cidr} scram-sha-256" >> /data/postgresql/data/pg_hba.conf

    systemctl daemon-reload
    systemctl enable --now postgresql

    DB_PASSWORD=$(aws secretsmanager get-secret-value --region ${var.aws_region} --secret-id ${aws_secretsmanager_secret.db_password.arn} --query SecretString --output text)

    sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
    ALTER USER postgres WITH PASSWORD '$${DB_PASSWORD}';
    DO \$\$BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${var.db_username}') THEN
        CREATE ROLE ${var.db_username} LOGIN PASSWORD '$${DB_PASSWORD}';
      ELSE
        ALTER ROLE ${var.db_username} LOGIN PASSWORD '$${DB_PASSWORD}';
      END IF;
    END\$\$;
    SQL

    if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${var.db_name}'" | grep -q 1; then
      sudo -u postgres createdb --owner=${var.db_username} ${var.db_name}
    fi

    cat > /usr/local/bin/migrate-rds-to-local-postgres.sh <<'SCRIPT'
    #!/bin/bash
    set -euo pipefail

    choose_pg_tool() {
      local tool="$1"
      if command -v "$${tool}17" >/dev/null 2>&1; then
        echo "$${tool}17"
        return
      fi
      if [ -x "/usr/pgsql-17/bin/$${tool}" ]; then
        echo "/usr/pgsql-17/bin/$${tool}"
        return
      fi
      if command -v "$${tool}" >/dev/null 2>&1; then
        echo "$${tool}"
        return
      fi
      return 1
    }

    PG_DUMP_BIN="$(choose_pg_tool pg_dump)"
    PG_RESTORE_BIN="$(choose_pg_tool pg_restore)"

    PG_DUMP_MAJOR="$("$PG_DUMP_BIN" --version | grep -oE '[0-9]+' | head -n 1)"
    if [ "$PG_DUMP_MAJOR" -lt 17 ]; then
      echo "ERROR: pg_dump major version must be >= 17 for this source. Found: $("$PG_DUMP_BIN" --version)" >&2
      exit 1
    fi

    SOURCE_HOST="${local.rds_source_host}"
    SOURCE_PORT="${local.rds_source_port}"
    SOURCE_DB="${var.db_name}"
    SOURCE_USER="${var.db_username}"
    SOURCE_PASSWORD_SECRET_ARN="${aws_secretsmanager_secret.db_password.arn}"

    DEST_HOST="127.0.0.1"
    DEST_PORT="5432"
    DEST_DB="${var.db_name}"
    DEST_USER="${var.db_username}"
    DEST_PASSWORD_SECRET_ARN="${aws_secretsmanager_secret.db_password.arn}"

    AWS_REGION="${var.aws_region}"
    BACKUP_DIR="/var/lib/postgresql/migration"
    BACKUP_FILE="$BACKUP_DIR/rds-to-local-$(date +%Y%m%d-%H%M%S).dump"

    mkdir -p "$BACKUP_DIR"
    chown postgres:postgres "$BACKUP_DIR"
    chmod 700 "$BACKUP_DIR"

    SOURCE_PASSWORD=$(aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$SOURCE_PASSWORD_SECRET_ARN" --query SecretString --output text)
    DEST_PASSWORD=$(aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$DEST_PASSWORD_SECRET_ARN" --query SecretString --output text)

    if [ -z "$SOURCE_HOST" ]; then
      echo "ERROR: RDS source host is not configured (enable_rds_cluster=false)." >&2
      exit 1
    fi

    echo "Creating backup from Aurora source: ${local.rds_source_host}:${local.rds_source_port}/${var.db_name}"
    PGPASSWORD="$SOURCE_PASSWORD" "$PG_DUMP_BIN" \
      -h "$SOURCE_HOST" \
      -p "$SOURCE_PORT" \
      -U "$SOURCE_USER" \
      -d "$SOURCE_DB" \
      -Fc \
      -f "$BACKUP_FILE"

    echo "Restoring backup into local PostgreSQL target: 127.0.0.1:5432/${var.db_name}"
    PGPASSWORD="$DEST_PASSWORD" psql \
      -h "$DEST_HOST" \
      -p "$DEST_PORT" \
      -U "$DEST_USER" \
      -d "$DEST_DB" \
      -v ON_ERROR_STOP=1 \
      -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public AUTHORIZATION ${var.db_username};"

    PGPASSWORD="$DEST_PASSWORD" "$PG_RESTORE_BIN" \
      -h "$DEST_HOST" \
      -p "$DEST_PORT" \
      -U "$DEST_USER" \
      -d "$DEST_DB" \
      --no-owner \
      --no-privileges \
      "$BACKUP_FILE"

    echo "Migration completed successfully."
    echo "Backup file: $BACKUP_FILE"
    SCRIPT

    chmod 750 /usr/local/bin/migrate-rds-to-local-postgres.sh

    systemctl restart postgresql
  EOT

  tags = {
    Name = "${var.project_name}-postgres-ec2"
  }
}

resource "aws_volume_attachment" "postgres_data" {
  device_name = "/dev/xvdb"
  volume_id   = aws_ebs_volume.postgres_data.id
  instance_id = aws_instance.postgres_ec2.id
}

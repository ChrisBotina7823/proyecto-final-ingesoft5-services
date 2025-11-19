terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Single S3 Bucket for Terraform state (shared backend)
resource "aws_s3_bucket" "tfstate" {
  bucket = "chrisb-tfstate-ecommerce"

  tags = {
    Name        = "terraform-state"
    Environment = "shared"
    ManagedBy   = "Terraform"
    Purpose     = "RemoteState"
  }
}

# Enable server-side encryption for the shared bucket
resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Block public access for the shared bucket
resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# DynamoDB table for state locking (shared between environments)
resource "aws_dynamodb_table" "tfstate_lock" {
  name           = "tfstate-lock-ecommerce"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Name        = "terraform-state-lock"
    Environment = "shared"
    ManagedBy   = "Terraform"
  }
}

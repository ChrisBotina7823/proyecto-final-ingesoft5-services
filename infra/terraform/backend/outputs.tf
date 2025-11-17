output "s3_bucket_name" {
  description = "Name of the shared S3 bucket for Terraform state"
  value       = aws_s3_bucket.tfstate.id
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table for state locking"
  value       = aws_dynamodb_table.tfstate_lock.name
}

output "backend_config_base_infra" {
  description = "Backend configuration for base-infra environment"
  value = <<-EOT
    Backend configuration for base-infra:
    
    backend "s3" {
      bucket         = "${aws_s3_bucket.tfstate.id}"
      key            = "base-infra/terraform.tfstate"
      region         = "${var.aws_region}"
      dynamodb_table = "${aws_dynamodb_table.tfstate_lock.name}"
      encrypt        = true
    }
  EOT
}

output "backend_config_dev" {
  description = "Backend configuration for Dev environment"
  value = <<-EOT
    Backend configuration for Dev:
    
    backend "s3" {
      bucket         = "${aws_s3_bucket.tfstate.id}"
      key            = "dev/terraform.tfstate"
      region         = "${var.aws_region}"
      dynamodb_table = "${aws_dynamodb_table.tfstate_lock.name}"
      encrypt        = true
    }
  EOT
}

output "backend_config_prod" {
  description = "Backend configuration for Prod environment"
  value = <<-EOT
    Backend configuration for Prod:
    
    backend "s3" {
      bucket         = "${aws_s3_bucket.tfstate.id}"
      key            = "prod/terraform.tfstate"
      region         = "${var.aws_region}"
      dynamodb_table = "${aws_dynamodb_table.tfstate_lock.name}"
      encrypt        = true
    }
  EOT
}

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }

  # S3 backend for shared state (base-infra)
  backend "s3" {
    bucket         = "chrisb-tfstate-ecommerce"
    key            = "base-infra/terraform.tfstate"
    region         = "eu-west-2"
    dynamodb_table = "tfstate-lock-ecommerce"
    encrypt        = true
  }
}

# Jenkins VM Module - AWS EC2 (moved here from per-environment configs)
module "jenkins_vm" {
  source = "../../modules/aws-jenkins-vm"

  environment         = var.environment
  location            = var.aws_region
  resource_group_name = ""  # Not used in AWS
  
  # Networking
  vpc_cidr    = "10.0.0.0/16"
  subnet_cidr = "10.0.0.0/24"
  
  # VM Configuration
  vm_size           = var.jenkins_vm_size
  disk_size_gb      = 30
  data_disk_size_gb = 50
  admin_username    = "ubuntu"
  
  ssh_public_key_path = var.ssh_public_key_path

  tags = local.common_tags
}

locals {
  common_tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
    CostCenter  = var.cost_center
  }
}

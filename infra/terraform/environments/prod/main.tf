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

  # S3 backend for Prod state management (supports both AWS and Azure resources)
  backend "s3" {
    bucket         = "chrisb-tfstate-ecommerce"
    key            = "prod/terraform.tfstate"
    region         = "eu-west-2"
    dynamodb_table = "tfstate-lock-ecommerce"
    encrypt        = true
  }
}

# Azure Resource Group
resource "azurerm_resource_group" "main" {
  name     = "rg-${var.environment}-${var.project_name}"
  location = var.azure_location

  tags = local.common_tags
}

// Jenkins VM has been moved to the top-level `base-infra` environment.
// This module was intentionally removed from per-environment configurations
// to allow creating/updating Jenkins independently from dev/prod workloads.

# AKS Module - Azure Kubernetes
module "aks" {
  source = "../../modules/azure-aks"

  environment         = var.environment
  project_name        = var.project_name
  location            = var.azure_location
  resource_group_name = azurerm_resource_group.main.name
  
  # Networking
  vnet_address_space    = "10.1.0.0/16"
  subnet_address_prefix = "10.1.0.0/20"
  
  # Kubernetes configuration
  kubernetes_version = var.kubernetes_version
  node_vm_size       = var.aks_node_vm_size  # 16GB RAM, 4 vCPU
  
  # Autoscaling (scales down when idle)
  enable_autoscaling = true
  node_count         = var.aks_node_count
  min_node_count     = var.aks_min_node_count
  max_node_count     = var.aks_max_node_count

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

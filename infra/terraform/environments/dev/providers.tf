provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
  client_id       = var.client_id
  client_secret   = var.client_secret
  tenant_id       = var.tenant_id
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "ecommerce-microservices"
      ManagedBy   = "Terraform"
      Environment = "dev"
    }
  }

  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
}

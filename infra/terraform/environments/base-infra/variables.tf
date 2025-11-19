# AWS credentials
variable "aws_region" {
  description = "AWS region for Jenkins deployment"
  type        = string
  default     = "eu-west-2"
}

variable "aws_access_key" {
  description = "AWS access key"
  type        = string
  sensitive   = true
}

variable "aws_secret_key" {
  description = "AWS secret key"
  type        = string
  sensitive   = true
}

# Azure authentication
variable "subscription_id" {
  description = "Azure subscription ID"
  type        = string
  sensitive   = true
}

variable "client_id" {
  description = "Azure service principal client ID"
  type        = string
  sensitive   = true
}

variable "client_secret" {
  description = "Azure service principal client secret"
  type        = string
  sensitive   = true
}

variable "tenant_id" {
  description = "Azure tenant ID"
  type        = string
  sensitive   = true
}

# General configuration
variable "environment" {
  description = "Environment name"
  type        = string
  default     = "base-infra"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ecommerce"
}

variable "azure_location" {
  description = "Azure region"
  type        = string
  default     = "East US"
}

# SSH configuration
variable "ssh_public_key_path" {
  description = "Path to SSH public key file"
  type        = string
  default     = "../../../credentials/vm_key.pub"
}

# Jenkins VM configuration
variable "jenkins_vm_size" {
  description = "Jenkins VM instance type"
  type        = string
  default     = "m7i-flex.large"
}

variable "cost_center" {
  description = "Cost center tag"
  type        = string
  default     = "Shared"
}

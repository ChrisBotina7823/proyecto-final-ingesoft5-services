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
  default     = "prod"
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
  default     = "~/.ssh/id_rsa.pub"
}

# Jenkins VM configuration
variable "jenkins_vm_size" {
  description = "Jenkins VM instance type"
  type        = string
  default     = "m7i-flex.large"  # 2 vCPU, 8GB RAM (free tier eligible)
}

# AKS configuration
variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.30"
}

variable "aks_node_vm_size" {
  description = "AKS node VM size"
  type        = string
  default     = "Standard_D4s_v3"  # 16GB RAM, 4 vCPU
}

variable "aks_node_count" {
  description = "Initial number of AKS nodes"
  type        = number
  default     = 1
}

variable "aks_min_node_count" {
  description = "Minimum number of AKS nodes for autoscaling"
  type        = number
  default     = 1
}

variable "aks_max_node_count" {
  description = "Maximum number of AKS nodes for autoscaling"
  type        = number
  default     = 1
}

variable "cost_center" {
  description = "Cost center tag"
  type        = string
  default     = "Production"
}

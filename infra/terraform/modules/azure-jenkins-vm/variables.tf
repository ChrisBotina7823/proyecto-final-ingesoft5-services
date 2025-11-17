# ============================================================================
# STANDARDIZED JENKINS VM VARIABLES (Azure Implementation)
# These variables match aws-jenkins-vm for easy provider switching
# ============================================================================

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "location" {
  description = "Cloud region (AWS region or Azure location)"
  type        = string
}

variable "resource_group_name" {
  description = "Resource group name (required for Azure)"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for Jenkins VPC/VNet"
  type        = string
  default     = "10.2.0.0/16"
}

variable "subnet_cidr" {
  description = "CIDR block for Jenkins subnet"
  type        = string
  default     = "10.2.1.0/24"
}

variable "vm_size" {
  description = "VM/Instance size (e.g., 't3.small' for AWS, 'Standard_B2s' for Azure)"
  type        = string
  default     = "Standard_B2s"  # 2 vCPU, 4GB RAM - cost-optimized (~$30/month)
}

variable "disk_size_gb" {
  description = "OS disk size in GB"
  type        = number
  default     = 64
}

variable "data_disk_size_gb" {
  description = "Data disk size in GB"
  type        = number
  default     = 100
}

variable "admin_username" {
  description = "Admin username for VM"
  type        = string
  default     = "azureuser"  # Azure default
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key file"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}

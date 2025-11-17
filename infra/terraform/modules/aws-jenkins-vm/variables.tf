# ============================================================================
# STANDARDIZED JENKINS VM VARIABLES (AWS Implementation)
# These variables match azure-jenkins-vm for easy provider switching
# ============================================================================

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "location" {
  description = "Cloud region (AWS region or Azure location)"
  type        = string
  default     = "us-east-1"
}

variable "resource_group_name" {
  description = "Resource group name (not used in AWS, for interface compatibility)"
  type        = string
  default     = ""
}

variable "vpc_cidr" {
  description = "CIDR block for Jenkins VPC/VNet"
  type        = string
  default     = "10.0.0.0/16"
}

variable "subnet_cidr" {
  description = "CIDR block for Jenkins subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "vm_size" {
  description = "VM/Instance size (e.g., 't3.small' for AWS, 'Standard_B2s' for Azure)"
  type        = string
  default     = "t3.small"  # 2 vCPU, 2GB RAM - cost-optimized (~$15/month)
}

variable "disk_size_gb" {
  description = "OS disk size in GB"
  type        = number
  default     = 30
}

variable "data_disk_size_gb" {
  description = "Data disk size in GB"
  type        = number
  default     = 50
}

variable "admin_username" {
  description = "Admin username for VM"
  type        = string
  default     = "ubuntu"  # AWS default
}

variable "admin_password" {
  description = "Admin password for VM"
  type        = string
  default     = ""
  sensitive   = true
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

# ============================================================================
# AWS-SPECIFIC VARIABLES (optional, with defaults)
# ============================================================================

variable "availability_zone" {
  description = "AWS availability zone for subnet"
  type        = string
  default     = ""  # Auto-select based on region (e.g., us-east-1a)
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "project_name" {
  description = "Project name"
  type        = string
}

variable "location" {
  description = "Azure region"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "vnet_address_space" {
  description = "VNet address space for AKS"
  type        = string
  default     = "10.1.0.0/16"
}

variable "subnet_address_prefix" {
  description = "Subnet address prefix for AKS nodes"
  type        = string
  default     = "10.1.0.0/20"
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.30"
}

variable "node_count" {
  description = "Number of nodes in the default node pool"
  type        = number
  default     = 1
}

variable "node_vm_size" {
  description = "VM size for AKS nodes"
  type        = string
  default     = "Standard_D4s_v3"  # 16GB RAM, 4 vCPU - sufficient for 10 microservices
}

variable "enable_autoscaling" {
  description = "Enable autoscaling for the default node pool"
  type        = bool
  default     = true  # Enable by default for cost optimization
}

variable "min_node_count" {
  description = "Minimum number of nodes when autoscaling is enabled"
  type        = number
  default     = 1
}

variable "max_node_count" {
  description = "Maximum number of nodes when autoscaling is enabled"
  type        = number
  default     = 3
}

variable "service_cidr" {
  description = "CIDR for Kubernetes services"
  type        = string
  default     = "10.10.0.0/16"
}

variable "dns_service_ip" {
  description = "IP address for DNS service within the service CIDR"
  type        = string
  default     = "10.10.0.10"
}

variable "container_registry_id" {
  description = "ID of Azure Container Registry (optional)"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}

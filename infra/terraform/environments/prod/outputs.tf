# Note: Jenkins VM outputs are now in the base-infra environment
# To get Jenkins information, run terraform output in infra/terraform/environments/base-infra

# AKS Outputs
output "aks_cluster_name" {
  description = "AKS cluster name"
  value       = module.aks.cluster_name
}

output "aks_cluster_id" {
  description = "AKS cluster ID"
  value       = module.aks.cluster_id
}

output "aks_kube_config_command" {
  description = "Command to get AKS credentials"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${module.aks.cluster_name}"
}

# Resource Groups
output "azure_resource_group_name" {
  description = "Azure resource group name"
  value       = azurerm_resource_group.main.name
}

output "azure_location" {
  description = "Azure region"
  value       = azurerm_resource_group.main.location
}

output "aws_region" {
  description = "AWS region"
  value       = var.aws_region
}

# Cost Estimate
output "estimated_monthly_cost" {
  description = "Estimated monthly cost (USD)"
  value       = "~$140-170 (AKS 1 node Standard_D4s_v3: ~$140-$210). Jenkins VM cost in base-infra environment."
}

# Next Steps
output "next_steps" {
  description = "Next steps after deployment"
  value = <<-EOT
    
    ========================================
    PRODUCTION Environment Deployed! 
    ========================================
    
    1. Get AKS credentials:
       az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${module.aks.cluster_name}
    
    2. For Jenkins VM access, see base-infra environment outputs:
       cd infra/terraform/environments/base-infra
       terraform output
    
    3. Configure Jenkins:
       - Install required plugins
       - Set up credentials for AKS
       - Configure SonarQube integration
    
    CONFIGURATION (PROD):
    - AKS autoscaling: 1 node (min), up to 3 nodes (max)
    - Node size: Standard_D4s_v3 (16GB RAM, 4 vCPU)
    - Jenkins VM: in separate base-infra environment (shared across environments)
    
    ========================================
  EOT
}

# ============================================================================
# STANDARDIZED JENKINS VM OUTPUTS (Azure Implementation)
# These outputs match aws-jenkins-vm for easy provider switching
# ============================================================================

# Network Outputs
output "vpc_id" {
  description = "ID of the Jenkins VPC/VNet"
  value       = azurerm_virtual_network.jenkins.id
}

output "subnet_id" {
  description = "ID of the Jenkins subnet"
  value       = azurerm_subnet.jenkins.id
}

# VM/Instance Outputs
output "vm_id" {
  description = "ID of the Jenkins VM/instance"
  value       = azurerm_linux_virtual_machine.jenkins.id
}

output "vm_name" {
  description = "Name of the Jenkins VM/instance"
  value       = azurerm_linux_virtual_machine.jenkins.name
}

# IP Addresses
output "public_ip" {
  description = "Public IP address of Jenkins server"
  value       = azurerm_public_ip.jenkins.ip_address
}

output "private_ip" {
  description = "Private IP address of Jenkins server"
  value       = azurerm_network_interface.jenkins.private_ip_address
}

# Access Information
output "admin_username" {
  description = "Admin username for SSH access"
  value       = var.admin_username
}

output "ssh_command" {
  description = "SSH command to connect to Jenkins server"
  value       = "ssh -i ~/.ssh/id_rsa ${var.admin_username}@${azurerm_public_ip.jenkins.ip_address}"
}

# Service URLs
output "jenkins_url" {
  description = "URL to access Jenkins"
  value       = "http://${azurerm_public_ip.jenkins.ip_address}:8080"
}

output "sonarqube_url" {
  description = "URL to access SonarQube"
  value       = "http://${azurerm_public_ip.jenkins.ip_address}:9000"
}

# ============================================================================
# AZURE-SPECIFIC OUTPUTS (additional information)
# ============================================================================

output "identity_principal_id" {
  description = "Managed identity principal ID"
  value       = azurerm_linux_virtual_machine.jenkins.identity[0].principal_id
}

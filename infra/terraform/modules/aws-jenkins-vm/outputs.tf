# ============================================================================
# STANDARDIZED JENKINS VM OUTPUTS (AWS Implementation)
# These outputs match azure-jenkins-vm for easy provider switching
# ============================================================================

# Network Outputs
output "vpc_id" {
  description = "ID of the Jenkins VPC/VNet"
  value       = aws_vpc.jenkins.id
}

output "subnet_id" {
  description = "ID of the Jenkins subnet"
  value       = aws_subnet.public.id
}

# VM/Instance Outputs
output "vm_id" {
  description = "ID of the Jenkins VM/instance"
  value       = aws_instance.jenkins.id
}

output "vm_name" {
  description = "Name of the Jenkins VM/instance"
  value       = aws_instance.jenkins.tags["Name"]
}

# IP Addresses
output "public_ip" {
  description = "Public IP address of Jenkins server"
  value       = aws_eip.jenkins.public_ip
}

output "private_ip" {
  description = "Private IP address of Jenkins server"
  value       = aws_instance.jenkins.private_ip
}

# Access Information
output "admin_username" {
  description = "Admin username for SSH access"
  value       = var.admin_username
}

output "ssh_command" {
  description = "SSH command to connect to Jenkins server"
  value       = "ssh -i ~/.ssh/id_rsa ${var.admin_username}@${aws_eip.jenkins.public_ip}"
}

# Service URLs
output "jenkins_url" {
  description = "URL to access Jenkins"
  value       = "http://${aws_eip.jenkins.public_ip}:8080"
}

output "sonarqube_url" {
  description = "URL to access SonarQube"
  value       = "http://${aws_eip.jenkins.public_ip}:9000"
}

# ============================================================================
# AWS-SPECIFIC OUTPUTS (additional information)
# ============================================================================

output "security_group_id" {
  description = "ID of the Jenkins security group"
  value       = aws_security_group.jenkins.id
}

output "iam_role_arn" {
  description = "ARN of the Jenkins IAM role"
  value       = aws_iam_role.jenkins.arn
}

# Jenkins VM outputs
output "jenkins_vm_id" {
  description = "ID of the Jenkins VM"
  value       = module.jenkins_vm.vm_id
}

output "jenkins_public_ip" {
  description = "Public IP address of Jenkins VM"
  value       = module.jenkins_vm.public_ip
}

output "jenkins_ssh_command" {
  description = "SSH command to connect to Jenkins VM"
  value       = "ssh ubuntu@${module.jenkins_vm.public_ip}"
}

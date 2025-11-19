# Ansible Jenkins deploy

## Requirements

- Properly configured .env file in `infra/jenkins` folder
- Set environment variable `JENKINS_VM_IP` using `export JENKINS_VM_IP=<YOUR_IP>`
- Copy the private key to the path `~/.ssh/vm_key` and `chmod 600 ~/.ssh/vm_key`
- Connect manually to the vm using the private key in `infra/credentials/vm_key` if it is the first time, so that it is added to the trusted hosts, use `ssh -i ~/.ssh/vm_key <IP>`
- Execute with `ansible-playbook -i inventory/hosts.yml deploy.yml`
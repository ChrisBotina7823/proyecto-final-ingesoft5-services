# Ansible Jenkins deploy

## Requirements

- Properly configured .env file in `infra/jenkins` folder
- Set environment variable `JENKINS_VM_IP` using export
- Connect manually to the vm using the private key in `infra/credentials/vm_key` if it is the first time, so that it is added to the trusted hosts
- Execute with `ansible-playbook`
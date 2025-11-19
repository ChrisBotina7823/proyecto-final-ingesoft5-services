# Ansible Configuration

Ansible automates the deployment of Jenkins and SonarQube on a remote VM using Docker Compose.

## Purpose

Install and configure Jenkins and SonarQube containers on a remote host with all required dependencies and configurations.

## How It Works

The playbook copies the entire `infra/jenkins/` directory to the remote VM, including:

- Environment variables
- Configuration as Code files
- Docker Compose definitions
- Kubeconfig files

After copying, it installs dependencies and starts the containers.

## Requirements

**Private Key**: Required for SSH authentication to the remote VM.

Storage options:
- GitHub Actions: Store as `JENKINS_VM_PRIVATE_KEY` secret
- Local usage: Store in `misc/credentials/` (ignored by Git)

**Host Configuration**: Set the Jenkins VM IP address.

```bash
export JENKINS_VM_IP="<your-vm-ip>"
```

Or update the inventory file directly.

## Usage

### Deploy Jenkins and SonarQube

```bash
cd misc/ansible
ansible-playbook -i inventory.yml deploy-jenkins.yml
```

### Update Kubernetes Configuration

Updates kubeconfig files on the remote VM without redeploying containers.

```bash
ansible-playbook -i inventory.yml update-kubeconfig.yml
```

## WSL Considerations

Windows Subsystem for Linux may require specific file permissions for the SSH private key.

**Copy key with correct permissions:**

```bash
./copy-key.sh
```

This script copies the private key from Windows to WSL with proper permissions (chmod 600), preventing SSH authentication errors.

## Files

- `inventory.yml` - Host configuration with Jenkins VM IP
- `deploy-jenkins.yml` - Main playbook for deployment
- `update-kubeconfig.yml` - Playbook for kubeconfig updates
- `copy-key.sh` - Helper script for WSL key setup

#!/bin/bash
# User data script for Jenkins EC2 instance
# Installs Docker, Jenkins, kubectl, Azure CLI, and other tools

set -e

echo "=== Starting Jenkins EC2 Setup ==="

# Update system
apt-get update
apt-get upgrade -y

# Install basic tools
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    software-properties-common \
    unzip \
    git \
    jq \
    wget

# Install Docker
echo "=== Installing Docker ==="
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start and enable Docker
systemctl start docker
systemctl enable docker

# Add ubuntu user to docker group
usermod -aG docker ubuntu

# Install Docker Compose
echo "=== Installing Docker Compose ==="
DOCKER_COMPOSE_VERSION="2.24.0"
curl -L "https://github.com/docker/compose/releases/download/v$${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install kubectl
echo "=== Installing kubectl ==="
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm kubectl

# Install Azure CLI
echo "=== Installing Azure CLI ==="
curl -sL https://aka.ms/InstallAzureCLIDeb | bash

# Install AWS CLI
echo "=== Installing AWS CLI ==="
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip

# Install Terraform
echo "=== Installing Terraform ==="
wget -O- https://apt.releases.hashicorp.com/gpg | gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/hashicorp.list
apt-get update
apt-get install -y terraform

# Install Java (required for Jenkins and SonarQube)
echo "=== Installing Java ==="
apt-get install -y openjdk-17-jdk

# Format and mount data volume
echo "=== Configuring data volume ==="
if [ -b /dev/nvme1n1 ]; then
    mkfs -t ext4 /dev/nvme1n1 || true
    mkdir -p /var/jenkins_home
    mount /dev/nvme1n1 /var/jenkins_home
    echo '/dev/nvme1n1 /var/jenkins_home ext4 defaults,nofail 0 2' >> /etc/fstab
    chown -R ubuntu:ubuntu /var/jenkins_home
fi

# Create directory for Jenkins with Docker Compose
echo "=== Creating Jenkins directory ==="
mkdir -p /home/ubuntu/jenkins
chown -R ubuntu:ubuntu /home/ubuntu/jenkins

# Create docker-compose.yml for Jenkins
cat > /home/ubuntu/jenkins/docker-compose.yml <<'EOF'
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    user: root
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - JAVA_OPTS=-Xmx2048m -XX:MaxPermSize=512m
    restart: unless-stopped

volumes:
  jenkins_home:
    driver: local
EOF

# Start Jenkins
echo "=== Starting Jenkins ==="
cd /home/ubuntu/jenkins
docker-compose up -d

# Wait for Jenkins to start
echo "=== Waiting for Jenkins to initialize ==="
sleep 30

# Get initial admin password
if [ -d /var/lib/docker/volumes/jenkins_jenkins_home/_data ]; then
    JENKINS_PASSWORD=$(docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 2>/dev/null || echo "Not available yet")
    echo "Jenkins Initial Admin Password: $JENKINS_PASSWORD" > /home/ubuntu/jenkins-password.txt
    chown ubuntu:ubuntu /home/ubuntu/jenkins-password.txt
fi

# Create info file
cat > /home/ubuntu/SETUP_INFO.txt <<EOF
===========================================
Jenkins EC2 Instance Setup Complete
===========================================

Environment: ${environment}

Services:
- Jenkins: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080
- Initial password location: /home/ubuntu/jenkins-password.txt

Installed tools:
- Docker $(docker --version)
- Docker Compose $(docker-compose --version)
- kubectl $(kubectl version --client --short 2>/dev/null || echo "installed")
- Azure CLI $(az version --output tsv 2>/dev/null | head -1 || echo "installed")
- AWS CLI $(aws --version)
- Terraform $(terraform version | head -1)
- Java $(java -version 2>&1 | head -1)

Next steps:
1. Access Jenkins UI
2. Use initial admin password from jenkins-password.txt
3. Install suggested plugins
4. Create admin user
5. Configure Jenkins with GitHub credentials
6. Configure Azure service principal
7. Deploy SonarQube with docker-compose

Data volume: /var/jenkins_home (mounted from /dev/nvme1n1)

===========================================
EOF

chown ubuntu:ubuntu /home/ubuntu/SETUP_INFO.txt

echo "=== Setup Complete ==="
echo "Jenkins URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"

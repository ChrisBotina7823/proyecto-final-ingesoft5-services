FROM jenkins/jenkins:2.479-jdk17

# Skip setup wizard
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

# Install plugins from plugins.txt
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Switch to root to install system tools
USER root

# Combine all apt-get operations in ONE layer to improve cache efficiency
RUN apt-get update && \
apt-get install -y --no-install-recommends \
apt-transport-https \
ca-certificates \
curl \
gnupg \
lsb-release \
maven \
gettext-base \
dos2unix && \
    rm -rf /var/lib/apt/lists/*
    
# Install Docker CLI (separate layer for better caching)
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
RUN set -eux; \
DISTRO="$(lsb_release -cs)"; \
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian ${DISTRO} stable" \
| tee /etc/apt/sources.list.d/docker.list > /dev/null

RUN apt-get update && \
apt-get install -y --no-install-recommends docker-ce-cli && \
rm -rf /var/lib/apt/lists/*

# Install kubectl (separate layer, rarely changes)
ADD https://dl.k8s.io/release/v1.28.0/bin/linux/amd64/kubectl /usr/local/bin/kubectl
RUN chmod +x /usr/local/bin/kubectl

# Install Azure CLI (separate layer, rarely changes)
# RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash && \
#     rm -rf /var/lib/apt/lists/*

# Install sonar-scanner (for SonarQube analysis in Jenkins)

ADD https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-7.2.0.5079-linux-x64.zip /tmp/sonar-scanner.zip
RUN unzip /tmp/sonar-scanner.zip -d /opt && \
mv /opt/sonar-scanner-7.2.0.5079-linux-x64 /opt/sonar-scanner && \
ln -s /opt/sonar-scanner/bin/sonar-scanner /usr/local/bin/sonar-scanner && \
rm /tmp/sonar-scanner.zip

# Add jenkins user to docker group
RUN groupadd -f docker && usermod -aG docker jenkins

# Copy and prepare entrypoint script
# COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
# RUN chmod +x /usr/local/bin/docker-entrypoint.sh && \
#     dos2unix /usr/local/bin/docker-entrypoint.sh

# Switch back to jenkins user
USER jenkins

# Use custom entrypoint
# ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

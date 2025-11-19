# Jenkins Configuration

Jenkins and SonarQube run as Docker containers with persistent storage and rapid configuration refresh capabilities.

## Architecture

**Docker Compose Setup**: Two main services (Jenkins + SonarQube) with supporting infrastructure.

**Volume Strategy**:

Named volumes (persistent caches):
- `jenkins-npm-cache` - NPM packages
- `jenkins-maven-cache` - Maven dependencies
- `jenkins-pip-cache` - Python packages

These volumes prevent re-downloading dependencies on every build.

Bind mounts (hot-reload configuration):
- `./casc.yaml:/var/jenkins_home/casc.yaml`
- `./kubeconfig-dev:/home/jenkins/.kube/config-dev`
- `./kubeconfig-prod:/home/jenkins/.kube/config-prod`

Configuration changes apply with `docker compose restart jenkins` (no rebuild needed).

## Environment Variables

Required credentials stored in `.env` file:

```
GITHUB_TOKEN=<token>
DOCKER_REGISTRY_USER=<username>
DOCKER_REGISTRY_PASS=<password>
AZURE_SUBSCRIPTION_ID=<id>
AZURE_CLIENT_ID=<id>
AZURE_CLIENT_SECRET=<secret>
AZURE_TENANT_ID=<id>
KUBECONFIG_DEV_BASE64=<base64-encoded-config>
KUBECONFIG_PROD_BASE64=<base64-encoded-config>
JENKINS_ADMIN_USER=<username>
JENKINS_ADMIN_PASSWORD=<password>
SONAR_ADMIN_PASSWORD=<password>
SONAR_DB_PASSWORD=<password>
SLACK_BOT_TOKEN=<token>
```

See `.env.example` for complete template.

## Custom Dockerfile

Jenkins uses a custom image with pre-installed tools:

- Docker CLI
- kubectl
- Terraform
- Azure CLI
- Node.js, Python, Maven
- Required Jenkins plugins (from `plugins.txt`)

This eliminates runtime installation delays.

## SonarQube Initialization

SonarQube 9.9 requires password change on first start.

`init-sonar.sh` script handles:
- Waiting for SonarQube availability
- Changing default admin password
- Creating initial project configurations

Runs automatically during container startup.

## Kubernetes Configuration Management

Multiple kubeconfig files enable environment switching:

- `kubeconfig-dev` - Development AKS cluster
- `kubeconfig-prod` - Production AKS cluster

Files stored in base64 format as environment variables, decoded at runtime.

Pipeline switches contexts using `KUBECONFIG` environment variable:

```bash
export KUBECONFIG=/home/jenkins/.kube/config-dev
kubectl apply -f manifests/
```

## Quick Commands

**Start services:**
```bash
docker compose up -d
```

**Reload configuration:**
```bash
docker compose restart jenkins
```

**View logs:**
```bash
docker compose logs -f jenkins
```

**Rebuild with new dependencies:**
```bash
docker compose up -d --build
```

## Access

- Jenkins: `http://localhost:8090`
- SonarQube: `http://localhost:9000`

Default credentials in `.env` file.

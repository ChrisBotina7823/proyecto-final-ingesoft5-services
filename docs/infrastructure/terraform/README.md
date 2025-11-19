# Terraform Infrastructure

Modular infrastructure provisioning for Azure Kubernetes Service and supporting resources.

## Architecture

**Modular Design**: Independent, reusable modules that can be deployed separately.

**Modules**:
- `azure-aks` - Kubernetes cluster with networking (VNet, subnet, NSG, load balancer)
- `azure-vm` - Virtual machines (Jenkins CI/CD server)

Modules are environment-agnostic and configurable through variables.

## Environments

**base-infra**: Jenkins VM only. Independent from dev/prod to prevent Kubernetes changes from affecting CI/CD infrastructure.

**dev**: Development AKS cluster with reduced resources (Standard_B2s nodes).

**prod**: Production AKS cluster with higher resources and autoscaling.

Environment-specific configurations in `environments/{base-infra,dev,prod}/`:
- `main.tf` - Module composition
- `variables.tf` - Environment parameters
- `outputs.tf` - Exported values

## State Management

**Backend**: AWS S3 bucket with DynamoDB locking.

Configuration:
```hcl
backend "s3" {
  bucket         = "chrisb-tfstate-ecommerce"
  key            = "dev/terraform.tfstate"  # or prod, base-infra
  region         = "eu-west-2"
  dynamodb_table = "tfstate-lock-ecommerce"
  encrypt        = true
}
```

Benefits:
- Centralized state storage
- State locking prevents concurrent modifications
- Team collaboration without conflicts
- State versioning and recovery

## Initialization Scripts

**Location**: `misc/scripts/`

**init-infrastructure.ps1**: Initialize and manage infrastructure.

Usage:
```powershell
# Initialize with backend
.\init-infrastructure.ps1 -Environment dev -WithBackend

# Initialize without backend (local state)
.\init-infrastructure.ps1 -Environment dev

# Initialize base infrastructure
.\init-infrastructure.ps1 -Environment base-infra -WithBackend

# Destroy environment
.\init-infrastructure.ps1 -Environment dev -Destroy
```

Options:
- `-Environment` - Target environment (dev/prod/base-infra)
- `-WithBackend` - Use S3 backend (default: local)
- `-Destroy` - Destroy infrastructure instead of creating
- `-SkipJenkins` - Skip Jenkins VM (for dev/prod only)

## Deployment Workflow

**1. Initialize backend storage:**

```bash
cd infra/terraform/backend
terraform init
terraform apply
```

**2. Deploy base infrastructure:**

```bash
cd ../environments/base-infra
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

**3. Deploy environment:**

```bash
cd ../dev  # or prod
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

## Module Details

**azure-aks**:
- Creates VNet with configurable CIDR
- Subnet for AKS nodes
- Network Security Group with rules
- AKS cluster with system node pool
- Autoscaling configuration
- Container registry integration (optional)

**azure-vm**:
- Ubuntu VM for Jenkins
- Public IP with DNS label
- SSH key authentication
- Security group with port 8090 (Jenkins), 9000 (SonarQube)
- Cloud-init for Docker installation

## Variables

Key variables across environments:

| Variable | Description | Dev Default | Prod Default |
|----------|-------------|-------------|--------------|
| `aks_node_vm_size` | Node instance type | Standard_B2s | Standard_D4s_v3 |
| `aks_node_count` | Initial nodes | 1 | 2 |
| `aks_min_node_count` | Autoscale minimum | 1 | 2 |
| `aks_max_node_count` | Autoscale maximum | 3 | 5 |
| `kubernetes_version` | K8s version | 1.30 | 1.30 |

## Outputs

Exported values for pipeline integration:

```hcl
output "aks_cluster_name" {
  value = module.aks.cluster_name
}

output "aks_kubeconfig" {
  value     = module.aks.kubeconfig
  sensitive = true
}

output "jenkins_public_ip" {
  value = azurerm_public_ip.jenkins.ip_address
}
```

## Best Practices

**Separate state files**: Each environment has isolated state, preventing cross-environment changes.

**Module versioning**: Pin module versions in production for stability.

**Variable files**: Use `terraform.tfvars` (gitignored) for sensitive values.

**Plan before apply**: Always review `terraform plan` output.

**State locking**: Prevents concurrent modifications and state corruption.

## Troubleshooting

**State locked:**
```bash
terraform force-unlock <lock-id>
```

**View current state:**
```bash
terraform show
```

**Import existing resource:**
```bash
terraform import module.aks.azurerm_kubernetes_cluster.main /subscriptions/.../resourceGroups/.../providers/Microsoft.ContainerService/managedClusters/...
```

**Refresh state:**
```bash
terraform refresh
```

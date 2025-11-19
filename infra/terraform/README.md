# Terraform Infrastructure for E-commerce Microservices

Esta carpeta contiene la infraestructura como código para desplegar el proyecto en AWS y Azure.

## Backend State Management

El backend de Terraform usa S3 buckets separados por entorno:
- **Dev**: `tfstate-ecommerce-dev`
- **Prod**: `tfstate-ecommerce-prod`

Ambos comparten una tabla DynamoDB para state locking: `tfstate-lock-ecommerce`

## Uso de Terraform

### 1. Configurar Backend (Una sola vez)

Primero, crea los buckets S3 y la tabla DynamoDB para almacenar el estado de Terraform:

```powershell
cd backend
cp terraform.tfvars.example terraform.tfvars
# Edita terraform.tfvars con tus credenciales AWS

terraform init
terraform apply
```

### 2. Desplegar Entornos

#### Entorno de Desarrollo

```powershell
cd environments/dev
cp terraform.tfvars.example terraform.tfvars
# Edita terraform.tfvars con tus credenciales AWS y Azure

terraform init
terraform plan
terraform apply
```

#### Entorno de Producción

```powershell
cd environments/prod
cp terraform.tfvars.example terraform.tfvars
# Edita terraform.tfvars con tus credenciales AWS y Azure

terraform init
terraform plan
terraform apply
```

## Architecture

- **Backend State**: S3 buckets separados por entorno (dev/prod) + DynamoDB para locking
- **Jenkins VM**: AWS EC2 (t3.micro para dev, t3.small para prod)
- **Azure Kubernetes Service (AKS)**: Cluster Kubernetes administrado para microservicios
- **Networking**: VPC en AWS para Jenkins, VNet en Azure para AKS

## Directory Structure

```
terraform/
├── backend/                    # Configuración de backend S3 (crea buckets dev/prod)
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── terraform.tfvars.example
├── modules/                    # Módulos Terraform reutilizables
│   ├── aws-jenkins-vm/        # Jenkins VM en AWS EC2
│   ├── azure-jenkins-vm/      # Jenkins VM en Azure (alternativa)
│   └── azure-aks/             # Azure Kubernetes Service
├── environments/              # Configuraciones por entorno
│   ├── dev/                   # Desarrollo (usa tfstate-ecommerce-dev)
│   │   ├── main.tf
│   │   ├── providers.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars.example
│   └── prod/                  # Producción (usa tfstate-ecommerce-prod)
│       ├── main.tf
│       ├── providers.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars.example
└── .gitignore
```

## Prerequisites

1. **AWS CLI** configurado (para backend S3 y Jenkins VM)
2. **Azure CLI** instalado y autenticado (para AKS)
3. **Terraform** >= 1.5.0
4. **SSH Key pair** para acceso a VMs

## Buckets de Estado por Entorno

- **Dev**: `tfstate-ecommerce-dev` (estado completo del entorno de desarrollo)
- **Prod**: `tfstate-ecommerce-prod` (estado completo del entorno de producción)
- **Lock Table**: `tfstate-lock-ecommerce` (compartida entre entornos)

Esta separación garantiza que los cambios en dev no afecten prod y viceversa.

## Security Best Practices

- SSH key-based authentication (sin contraseñas)
- Network Security Groups con puertos mínimos requeridos
- Terraform state encriptado en S3 (AES256)
- DynamoDB para prevenir modificaciones concurrentes (state locking)
- Service Principal con privilegios mínimos
- Buckets S3 con acceso público bloqueado
- Managed identities para AKS

## Outputs por Entorno

Después del deployment exitoso:
- IP pública de Jenkins VM
- Nombre del cluster AKS y resource group
- Comandos para obtener kubeconfig
- Estimado de costos mensuales
- URLs de acceso a Jenkins y SonarQube

## Cost Optimization

**Dev Environment** (~$50-80/mes):
- Jenkins: t3.micro (1 vCPU, 1GB RAM)
- AKS: 1-2 nodos Standard_B2s con autoscaling

**Prod Environment** (~$170-200/mes):
- Jenkins: t3.small (2 vCPU, 2GB RAM)
- AKS: 2-3 nodos Standard_D4s_v3 con autoscaling

## Troubleshooting

Si ves errores de backend al hacer `terraform init`:

1. Asegúrate de haber aplicado primero el backend:
   ```powershell
   cd backend
   terraform apply
   ```

2. Verifica que los buckets existan en S3:
   - `tfstate-ecommerce-dev`
   - `tfstate-ecommerce-prod`

3. Si necesitas recrear el estado, elimina `.terraform/` y vuelve a hacer `terraform init`

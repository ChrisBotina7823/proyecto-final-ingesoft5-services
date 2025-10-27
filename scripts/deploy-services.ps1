# Script to deploy all microservices in AKS
# Usage: .\deploy-services.ps1

Write-Host ""
Write-Host "=== DEPLOYING ECOMMERCE MICROSERVICES ===" -ForegroundColor Cyan

# Change to the kubernetes directory
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$kubernetesDir = Join-Path $scriptPath "..\infra\kubernetes\base"
Set-Location $kubernetesDir

# Get AKS credentials
Write-Host ""
Write-Host "[1/5] Getting AKS credentials..." -ForegroundColor Yellow
az aks get-credentials --resource-group "rg-dev-ecommerce" --name "aks-dev-ecommerce" --overwrite-existing

# Verify connection to AKS
Write-Host ""
Write-Host "[2/5] Verifying connection to AKS..." -ForegroundColor Yellow
kubectl get nodes
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: No connection to AKS cluster" -ForegroundColor Red
    exit 1
}

# Verify or create namespace
Write-Host ""
Write-Host "[3/5] Verifying namespace ecommerce-prod..." -ForegroundColor Yellow
kubectl get namespace ecommerce-prod 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Creating namespace ecommerce-prod..." -ForegroundColor Green
    kubectl create namespace ecommerce-prod
}

# Deploy infrastructure services
Write-Host ""
Write-Host "[4/5] Deploying infrastructure services..." -ForegroundColor Yellow
kubectl apply -f service-discovery.yaml -n ecommerce-prod
kubectl apply -f cloud-config.yaml -n ecommerce-prod
kubectl apply -f zipkin.yaml -n ecommerce-prod

Write-Host "Waiting 20 seconds for infrastructure to start..." -ForegroundColor Cyan
Start-Sleep -Seconds 20

# Deploy business microservices
Write-Host ""
Write-Host "[5/5] Deploying business microservices..." -ForegroundColor Yellow
kubectl apply -f user-service.yaml -n ecommerce-prod
kubectl apply -f product-service.yaml -n ecommerce-prod
kubectl apply -f order-service.yaml -n ecommerce-prod
kubectl apply -f payment-service.yaml -n ecommerce-prod
kubectl apply -f shipping-service.yaml -n ecommerce-prod
kubectl apply -f favourite-service.yaml -n ecommerce-prod
kubectl apply -f api-gateway.yaml -n ecommerce-prod

# Show status
Write-Host ""
Write-Host "=== DEPLOYMENT COMPLETE ===" -ForegroundColor Green
Write-Host ""

Write-Host "Current status of all resources in namespace ecommerce-prod:" -ForegroundColor Cyan

kubectl get all -n ecommerce-prod
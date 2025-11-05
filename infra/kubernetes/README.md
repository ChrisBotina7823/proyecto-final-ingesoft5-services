# Kubernetes Infrastructure

Kubernetes deployment configuration for microservices using Kustomize.

## Architecture

All services deployed to `ecommerce-prod` namespace with:

- Service discovery via Eureka
- Centralized configuration via Spring Cloud Config
- API Gateway for external access
- Inter-service communication via ClusterIP services

## Quick Start

```powershell
# Apply all configurations
kubectl apply -k infra/kubernetes/

# Check deployment status
kubectl get pods -n ecommerce-prod

# Access API Gateway locally
kubectl port-forward svc/api-gateway 8080:8080 -n ecommerce-prod
```

## Services Deployed

| Service | Type | Port | Replicas |
|---------|------|------|----------|
| service-discovery | ClusterIP | 8761 | 1 |
| cloud-config | ClusterIP | 8888 | 1 |
| api-gateway | LoadBalancer | 8080 | 2 |
| proxy-client | ClusterIP | 8080 | 2 |
| user-service | ClusterIP | 8080 | 2 |
| product-service | ClusterIP | 8080 | 2 |
| favourite-service | ClusterIP | 8080 | 2 |
| order-service | ClusterIP | 8080 | 2 |
| payment-service | ClusterIP | 8080 | 2 |
| shipping-service | ClusterIP | 8080 | 2 |

## Kustomize Configuration

Using `kustomization.yaml` for:

- **Namespace**: All resources in `ecommerce-prod`
- **Image tags**: Updated via Jenkins pipeline with commit hash
- **Common labels**: `app: ecommerce`, `environment: production`
- **Registry secret**: `ghcr-secret` for pulling private images

### Updating Image Tags

Jenkins pipeline automatically updates tags:

```bash
cd infra/kubernetes
kustomize edit set image service-name=ghcr.io/user/service-name:commit-hash
kubectl apply -k .
```

## Resource Limits

Each microservice configured with:

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

Core services (Eureka, Config) have higher limits.

## Health Checks

All services include:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

## Accessing Services

### API Gateway (External)

```powershell
# Get external IP (if LoadBalancer provisioned)
kubectl get svc api-gateway -n ecommerce-prod

# Port-forward for local testing
kubectl port-forward svc/api-gateway 8080:8080 -n ecommerce-prod
```

### Eureka Dashboard

```powershell
kubectl port-forward svc/service-discovery 8761:8761 -n ecommerce-prod
# Open http://localhost:8761
```

### Config Server

```powershell
kubectl port-forward svc/cloud-config 8888:8888 -n ecommerce-prod
# Test: curl http://localhost:8888/user-service/default
```

## CI/CD Integration

Jenkins pipeline stages:

1. **Deploy to Kubernetes**: Applies all manifests
2. **Wait for Ready**: Waits up to 5 minutes for all pods
3. **E2E Tests**: Port-forwards API Gateway and runs Cypress
4. **Performance Tests**: Port-forwards and runs Locust

### Port-Forward in Pipeline

Pipeline uses localhost connection to avoid network issues:

```bash
kubectl port-forward svc/api-gateway 9090:8080 -n ecommerce-prod &
# Tests run against http://localhost:9090
```

## Troubleshooting

### Pods not starting

Check events:

```powershell
kubectl describe pod <pod-name> -n ecommerce-prod
```

Common issues:
- Image pull errors: Verify `ghcr-secret` credentials
- Resource limits: Check node capacity
- Config server not ready: Wait for cloud-config pod

### Service discovery issues

Verify Eureka registration:

```powershell
kubectl logs <service-pod> -n ecommerce-prod | grep "Registered instance"
```

All services should register with Eureka at startup.

### Database connections

Services use in-memory H2 databases for demo purposes. No external DB required.

## Cleanup

```powershell
# Delete all resources
kubectl delete namespace ecommerce-prod

# Or delete specific deployment
kubectl delete deployment <service-name> -n ecommerce-prod
```

## Infrastructure as Code

All manifests stored in `infra/kubernetes/`:
- `namespace.yaml`: Namespace definition
- `kustomization.yaml`: Kustomize configuration
- `*.yaml`: Individual service deployments

Changes should be committed to Git and deployed via Jenkins pipeline.

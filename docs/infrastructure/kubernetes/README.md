# Kubernetes Deployment

Kustomize-based deployment strategy for microservices across multiple environments.

## Architecture

**Kustomize**: Apply all manifests in a single operation with environment-specific overlays.

**Structure**:
```
infra/kubernetes/
├── base/                    # Base manifests
│   ├── deployments/
│   ├── services/
│   ├── configmaps/
│   └── kustomization.yaml
└── overlays/
    ├── dev/                 # Development overrides
    │   └── kustomization.yaml
    └── prod/                # Production overrides
        └── kustomization.yaml
```

## Environments

**Dev and Prod**: Currently identical configurations following best practices. Separation allows future customization (replicas, resources, etc.) without affecting other environments.

## Components

**Namespace Configuration**: Environment-specific namespace creation (dev/prod).

**Loki Data Source**: ConfigMap for Grafana to connect to Loki for log queries.

**Grafana Dashboards**: Pre-configured dashboards as ConfigMaps, automatically loaded by Grafana.

**ServiceMonitor**: Prometheus configuration to scrape metrics from microservices endpoints (`/actuator/prometheus`).

## Deployment

**Deploy to development:**

```bash
kubectl apply -k infra/kubernetes/overlays/dev
```

**Deploy to production:**

```bash
kubectl apply -k infra/kubernetes/overlays/prod
```

**Verify deployment:**

```bash
kubectl get all -n dev
kubectl get servicemonitors -n observability
```

## ServiceMonitor Configuration

Enables Prometheus to automatically discover and scrape microservices:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: microservices-metrics
  namespace: observability
spec:
  selector:
    matchLabels:
      metrics: enabled
  endpoints:
  - port: http
    path: /actuator/prometheus
```

Services must have label `metrics: enabled` to be scraped.

## Grafana Dashboards

Custom dashboards provided as ConfigMaps:

- `dashboard-microservices.json` - Service health and performance
- `dashboard-business-metrics.json` - Business KPIs (registrations, orders, payments)
- `dashboard-kubernetes.json` - Cluster resource utilization

Dashboards auto-import on Grafana startup.

## Update Configuration

After modifying manifests or overlays:

```bash
kubectl apply -k infra/kubernetes/overlays/dev --prune
```

The `--prune` flag removes resources deleted from manifests.

## Troubleshooting

**View applied configuration:**
```bash
kubectl kustomize infra/kubernetes/overlays/dev
```

**Check ServiceMonitor status:**
```bash
kubectl get servicemonitors -n observability
kubectl describe servicemonitor microservices-metrics -n observability
```

**Verify Prometheus targets:**

Port-forward to Prometheus and check `http://localhost:9090/targets`

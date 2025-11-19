# Helm Charts

Helm templates for rapid deployment of the observability stack.

## Purpose

Provide reusable chart templates for installing Prometheus, Grafana, and Loki in Kubernetes clusters.

## Components

**Prometheus Stack** (`kube-prometheus-stack`):
- Prometheus server for metrics collection
- Grafana for visualization
- Alertmanager for alert routing
- Node exporters for host metrics
- Kube-state-metrics for cluster state

**Loki Stack** (`loki-stack`):
- Loki for log aggregation
- Promtail for log collection from pods

## Installation

**Add Helm repositories:**

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
```

**Install observability stack:**

```bash
# Create namespace
kubectl create namespace observability

# Install Prometheus stack
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace observability \
  --values infra/helm/prometheus-values.yaml

# Install Loki stack
helm install loki-stack grafana/loki-stack \
  --namespace observability \
  --values infra/helm/loki-values.yaml
```

## Configuration Files

- `prometheus-values.yaml` - Prometheus stack configuration
  - Retention policies
  - Storage settings
  - Service monitors
  - Alert rules

- `loki-values.yaml` - Loki stack configuration
  - Log retention
  - Storage configuration
  - Promtail settings

## Customization

Values files allow customization without modifying charts:

```yaml
# Example: Enable persistence
prometheus:
  prometheusSpec:
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi
```

## Upgrade

```bash
helm upgrade kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace observability \
  --values infra/helm/prometheus-values.yaml

helm upgrade loki-stack grafana/loki-stack \
  --namespace observability \
  --values infra/helm/loki-values.yaml
```

## Uninstall

```bash
helm uninstall kube-prometheus-stack -n observability
helm uninstall loki-stack -n observability
```

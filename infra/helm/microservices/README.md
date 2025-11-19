# Ecommerce Microservices Helm Chart

Helm chart para desplegar todos los microservicios de la aplicación de ecommerce.

## Estructura

```
microservices/
├── Chart.yaml                  # Metadata del chart
├── values.yaml                 # Valores por defecto (producción)
├── values-dev.yaml            # Valores para desarrollo
├── values-stage.yaml          # Valores para staging
├── values-prod.yaml           # Valores para producción
└── templates/
    ├── namespace.yaml         # Namespace para los servicios
    ├── deployment.yaml        # Deployments de todos los servicios
    ├── service.yaml           # Services de todos los servicios
    ├── servicemonitor.yaml    # ServiceMonitors para Prometheus
    ├── loki-datasource.yaml   # ConfigMap para Loki datasource
    └── imagepullsecret.yaml   # Secret para pull de imágenes
```

## Instalación

### Desarrollo
```bash
helm install ecommerce-dev infra/helm/microservices \
  --values infra/helm/microservices/values-dev.yaml \
  --namespace dev \
  --create-namespace
```

### Staging
```bash
helm install ecommerce-stage infra/helm/microservices \
  --values infra/helm/microservices/values-stage.yaml \
  --namespace stage \
  --create-namespace
```

### Producción
```bash
helm install ecommerce-prod infra/helm/microservices \
  --values infra/helm/microservices/values-prod.yaml \
  --namespace prod \
  --create-namespace
```

## Actualización

### Desarrollo
```bash
helm upgrade ecommerce-dev infra/helm/microservices \
  --values infra/helm/microservices/values-dev.yaml \
  --namespace dev
```

### Staging
```bash
helm upgrade ecommerce-stage infra/helm/microservices \
  --values infra/helm/microservices/values-stage.yaml \
  --namespace stage
```

### Producción
```bash
helm upgrade ecommerce-prod infra/helm/microservices \
  --values infra/helm/microservices/values-prod.yaml \
  --namespace prod
```

## Actualización con nueva versión de imagen

```bash
helm upgrade ecommerce-prod infra/helm/microservices \
  --values infra/helm/microservices/values-prod.yaml \
  --set global.imageTag=v1.2.3 \
  --namespace prod
```

## Desinstalación

```bash
helm uninstall ecommerce-dev -n dev
helm uninstall ecommerce-stage -n stage
helm uninstall ecommerce-prod -n prod
```

## Configuración

Cada ambiente tiene su archivo de valores específico con configuraciones optimizadas:

- **Dev**: Recursos mínimos, 1 réplica por servicio
- **Stage**: Recursos medios, 1-2 réplicas por servicio
- **Prod**: Recursos completos, 2-3 réplicas por servicio

## Servicios incluidos

- service-discovery (Eureka)
- cloud-config (Spring Cloud Config)
- api-gateway (Gateway)
- proxy-client (Proxy)
- user-service (Usuarios)
- product-service (Productos)
- favourite-service (Favoritos)
- order-service (Órdenes)
- shipping-service (Envíos)
- payment-service (Pagos)
- zipkin (Tracing distribuido)

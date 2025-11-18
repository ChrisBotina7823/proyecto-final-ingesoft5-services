# Helm Chart Dependencies

Este directorio contiene las configuraciones de deployment por ambiente usando los Helm charts individuales de cada microservicio.

## Estructura

```
infra/helm/
├── dev-values.yaml          # Configuración para desarrollo
├── stage-values.yaml        # Configuración para staging  
├── prod-values.yaml         # Configuración para producción
├── install-all-dev.sh       # Script para instalar en dev
├── install-all-stage.sh     # Script para instalar en stage
├── install-all-prod.sh      # Script para instalar en prod
└── microservices/           # Chart agregador (legacy)
```

## Instalación Individual

Cada microservicio tiene su propio Helm chart en `services/<nombre>/helm/`.

```bash
# Instalar un servicio específico
helm install service-discovery \
  ./services/service-discovery/helm \
  --namespace dev \
  --create-namespace

# Con valores personalizados
helm install user-service \
  ./services/user-service/helm \
  -f infra/helm/dev-values.yaml \
  --namespace dev
```

## Instalación de Todos los Servicios

### Desarrollo
```bash
./infra/helm/install-all-dev.sh
```

### Staging
```bash
./infra/helm/install-all-stage.sh
```

### Producción
```bash
./infra/helm/install-all-prod.sh
```

## Actualización

```bash
# Actualizar un servicio
helm upgrade service-discovery \
  ./services/service-discovery/helm \
  --namespace dev \
  --set image.tag=v1.2.3

# Actualizar todos con script
./infra/helm/upgrade-all-dev.sh v1.2.3
```

## Valores Comunes

Cada ambiente tiene su archivo de valores que se aplica a todos los servicios:

- **dev-values.yaml**: Recursos mínimos, 1 réplica
- **stage-values.yaml**: Recursos medios, 1-2 réplicas
- **prod-values.yaml**: Recursos completos, 2-3 réplicas

## Override de Valores

```bash
# Override para servicio específico
helm install api-gateway \
  ./services/api-gateway/helm \
  -f infra/helm/prod-values.yaml \
  --set replicaCount=5 \
  --set resources.limits.memory=2Gi \
  --namespace prod
```

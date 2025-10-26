# Monorepo & Branching Strategy (CI/CD guidance)

This repository is organized as a monorepo that contains all code, infrastructure and documentation in a single repository. The monorepo layout allows us to run CI per-directory, version modules independently when needed, and simplify cross-service changes.

Folder structure:

```
/
	/infra                # IaC, k8s manifests, helm charts, Jenkins configuration
	/services             # All microservices
	/docs                 # Documentation site and guides
	/scripts              # Helper scripts (cleaning, local helpers)
	compose.yml           # Local docker-compose orchestrator
	pom.xml               # Optional parent pom (if using Maven multi-module)
```

---

## Branching strategy (adapted to monorepo)

A branch model compatible with feature work, release promotion and hotfixes but adapted to avoid full-monorepo releases for every change.

Branch types:

- `main` - production-ready state for the whole repository. Protected: requires PR review, code owners and successful CI for affected paths.
- `develop` - integration branch for staging; merges go here from `feature/*` branches once PR checks pass.
- `feature/<scope>/<short-desc>` - developer branches. `scope` should follow the directory name (e.g. `feature/user-service/add-login`).
- `release/<version>` - prepares a release; used to cut release candidates when multiple services are promoted together.
- `hotfix/<issue>` - urgent fixes applied on top of `main`.

---

## Local development and E2E testing

- Use `compose.yml` (root) to bring up only required services for local E2E tests. Example minimal stack for E2E:
	- `zipkin`, `service-discovery`, `cloud-config`, `api-gateway`, `user-service`, `product-service`, `order-service`, `payment-service`, `shipping-service`
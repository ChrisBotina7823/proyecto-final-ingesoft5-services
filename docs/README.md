# Documentation

This documentation covers all components of the e-commerce microservices platform, including infrastructure, services, testing strategies, and CI/CD pipeline.

## Structure

- **infrastructure/** - Infrastructure as Code and deployment configurations
  - **ansible/** - Remote host provisioning and Jenkins/SonarQube deployment
  - **jenkins/** - Jenkins configuration with Docker Compose
  - **helm/** - Helm charts for observability stack
  - **kubernetes/** - Kubernetes manifests with Kustomize
  - **terraform/** - Modular infrastructure provisioning (AKS, VMs)

- **microservices/** - Service architecture and implementation details
  - Fixes applied to original codebase
  - Resilience patterns implementation
  - Health checks and business metrics

- **testing/** - Testing strategies and implementation
  - Unit and integration tests
  - End-to-end tests with Cypress
  - Performance tests with Locust

- **ci-cd/** - Jenkins pipeline and deployment workflow
  - Build and test phases
  - Security scanning
  - Multi-environment deployment
  - Notifications and reporting

## Quick Start

Each module contains specific setup instructions and usage examples. Navigate to the relevant section for detailed information.

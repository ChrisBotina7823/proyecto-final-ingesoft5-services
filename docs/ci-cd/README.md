# CI/CD Pipeline

Jenkins-based continuous integration and deployment pipeline with security scanning and multi-environment deployment.

## Pipeline Stages

### 1. Install Dependencies

Installs required build tools and dependencies for all microservices.

```groovy
stage('Install Dependencies') {
    steps {
        sh 'mvn clean install -DskipTests'
    }
}
```

Downloads Maven dependencies and prepares workspace.

### 2. Compile

Compiles all microservices source code.

```groovy
stage('Compile') {
    steps {
        sh 'mvn compile'
    }
}
```

Validates syntax and generates bytecode.

### 3. Test

Executes unit and integration tests.

```groovy
stage('Test') {
    steps {
        sh 'mvn test verify'
    }
}
```

Runs all JUnit tests and generates reports. Publishes test results to Jenkins.

### 4. SonarQube Analysis

Static code analysis and quality gate verification.

```groovy
stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQube') {
            sh 'mvn sonar:sonar'
        }
    }
}
```

Analyzes code smells, bugs, security vulnerabilities, code coverage, and duplications.

Quality gate must pass to proceed.

### 5. Generate Version

Semantic versioning based on commit messages.

```groovy
stage('Generate Version') {
    steps {
        script {
            def commitMessage = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
            
            if (commitMessage.startsWith('BREAKING CHANGE:')) {
                version = incrementMajor(currentVersion)
            } else if (commitMessage.startsWith('feat:')) {
                version = incrementMinor(currentVersion)
            } else {
                version = incrementPatch(currentVersion)
            }
            
            env.VERSION = version
        }
    }
}
```

Versioning rules:
- `BREAKING CHANGE:` - Major version (1.0.0 → 2.0.0)
- `feat:` - Minor version (1.0.0 → 1.1.0)
- `fix:` - Patch version (1.0.0 → 1.0.1)

### 6. Build and Push Images

Builds Docker images and pushes to registry.

```groovy
stage('Build and Push Images') {
    steps {
        script {
            services.each { service ->
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${service}:${VERSION} services/${service}
                    docker push ${DOCKER_REGISTRY}/${service}:${VERSION}
                """
            }
        }
    }
}
```

Tags images with generated version.

### 7. Security Scan

Trivy scans Docker images for vulnerabilities.

```groovy
stage('Security Scan') {
    steps {
        script {
            services.each { service ->
                sh """
                    trivy image --format json --output trivy-${service}.json \
                        ${DOCKER_REGISTRY}/${service}:${VERSION}
                """
            }
        }
    }
}
```

Scans OS packages, application dependencies, and known CVEs.

Results archived as JSON for review.

### 8. Deploy to Dev

Deploys services to development environment.

```groovy
stage('Deploy to Dev') {
    steps {
        script {
            withKubeConfig([credentialsId: 'kubeconfig-dev']) {
                sh 'kubectl apply -k infra/kubernetes/overlays/dev'
                sh 'kubectl rollout status deployment -n dev --timeout=5m'
            }
        }
    }
}
```

Uses Kustomize overlays for environment-specific configuration.

### 9. E2E Tests

Runs Cypress end-to-end tests against dev environment.

```groovy
stage('E2E Tests') {
    steps {
        sh 'kubectl port-forward -n dev svc/api-gateway 8080:8080 &'
        sh 'cd tests/e2e && npm run cy:run -- --reporter mochawesome'
    }
}
```

Validates user workflows, API integration, and N+1 query prevention.

Generates HTML reports with screenshots.

### 10. Performance Tests

Executes Locust performance tests.

```groovy
stage('Performance Tests') {
    steps {
        sh """
            cd tests/performance
            locust -f locustfile.py --host=http://localhost:8080 \
                --users 100 --spawn-rate 10 --run-time 5m --headless \
                --html reports/locust-report.html --csv reports/locust-stats
        """
    }
}
```

Validates response times, throughput, and error rates under load.

### 11. Generate Release Notes

Creates release documentation.

```groovy
stage('Generate Release Notes') {
    steps {
        script {
            def notes = """
            # Release ${VERSION}
            
            ## Changes
            ${getCommitsSinceLastTag()}
            
            ## Test Results
            - Unit Tests: PASSED
            - Integration Tests: PASSED
            - E2E Tests: PASSED
            - Performance: PASSED
            """
            
            writeFile file: "RELEASE_NOTES_${VERSION}.md", text: notes
            archiveArtifacts artifacts: "RELEASE_NOTES_${VERSION}.md"
        }
    }
}
```

Includes version number, commit messages since last release, and test results summary.

Accessible in Jenkins build artifacts section.

### 12. Approval Gate

Manual approval required for production deployment.

```groovy
stage('Approval for Production') {
    steps {
        input message: 'Deploy to production?', ok: 'Deploy'
    }
}
```

Prevents automatic production deployments.

### 13. Deploy to Production

Deploys to production environment after approval.

```groovy
stage('Deploy to Production') {
    steps {
        script {
            withKubeConfig([credentialsId: 'kubeconfig-prod']) {
                sh 'kubectl apply -k infra/kubernetes/overlays/prod'
                sh 'kubectl rollout status deployment -n prod --timeout=10m'
            }
        }
    }
}
```

Uses rolling update strategy.

### 14. Notification

Sends deployment notification to Slack.

```groovy
post {
    success {
        slackSend(
            channel: '#devops-deployments',
            color: 'good',
            message: "Deployment ${VERSION} successful"
        )
    }
    failure {
        slackSend(
            channel: '#devops-deployments',
            color: 'danger',
            message: "Deployment ${VERSION} failed"
        )
    }
}
```

## Artifact Archiving

All build artifacts archived automatically:

- Test reports (Surefire, Failsafe)
- Code coverage (JaCoCo)
- Security scans (Trivy JSON)
- E2E reports (Mochawesome HTML, screenshots, videos)
- Performance reports (Locust HTML and CSV)
- SonarQube analysis summary
- Static analysis (Checkstyle, SpotBugs, PMD)
- Release notes
- Build info summary
- Maven dependency tree

Access artifacts from Jenkins UI under Build Artifacts section.

## Environment Variables

Required environment variables:

```bash
DOCKER_REGISTRY=<registry-url>
DOCKER_REGISTRY_USER=<username>
DOCKER_REGISTRY_PASS=<password>
KUBECONFIG_DEV=<base64-kubeconfig>
KUBECONFIG_PROD=<base64-kubeconfig>
SLACK_WEBHOOK=<webhook-url>
SONAR_HOST_URL=<sonarqube-url>
SONAR_AUTH_TOKEN=<token>
```

Stored as Jenkins credentials and environment variables.

## Pipeline Configuration

**Jenkinsfile Location**: Repository root.

**Trigger**: Webhook on push to main branch.

**Agents**: Jenkins agents with Docker, kubectl, Maven, and Node.js.

**Workspace**: Persistent workspace for caching dependencies.

## Monitoring

Pipeline execution monitored through:

- Jenkins Blue Ocean UI
- Slack notifications (#devops-deployments channel)
- Email alerts on failure
- Build trend dashboard

## Rollback

Manual rollback to previous version:

```bash
kubectl rollout undo deployment/<service-name> -n prod
```

Or deploy specific version:

```bash
kubectl set image deployment/<service-name> \
    <container-name>=<registry>/<service>:<previous-version> -n prod
```

## Best Practices

- Always review release notes before production deployment
- Monitor application logs after deployment
- Verify health checks pass before completing deployment
- Review security scan results regularly
- Track performance trends over time
- Keep approval window reasonable
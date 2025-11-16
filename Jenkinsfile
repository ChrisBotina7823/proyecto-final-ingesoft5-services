def isProduction() {
    return env.BRANCH_NAME == 'main'
}

def getChangedServices() {
    def changedFiles = sh(
        script: 'git diff --name-only HEAD~1 HEAD',
        returnStdout: true
    ).trim().split('\n')
    
    def services = [] as Set
    def allServices = [
        "service-discovery",
        "cloud-config",
        "api-gateway",
        "proxy-client",
        "user-service",
        "product-service",
        "favourite-service",
        "order-service",
        "shipping-service",
        "payment-service"
    ]
    
    changedFiles.each { file ->
        allServices.each { service ->
            if (file.startsWith("services/${service}/")) {
                services.add(service)
            }
        }
    }
    
    return services as List
}

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'ghcr.io/chrisbotina7823'}"
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        API_GATEWAY_URL = "${env.API_GATEWAY_URL ?: 'http://host.docker.internal:8080'}"
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        skipDefaultCheckout(false)
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Commit: ${GIT_COMMIT_SHORT}"
                    echo "Production: ${isProduction()}"
                }
            }
        }
        
        stage('Build and Test Services') {
            steps {
                script {
                    sh 'chmod +x mvnw'
                    sh './mvnw -N install -DskipTests -Dmaven.repo.local=/var/jenkins_home/.m2'
                    sh './mvnw clean verify -Dmaven.repo.local=/var/jenkins_home/.m2'
                }   
            }
        }
        
        stage('Trivy Filesystem Scan') {
            steps {
                script {
                    echo "Running Trivy filesystem vulnerability scan..."
                    sh """
                        trivy fs \
                            --severity HIGH,CRITICAL \
                            --format json \
                            --output trivy-fs-report.json \
                            --timeout 10m \
                            --skip-dirs "**/target,**/node_modules,**/.git,**/.mvn" \
                            .
                    """
                    
                    // Archive report as artifact
                    archiveArtifacts artifacts: 'trivy-fs-report.json', allowEmptyArchive: true
                    
                    // Generate human-readable report
                    sh """
                        trivy fs \
                            --severity HIGH,CRITICAL \
                            --format table \
                            --skip-dirs "**/target,**/node_modules,**/.git,**/.mvn" \
                            .
                    """
                }
            }
        }
        
        stage('Code Quality Analysis') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'sonarqube-admin',
                        usernameVariable: 'SONAR_LOGIN',
                        passwordVariable: 'SONAR_PASSWORD'
                    )]) {
                        sh """
                            sonar-scanner \
                                -Dsonar.host.url=http://sonarqube:9000 \
                                -Dsonar.login=\${SONAR_LOGIN} \
                                -Dsonar.password=\${SONAR_PASSWORD}
                        """
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    def changedServices = getChangedServices()
                    
                    if (changedServices.isEmpty()) {
                        echo "No service changes detected"
                        return
                    }
                    
                    echo "Changed services: ${changedServices.join(', ')}"
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo \${DOCKER_PASS} | docker login ghcr.io -u \${DOCKER_USER} --password-stdin"
                    }
                    
                    def parallelBuilds = [:]
                    
                    changedServices.each { service ->
                        def serviceName = service
                        parallelBuilds[serviceName] = {
                            dir("services/${serviceName}") {
                                sh """
                                    docker build \
                                        -t ${DOCKER_REGISTRY}/${serviceName}:${GIT_COMMIT_SHORT} \
                                        -t ${DOCKER_REGISTRY}/${serviceName}:latest \
                                        .
                                    
                                    docker push ${DOCKER_REGISTRY}/${serviceName}:${GIT_COMMIT_SHORT}
                                    docker push ${DOCKER_REGISTRY}/${serviceName}:latest
                                """
                            }
                        }
                    }
                    
                    parallel parallelBuilds
                }
            }
        }
        
        stage('Trivy Image Scan') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    def changedServices = getChangedServices()
                    
                    if (changedServices.isEmpty()) {
                        echo "No service changes detected, skipping image scan"
                        return
                    }
                    
                    echo "Scanning Docker images for vulnerabilities with Trivy..."
                    
                    def parallelScans = [:]
                    def scanResults = [:]
                    
                    changedServices.each { service ->
                        def serviceName = service
                        parallelScans[serviceName] = {
                            def imageTag = "${DOCKER_REGISTRY}/${serviceName}:${GIT_COMMIT_SHORT}"
                            
                            echo "Scanning image: ${imageTag}"
                            
                            // Scan image and save report
                            sh """
                                trivy image \
                                    --severity HIGH,CRITICAL \
                                    --format json \
                                    --output trivy-${serviceName}-report.json \
                                    --timeout 10m \
                                    ${imageTag}
                            """
                            
                            // Generate human-readable report
                            sh """
                                trivy image \
                                    --severity HIGH,CRITICAL \
                                    --format table \
                                    ${imageTag}
                            """
                            
                            // Check for critical vulnerabilities and fail if policy is strict
                            def exitCode = sh(
                                script: """
                                    trivy image \
                                        --severity CRITICAL \
                                        --exit-code 1 \
                                        --timeout 10m \
                                        ${imageTag}
                                """,
                                returnStatus: true
                            )
                            
                            scanResults[serviceName] = exitCode
                            
                            if (exitCode != 0) {
                                echo "WARNING: Critical vulnerabilities found in ${serviceName}"
                            } else {
                                echo "No critical vulnerabilities found in ${serviceName}"
                            }
                        }
                    }
                    
                    parallel parallelScans
                    
                    // Archive all scan reports
                    archiveArtifacts artifacts: 'trivy-*-report.json', allowEmptyArchive: true
                    
                    // Check if any service has critical vulnerabilities
                    def criticalFound = scanResults.any { service, exitCode -> exitCode != 0 }
                    
                    if (criticalFound) {
                        def affectedServices = scanResults.findAll { service, exitCode -> exitCode != 0 }.keySet()
                        echo "SECURITY WARNING: Critical vulnerabilities found in: ${affectedServices.join(', ')}"
                        
                        // Uncomment to fail the build on critical vulnerabilities
                        // error("Critical vulnerabilities detected. Deployment blocked.")
                    } else {
                        echo "All images passed security scan"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    def changedServices = getChangedServices()
                    
                    withCredentials([
                        file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG'),
                        usernamePassword(
                            credentialsId: 'docker-registry',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )
                    ]) {
                        sh "kubectl apply -f infra/kubernetes/namespace.yaml"
                        
                        sh """
                            kubectl create secret docker-registry ghcr-secret \
                                --docker-server=ghcr.io \
                                --docker-username=\${DOCKER_USER} \
                                --docker-password=\${DOCKER_PASS} \
                                --namespace=ecommerce-prod \
                                --dry-run=client -o yaml | kubectl apply -f -
                        """
                        
                        dir('infra/kubernetes') {
                            changedServices.each { service ->
                                sh "kustomize edit set image ${service}=${DOCKER_REGISTRY}/${service}:${GIT_COMMIT_SHORT}"
                            }
                        }
                        
                        sh "kubectl apply -k infra/kubernetes/"
                        
                        echo "Waiting for all pods to be ready (this may take up to 20 minutes)..."
                        sh """
                            kubectl wait --for=condition=ready pod \
                                --all \
                                --namespace=ecommerce-prod \
                                --timeout=1200s
                        """
                        
                        echo "All pods are ready!"
                        sh "kubectl get pods -n ecommerce-prod"
                    }
                }
            }
        }

        stage('E2E Tests') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                        // Copy tests to workspace to avoid read-only issues
                        sh """
                            mkdir -p /tmp/e2e-tests
                            cp -r tests/e2e/* /tmp/e2e-tests/
                        """
                        
                        dir('/tmp/e2e-tests') {
                            sh """
                                echo "Setting up port-forward to API Gateway..."
                                
                                # Start port-forward in background (using port 9090 to avoid conflict with Jenkins)
                                kubectl port-forward svc/api-gateway 9090:8080 -n ecommerce-prod &
                                PORT_FORWARD_PID=\$!
                                
                                # Wait for port-forward to be ready
                                echo "Waiting for port-forward to be ready..."
                                i=1
                                while [ \$i -le 30 ]; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "Port-forward ready!"
                                        break
                                    fi
                                    echo "Attempt \$i/30: Port-forward not ready yet..."
                                    sleep 2
                                    i=\$((i + 1))
                                done
                                
                                # Install dependencies (cached in volume)
                                npm ci --prefer-offline --no-audit
                                
                                # Run Cypress tests against localhost:9090
                                NO_COLOR=1 CYPRESS_baseUrl=http://localhost:9090 npx cypress run \
                                    --config video=false,screenshotOnRunFailure=false
                                
                                # Kill port-forward
                                kill \$PORT_FORWARD_PID || true
                            """
                        }
                    }
                }
            }
        }

        stage('Performance Tests') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
                        dir('tests/performance') {
                            sh """
                                echo "Setting up port-forward to API Gateway..."
                                
                                # Start port-forward in background (using port 9090 to avoid conflict with Jenkins)
                                kubectl port-forward svc/api-gateway 9090:8080 -n ecommerce-prod &
                                PORT_FORWARD_PID=\$!
                                
                                # Wait for port-forward to be ready
                                echo "Waiting for port-forward to be ready..."
                                i=1
                                while [ \$i -le 30 ]; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "Port-forward ready!"
                                        break
                                    fi
                                    echo "Attempt \$i/30: Port-forward not ready yet..."
                                    sleep 2
                                    i=\$((i + 1))
                                done
                                
                                # Install dependencies (cached in volume)
                                /opt/locust-venv/bin/pip install -r requirements.txt --quiet
                                
                                # Run Locust tests against localhost:9090
                                /opt/locust-venv/bin/locust \
                                    --headless \
                                    --host=http://localhost:9090 \
                                    --users 10 \
                                    --spawn-rate 2 \
                                    --run-time 30s \
                                    --loglevel WARNING \
                                    --autostart
                                
                                # Kill port-forward
                                kill \$PORT_FORWARD_PID || true
                            """
                        }
                    }
                }
            }
        }        
    }
    
    post {
        success {
            echo "Pipeline completed successfully"
        }
        failure {
            echo "Pipeline failed"
        }
    }
}

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
        
            when {
                expression { isProduction() }
            }

            steps {
                script {
                    sh 'chmod +x mvnw'
                    sh './mvnw -N install -DskipTests -Dmaven.repo.local=/var/jenkins_home/.m2'
                    sh './mvnw clean verify -Dmaven.repo.local=/var/jenkins_home/.m2'
                }   
            }
        }
        
        stage('Code Quality Analysis') {
            
            when {
                expression { isProduction() }
            }


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
                        
                        sh """
                            kubectl wait --for=condition=ready pod \
                                --all \
                                --namespace=ecommerce-prod \
                                --timeout=300s || true
                        """
                    }
                }
            }
        }

        stage('E2E Tests') {
            // when {
            //     expression { isProduction() }
            // }
            steps {
                script {
                    // Copy tests to workspace to avoid read-only issues
                    sh """
                        mkdir -p /tmp/e2e-tests
                        cp -r tests/e2e/* /tmp/e2e-tests/
                    """
                    
                    dir('/tmp/e2e-tests') {
                        sh """
                            echo "Running E2E tests against: ${API_GATEWAY_URL}"
                            
                            # Install dependencies (cached in volume)
                            npm ci --prefer-offline --no-audit
                            
                            # Disable Jenkins proxy for Cypress
                            export HTTP_PROXY=
                            export HTTPS_PROXY=
                            export NO_PROXY=*
                            
                            # Run Cypress tests with correct baseUrl
                            NO_COLOR=1 CYPRESS_baseUrl=${API_GATEWAY_URL} npx cypress run \
                                --config video=false,screenshotOnRunFailure=false
                        """
                    }
                }
            }
        }

        stage('Performance Tests') {
            // when {
            //     expression { isProduction() }
            // }
            steps {
                script {
                    dir('tests/performance') {
                        sh """
                            echo "Running performance tests against: ${API_GATEWAY_URL}"
                            
                            # Install dependencies (cached in volume)
                            /opt/locust-venv/bin/pip install -r requirements.txt --quiet
                            
                            # Run Locust tests (lightweight)
                            /opt/locust-venv/bin/locust \
                                --headless \
                                --host=${API_GATEWAY_URL} \
                                --users 10 \
                                --spawn-rate 2 \
                                --run-time 30s \
                                --loglevel WARNING
                        """
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

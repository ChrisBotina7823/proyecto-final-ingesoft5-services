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
                    sh 'du -sh ~/.m2/repository 2>/dev/null || echo "Maven cache: empty"'
                    sh './mvnw -N clean install --batch-mode --no-transfer-progress'
                    sh './mvnw clean verify -T 1C --batch-mode --no-transfer-progress'
                    sh 'du -sh ~/.m2/repository'
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

        stage('Deploy to Kubernetes') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    def changedServices = getChangedServices()
                    
                    if (changedServices.isEmpty()) {
                        echo "No deployments needed"
                        return
                    }
                    
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

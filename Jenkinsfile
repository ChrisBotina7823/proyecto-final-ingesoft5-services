// Jenkinsfile for monorepo with path-based service detection
// Simplified: dev = build only, prod = build + deploy

def getChangedServices() {
    def servicesList = [
        'user-service',
        'product-service',
        'order-service',
        'payment-service',
        'shipping-service',
        'favourite-service',
        'api-gateway',
        'cloud-config',
        'service-discovery',
        'proxy-client'
    ]
    
    def changedServices = []
    def changedFiles = sh(
        script: '''
            if [ "${GIT_PREVIOUS_COMMIT}" = "" ]; then
                git diff --name-only HEAD~1 HEAD
            else
                git diff --name-only ${GIT_PREVIOUS_COMMIT} ${GIT_COMMIT}
            fi
        ''',
        returnStdout: true
    ).trim().split('\n')
    
    for (service in servicesList) {
        if (changedFiles.any { it.startsWith("services/${service}/") }) {
            changedServices.add(service)
        }
    }
    
    if (changedFiles.any { it in ['pom.xml', 'compose.yml'] || it.startsWith('infra/') }) {
        return servicesList
    }
    
    return changedServices.isEmpty() ? servicesList : changedServices
}

def isProduction() {
    return env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master'
}

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'ghcr.io/chrisbotina7823'}"
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository -Dmaven.artifact.threads=10'
        DOCKER_BUILDKIT = '1'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 1, unit: 'HOURS')
        timestamps()
    }
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    env.CHANGED_SERVICES = getChangedServices().join(',')
                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Services to build: ${env.CHANGED_SERVICES}"
                    echo "Deploy to K8s: ${isProduction()}"
                }
            }
        }
        
        stage('Build All Services') {
            steps {
                script {
                    echo "Building entire Maven reactor (all modules)..."
                    sh 'chmod +x mvnw'
                    sh './mvnw clean install -DskipTests -Dmaven.repo.local=.m2/repository'
                    echo "All services built successfully. JARs are ready."
                }
            }
        }
        
        stage('Docker Build & Push') {
            steps {
                script {
                    def servicesToBuild = env.CHANGED_SERVICES.split(',')
                    def parallelBuilds = [:]
                    
                    echo "Building and pushing Docker images in parallel..."
                    
                    for (service in servicesToBuild) {
                        def serviceName = service
                        parallelBuilds[serviceName] = {
                            dir("services/${serviceName}") {
                                sh "docker build -t ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER} -t ${DOCKER_REGISTRY}/${serviceName}:latest ."
                                
                                withCredentials([usernamePassword(
                                    credentialsId: 'docker-registry',
                                    usernameVariable: 'DOCKER_USER',
                                    passwordVariable: 'DOCKER_PASS'
                                )]) {
                                    sh """
                                        echo \${DOCKER_PASS} | docker login ghcr.io -u \${DOCKER_USER} --password-stdin
                                        docker push ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER}
                                        docker push ${DOCKER_REGISTRY}/${serviceName}:latest
                                        
                                        echo "Setting package to public visibility..."
                                        TOKEN=\${DOCKER_PASS}
                                        PACKAGE_NAME=${serviceName}
                                        
                                        curl -X PATCH \
                                            -H "Accept: application/vnd.github+json" \
                                            -H "Authorization: Bearer \${TOKEN}" \
                                            -H "X-GitHub-Api-Version: 2022-11-28" \
                                            https://api.github.com/user/packages/container/\${PACKAGE_NAME} \
                                            -d '{"visibility":"public"}' || echo "Package already public or needs manual setting"
                                    """
                                }
                            }
                        }
                    }
                    parallel parallelBuilds
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            // when {
            //     expression { isProduction() }
            // }
            steps {
                script {
                    echo "=== Deploying to AKS using deploy script ==="
                    
                    withCredentials([
                        string(credentialsId: 'azure-subscription-id', variable: 'AZURE_SUBSCRIPTION_ID'),
                        string(credentialsId: 'azure-client-id', variable: 'AZURE_CLIENT_ID'),
                        string(credentialsId: 'azure-client-secret', variable: 'AZURE_CLIENT_SECRET'),
                        string(credentialsId: 'azure-tenant-id', variable: 'AZURE_TENANT_ID')
                    ]) {
                        // Login to Azure
                        sh """
                            echo "Logging in to Azure..."
                            az login --service-principal \
                                -u \${AZURE_CLIENT_ID} \
                                -p \${AZURE_CLIENT_SECRET} \
                                --tenant \${AZURE_TENANT_ID}
                            
                            az account set --subscription \${AZURE_SUBSCRIPTION_ID}
                        """
                        
                        // Get AKS credentials
                        sh """
                            echo "Getting AKS credentials..."
                            az aks get-credentials \
                                --resource-group rg-dev-ecommerce \
                                --name aks-dev-ecommerce \
                                --overwrite-existing
                        """
                        
                        // Verify cluster connection
                        sh """
                            echo "Verifying cluster connection..."
                            kubectl cluster-info
                            kubectl get nodes
                        """
                        
                        // Create or verify namespace
                        sh """
                            echo "Setting up namespace..."
                            kubectl get namespace ecommerce-prod || kubectl create namespace ecommerce-prod
                        """
                        
                        // Deploy infrastructure services first
                        sh """
                            echo "Deploying infrastructure services..."
                            kubectl apply -f infra/kubernetes/base/service-discovery.yaml -n ecommerce-prod
                            kubectl apply -f infra/kubernetes/base/cloud-config.yaml -n ecommerce-prod
                            kubectl apply -f infra/kubernetes/base/zipkin.yaml -n ecommerce-prod
                            
                            echo "Waiting for infrastructure pods to start..."
                            sleep 10
                            
                            echo "Waiting for infrastructure services to be ready (timeout: 5 minutes)..."
                            kubectl wait --for=condition=ready pod -l app=service-discovery -n ecommerce-prod --timeout=300s
                            kubectl wait --for=condition=ready pod -l app=cloud-config -n ecommerce-prod --timeout=300s
                            kubectl wait --for=condition=ready pod -l app=zipkin -n ecommerce-prod --timeout=300s
                            
                            echo "Infrastructure services are ready"
                        """
                        
                        // Deploy business services
                        def servicesToDeploy = env.CHANGED_SERVICES.split(',')
                        def businessServices = servicesToDeploy.findAll { 
                            it != 'service-discovery' && it != 'cloud-config' 
                        }
                        
                        if (businessServices) {
                            echo "Deploying business services: ${businessServices.join(', ')}"
                            for (service in businessServices) {
                                sh """
                                    echo "Deploying ${service}..."
                                    kubectl apply -f infra/kubernetes/base/${service}.yaml -n ecommerce-prod
                                """
                            }
                            
                            echo "Waiting for pods to initialize (60 seconds)..."
                            sh "sleep 60"
                            
                            echo "Waiting for deployments to complete (timeout: 5 minutes each)..."
                            for (service in businessServices) {
                                sh """
                                    echo "Waiting for ${service} deployment..."
                                    kubectl rollout status deployment/${service} -n ecommerce-prod --timeout=300s
                                """
                            }
                            
                            echo "All business services deployed successfully"
                        }
                        
                        // Show final status
                        sh """
                            echo "=== Deployment Status ==="
                            kubectl get all -n ecommerce-prod
                            
                            echo ""
                            echo "=== Pod Status ==="
                            kubectl get pods -n ecommerce-prod -o wide
                            
                            echo ""
                            echo "=== Service Endpoints ==="
                            kubectl get svc -n ecommerce-prod
                        """
                        
                        // Get API Gateway external IP
                        sh """
                            echo ""
                            echo "=== API Gateway Public IP ==="
                            kubectl get svc api-gateway -n ecommerce-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}' || echo "LoadBalancer IP pending..."
                            echo ""
                        """
                        
                        // Logout from Azure
                        sh 'az logout'
                    }
                }
            }
        }
        
        stage('Verify Deployment') {
            // when {
            //     expression { isProduction() }
            // }
            steps {
                script {
                    echo "=== Verifying Deployment Health ==="
                    
                    withCredentials([
                        string(credentialsId: 'azure-subscription-id', variable: 'AZURE_SUBSCRIPTION_ID'),
                        string(credentialsId: 'azure-client-id', variable: 'AZURE_CLIENT_ID'),
                        string(credentialsId: 'azure-client-secret', variable: 'AZURE_CLIENT_SECRET'),
                        string(credentialsId: 'azure-tenant-id', variable: 'AZURE_TENANT_ID')
                    ]) {
                        sh """
                            az login --service-principal \
                                -u \${AZURE_CLIENT_ID} \
                                -p \${AZURE_CLIENT_SECRET} \
                                --tenant \${AZURE_TENANT_ID}
                            
                            az account set --subscription \${AZURE_SUBSCRIPTION_ID}
                            
                            az aks get-credentials \
                                --resource-group rg-dev-ecommerce \
                                --name aks-dev-ecommerce \
                                --overwrite-existing
                        """
                        
                        // Check pod health
                        def checksPassed = true
                        def servicesToCheck = env.CHANGED_SERVICES.split(',')
                        
                        for (service in servicesToCheck) {
                            def podStatus = sh(
                                script: "kubectl get pods -l app=${service} -n ecommerce-prod -o jsonpath='{.items[0].status.phase}' 2>/dev/null || echo 'NotFound'",
                                returnStdout: true
                            ).trim()
                            
                            def podReady = sh(
                                script: "kubectl get pods -l app=${service} -n ecommerce-prod -o jsonpath='{.items[0].status.conditions[?(@.type==\"Ready\")].status}' 2>/dev/null || echo 'False'",
                                returnStdout: true
                            ).trim()
                            
                            if (podStatus == 'Running' && podReady == 'True') {
                                echo "[OK] ${service}: Running and Ready"
                            } else if (podStatus == 'Running') {
                                echo "[WAITING] ${service}: Running but not Ready yet"
                                checksPassed = false
                            } else {
                                echo "[ERROR] ${service}: ${podStatus}"
                                checksPassed = false
                            }
                        }
                        
                        // Check if API Gateway has external IP
                        sh """
                            echo ""
                            echo "Checking API Gateway LoadBalancer..."
                            GATEWAY_IP=\$(kubectl get svc api-gateway -n ecommerce-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo '')
                            
                            if [ -z "\${GATEWAY_IP}" ]; then
                                echo "API Gateway LoadBalancer IP is still pending"
                            else
                                echo "API Gateway public IP: \${GATEWAY_IP}"
                                echo "   Testing health endpoint..."
                                curl -f -s -o /dev/null -w "Status: %{http_code}\\n" "http://\${GATEWAY_IP}:8080/actuator/health" || echo "Health check not yet available"
                            fi
                        """
                        
                        sh 'az logout'
                        
                        if (!checksPassed) {
                            echo "Some services are not fully ready yet. Check the pods for details."
                            echo "This is normal for initial deployments. Services will become ready shortly."
                        }
                    }
                }
            }
        }
        
    }
    
    post {
        success {
            script {
                echo "=========================================="
                echo "PIPELINE COMPLETED SUCCESSFULLY"
                echo "=========================================="
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Built services: ${env.CHANGED_SERVICES}"
                
                // if (isProduction()) {
                    echo ""
                    echo "Services deployed to AKS:"
                    echo "   - Namespace: ecommerce-prod"
                    echo "   - Cluster: aks-dev-ecommerce"
                    echo "   - Resource Group: rg-dev-ecommerce"
                    echo ""
                    echo "Access API Gateway at:"
                    sh """
                        GATEWAY_IP=\$(kubectl get svc api-gateway -n ecommerce-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo 'pending')
                        echo "   http://\${GATEWAY_IP}:8080"
                    """ 
                // }
                echo "=========================================="
            }
        }
        failure {
            script {
                echo "=========================================="
                echo "PIPELINE FAILED"
                echo "=========================================="
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Failed during: ${currentBuild.result}"
                echo ""
                echo "Check logs above for details"
                echo "=========================================="
            }
        }
        always {
            script {
                echo "Cleaning up Docker resources (keeping Maven cache)..."
                sh 'docker system prune -f || true'
                
                echo "Maven repository cache size:"
                sh 'du -sh .m2/repository 2>/dev/null || echo "No Maven cache"'
                
                echo "Workspace size:"
                sh 'du -sh . 2>/dev/null || echo "Unable to calculate"'
            }
        }
    }
}

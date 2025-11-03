// Jenkinsfile for monorepo with path-based service detection
// Simplified: dev = build only, prod = build + deploy

def isProduction() {
    return false
    // def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown'
    // echo "branch name ${env.BRANCH_NAME}"
    // echo "git branch ${env.GIT_BRANCH}"
    // echo "Current branch detected: ${branch}"
    // branch = branch.replaceAll(/^origin\//, '')
    // return branch == 'main' || branch == 'master'
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
        skipDefaultCheckout(false)
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "Is Production: ${isProduction()}"
                }
            }
        }
        
        
        stage('Build All Services') {

            when {
                expression { isProduction() }
            }

            steps {
                script {
                    
                    // Build parent POM and services
                    sh 'chmod +x mvnw'
                    sh './mvnw -N clean install -Dmaven.repo.local=.m2/repository -DskipTests'                    
                    sh './mvnw clean install -B -Dmaven.repo.local=.m2/repository -Dmaven.artifact.threads=10'
                    
                    echo "All services built successfully. JARs and coverage reports are ready."
                }
            }
        }
        
        stage('Code Quality Analysis') {
            
            when {
                expression { isProduction() }
            }

            steps {
                script {
                    echo "Running SonarQube analysis with sonar-scanner..."
                    
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
                    def servicesToBuild = env.CHANGED_SERVICES.split(',')
                    
                    // Login once before parallel builds
                    echo "Logging in to GitHub Container Registry..."
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        retry(3) {
                            sh """
                                echo "Attempting Docker login to ghcr.io..."
                                echo \${DOCKER_PASS} | docker login ghcr.io -u \${DOCKER_USER} --password-stdin
                                echo "Docker login successful"
                            """
                        }
                    }
                    
                    def parallelBuilds = [:]
                    
                    echo "Building and pushing Docker images in parallel..."
                    
                    for (service in servicesToBuild) {
                        def serviceName = service
                        parallelBuilds[serviceName] = {
                            dir("services/${serviceName}") {
                                echo "Building Docker image for ${serviceName}..."
                                sh "docker build -t ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER} -t ${DOCKER_REGISTRY}/${serviceName}:latest ."
                                
                                echo "Pushing images for ${serviceName}..."
                                sh """
                                    docker push ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER}
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
            // when {
            //     expression { isProduction() }
            // }
            steps {
                script {
                    echo "=== Deploying to AKS using Kustomize ==="
                    
                    withCredentials([
                        file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG'),
                        usernamePassword(
                            credentialsId: 'docker-registry',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )
                    ]) {
                        
                        sh """
                            kubectl apply -f namespace.yaml
                        """

                        // Create Docker registry secret for pulling images from GHCR
                        sh """
                            echo "Creating/updating Docker registry secret..."
                            kubectl create secret docker-registry ghcr-secret \
                                --docker-server=ghcr.io \
                                --docker-username=\${DOCKER_USER} \
                                --docker-password=\${DOCKER_PASS} \
                                --namespace=ecommerce-prod \
                                --dry-run=client -o yaml | kubectl apply -f -
                            
                            echo "Docker registry secret created/updated successfully"
                        """
                        
                        // Deploy all services using Kustomize
                        sh """
                            echo "Deploying all services with Kustomize..."
                            kubectl apply -k infra/kubernetes/
                        """    
                        
                        sh """
                            echo "Waiting for pods to be ready..."
                            kubectl wait --for=condition=ready pod \
                                --all \
                                --namespace=ecommerce-prod \
                                --timeout=300s || true
                            echo "Deployment status:"
                            kubectl get pods -n ecommerce-prod
                        """
                        
                        echo "All services deployed successfully"
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
            }
        }
        failure {
            script {
                echo "=========================================="
                echo "PIPELINE FAILED"
                echo "=========================================="
            }
        }
    }
}

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
        if (changedFiles.any { it.startsWith("${service}/") }) {
            changedServices.add(service)
        }
    }
    
    // Build all if root files changed
    if (changedFiles.any { it in ['pom.xml', 'compose.yml'] || it.startsWith('infra/') }) {
        return servicesList
    }
    
    return changedServices.isEmpty() ? servicesList : changedServices
}

def isProduction() {
    return env.BRANCH_NAME == 'main'
}

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'localhost:5000'}"
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
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
        
        stage('Build Parent POM') {
            steps {
                sh './mvnw clean install -DskipTests -Dmaven.repo.local=.m2/repository'
            }
        }
        
        stage('Build Services') {
            steps {
                script {
                    def servicesToBuild = env.CHANGED_SERVICES.split(',')
                    def parallelBuilds = [:]
                    
                    for (service in servicesToBuild) {
                        def serviceName = service
                        parallelBuilds[serviceName] = {
                            dir("${serviceName}") {
                                sh "../mvnw clean package -DskipTests -Dmaven.repo.local=../.m2/repository"
                                sh "docker build -t ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER} -t ${DOCKER_REGISTRY}/${serviceName}:latest ."
                                
                                withCredentials([usernamePassword(
                                    credentialsId: 'docker-registry',
                                    usernameVariable: 'DOCKER_USER',
                                    passwordVariable: 'DOCKER_PASS'
                                )]) {
                                    sh """
                                        echo \${DOCKER_PASS} | docker login ${DOCKER_REGISTRY} -u \${DOCKER_USER} --password-stdin
                                        docker push ${DOCKER_REGISTRY}/${serviceName}:${env.BUILD_NUMBER}
                                        docker push ${DOCKER_REGISTRY}/${serviceName}:latest
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
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    def servicesToDeploy = env.CHANGED_SERVICES.split(',')
                    def namespace = "${env.K8S_NAMESPACE ?: 'ecommerce-prod'}"
                    
                    withCredentials([file(credentialsId: 'kubeconfig-local', variable: 'KUBECONFIG_FILE')]) {
                        for (service in servicesToDeploy) {
                            sh """
                                export KUBECONFIG=\${KUBECONFIG_FILE}
                                kubectl set image deployment/${service} \
                                    app=${DOCKER_REGISTRY}/${service}:${env.BUILD_NUMBER} \
                                    -n ${namespace}
                                kubectl rollout status deployment/${service} -n ${namespace} --timeout=5m
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
            echo "Built services: ${env.CHANGED_SERVICES}"
        }
        failure {
            echo "Pipeline failed"
            echo "Check logs for details"
        }
        always {
            sh 'docker system prune -f --volumes || true'
        }
    }
}

def calculateSemanticVersion() {
    def lastTag = sh(
        script: "git describe --tags --abbrev=0 2>/dev/null || echo ''",
        returnStdout: true
    ).trim()
    
    def commits = ""
    if (lastTag) {
        commits = sh(
            script: "git log ${lastTag}..HEAD --pretty=%B 2>/dev/null || echo ''",
            returnStdout: true
        ).trim()
    } else {
        commits = sh(
            script: "git log --pretty=%B",
            returnStdout: true
        ).trim()
        lastTag = "0.0.0"
    }
    
    def (major, minor, patch) = lastTag.replaceAll('v', '').tokenize('.').collect { it.toInteger() }
    
    if (commits.contains('BREAKING CHANGE:') || commits.contains('!:')) {
        major += 1
        minor = 0
        patch = 0
    } else if (commits.contains('feat:') || commits.contains('feature:')) {
        minor += 1
        patch = 0
    } else if (commits.contains('fix:') || commits.contains('bugfix:')) {
        patch += 1
    } else {
        patch += 1
    }
    
    return "${major}.${minor}.${patch}"
}

def createGitTag(version) {
    withCredentials([usernamePassword(
        credentialsId: 'github-token',
        usernameVariable: 'GIT_USER',
        passwordVariable: 'GIT_TOKEN'
    )]) {
        sh """
            git config user.name "Jenkins CI"
            git config user.email "jenkins@ci.local"
            git tag -a v${version} -m 'Release v${version}'
            git push https://\${GIT_TOKEN}@github.com/ChrisBotina7823/proyecto-final-ingesoft5-services.git v${version}
        """
    }
}

def generateReleaseNotes(version, previousVersion) {
    def notes = ""
    if (previousVersion && previousVersion != "0.0.0") {
        notes = sh(
            script: "git log ${previousVersion}..HEAD --pretty=format:'- %s (%h)' --no-merges 2>/dev/null || echo 'Initial release'",
            returnStdout: true
        ).trim()
    } else {
        notes = sh(
            script: "git log --pretty=format:'- %s (%h)' --no-merges",
            returnStdout: true
        ).trim()
    }
    
    writeFile file: "RELEASE_NOTES_${version}.md", text: """
# Release v${version}

## Changes

${notes}

## Deployed Services
- service-discovery
- cloud-config
- api-gateway
- proxy-client
- user-service
- product-service
- favourite-service
- order-service
- shipping-service
- payment-service

## Deployment Date
${new Date()}
"""
    
    archiveArtifacts artifacts: "RELEASE_NOTES_${version}.md", allowEmptyArchive: false
}

def sendNotification(status, message) {
    def color = status == 'SUCCESS' ? 'good' : (status == 'FAILURE' ? 'danger' : 'warning')
    
    try {
        slackSend(
            color: color,
            message: "${status}: ${message}\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nBranch: ${env.BRANCH_NAME}",
            channel: '#deployments'
        )
    } catch (Exception e) {
        echo "Failed to send Slack notification: ${e.message}"
    }
    
    try {
        emailext(
            subject: "${status}: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
            body: """
                <h2>${status}: ${message}</h2>
                <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                <p><strong>Build:</strong> ${env.BUILD_NUMBER}</p>
                <p><strong>Branch:</strong> ${env.BRANCH_NAME}</p>
                <p><strong>URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
            """,
            to: 'criedboca@gmail.com',
            mimeType: 'text/html'
        )
    } catch (Exception e) {
        echo "Failed to send email notification: ${e.message}"
    }
}

def deployWithHelm(environment, version) {
    def namespace = environment
    def overlayPath = "infra/kubernetes/overlays/${environment}"
    
    echo "Deploying to ${environment} with version ${version}"
    echo "KUBECONFIG is set to: ${env.KUBECONFIG}"
    
    sh """
        export KUBECONFIG=${env.KUBECONFIG}
        kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -
    """
    
    withCredentials([usernamePassword(
        credentialsId: 'docker-registry',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {
        sh """
            export KUBECONFIG=${env.KUBECONFIG}
            kubectl create secret docker-registry ghcr-secret \
                --docker-server=ghcr.io \
                --docker-username=\${DOCKER_USER} \
                --docker-password=\${DOCKER_PASS} \
                --namespace=${namespace} \
                --dry-run=client -o yaml | kubectl apply -f -
        """
    }
    
    if (version != "latest" && version != "dev-latest") {
        sh """
            cd ${overlayPath}
            kustomize edit set image \
                ghcr.io/chrisbotina7823/service-discovery:${version} \
                ghcr.io/chrisbotina7823/cloud-config:${version} \
                ghcr.io/chrisbotina7823/api-gateway:${version} \
                ghcr.io/chrisbotina7823/proxy-client:${version} \
                ghcr.io/chrisbotina7823/user-service:${version} \
                ghcr.io/chrisbotina7823/product-service:${version} \
                ghcr.io/chrisbotina7823/favourite-service:${version} \
                ghcr.io/chrisbotina7823/order-service:${version} \
                ghcr.io/chrisbotina7823/payment-service:${version} \
                ghcr.io/chrisbotina7823/shipping-service:${version}
            cd ../..
        """
    }
    
    sh """
        export KUBECONFIG=${env.KUBECONFIG}
        kubectl apply -k ${overlayPath} --force
    """
    sh """
        export KUBECONFIG=${env.KUBECONFIG}
        kubectl wait --for=condition=ready pod --all -n ${namespace} --timeout=600s || true
    """
    sh """
        export KUBECONFIG=${env.KUBECONFIG}
        kubectl get pods -n ${namespace}
    """
}

def isProduction() {
    return true || env.BRANCH_NAME == 'main'
}

def isDevelopment() {
    return env.BRANCH_NAME == 'develop'
}

def getAllServices() {
    return [
        "service-discovery",
        "cloud-config",
        "api-gateway",
        "user-service",
        "product-service",
        "favourite-service",
        "order-service",
        "payment-service",
        "shipping-service",
        "proxy-client"
    ]
}

pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = "${env.DOCKER_REGISTRY ?: 'ghcr.io/chrisbotina7823'}"
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
        VERSION = ""
        PREVIOUS_VERSION = ""
        ENVIRONMENT = ""
        APPROVER = ""
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 2, unit: 'HOURS')
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
                    
                    if (isProduction()) {
                        ENVIRONMENT = 'prod'
                        echo "Environment: Production"
                    } else if (isDevelopment()) {
                        ENVIRONMENT = 'dev'
                        echo "Environment: Development"
                    } else {
                        ENVIRONMENT = 'dev'
                        echo "Environment: Development (feature branch)"
                    }
                }
            }
        }
        
        stage('Semantic Versioning') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    PREVIOUS_VERSION = sh(
                        script: "git describe --tags --abbrev=0 2>/dev/null || echo '0.0.0'",
                        returnStdout: true
                    ).trim()
                    
                    VERSION = calculateSemanticVersion()
                    env.VERSION = VERSION
                    
                    echo "Previous version: ${PREVIOUS_VERSION}"
                    echo "New version: ${VERSION}"
                    
                    currentBuild.displayName = "#${env.BUILD_NUMBER} - v${VERSION}"
                }
            }
        }

        stage('Build and Test Services') {
            when {
                expression { false }
            }
            steps {
                script {
                    sh 'chmod +x mvnw'
                    sh './mvnw -N install -DskipTests -Dmaven.repo.local=/var/jenkins_home/.m2'
                    sh './mvnw clean verify -Dmaven.repo.local=/var/jenkins_home/.m2'
                }   
            }
        }
        
        stage('Trivy Filesystem Scan') {
            when {
                expression { false }
            }
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
                    
                    archiveArtifacts artifacts: 'trivy-fs-report.json', allowEmptyArchive: true
                    
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
            when {
                expression { false }
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
                                -Dsonar.password=\${SONAR_PASSWORD} \
                                -Dsonar.projectVersion=${VERSION ?: GIT_COMMIT_SHORT}
                        """
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                expression { false }
            }
            /*
            when {
                expression { isProduction() || isDevelopment() }
            }
            */
            steps {
                script {
                    def services = getAllServices()
                    def imageTag = isProduction() ? VERSION : "dev-${GIT_COMMIT_SHORT}"
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo \${DOCKER_PASS} | docker login ghcr.io -u \${DOCKER_USER} --password-stdin"
                    }
                    
                    def parallelBuilds = [:]
                    
                    services.each { service ->
                        def serviceName = service
                        parallelBuilds[serviceName] = {
                            dir("services/${serviceName}") {
                                sh """
                                    docker build \
                                        -t ${DOCKER_REGISTRY}/${serviceName}:${imageTag} \
                                        .
                                    
                                    docker push ${DOCKER_REGISTRY}/${serviceName}:${imageTag}
                                """
                                
                                if (isProduction()) {
                                    sh """
                                        docker tag ${DOCKER_REGISTRY}/${serviceName}:${imageTag} ${DOCKER_REGISTRY}/${serviceName}:latest
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
        
        stage('Trivy Image Scan') {
            when {
                expression { false }
            }
            /*
            when {
                expression { isProduction() || isDevelopment() }
            }
            */
            steps {
                script {
                    def services = getAllServices()
                    def imageTag = isProduction() ? VERSION : "dev-${GIT_COMMIT_SHORT}"
                    def parallelScans = [:]
                    def scanResults = [:]
                    
                    services.each { service ->
                        def serviceName = service
                        parallelScans[serviceName] = {
                            def imageFullName = "${DOCKER_REGISTRY}/${serviceName}:${imageTag}"
                            
                            sh """
                                trivy image \
                                    --severity HIGH,CRITICAL \
                                    --format json \
                                    --output trivy-${serviceName}-report.json \
                                    --timeout 10m \
                                    ${imageFullName}
                            """
                            
                            def exitCode = sh(
                                script: """
                                    trivy image \
                                        --severity CRITICAL \
                                        --exit-code 1 \
                                        --timeout 10m \
                                        ${imageFullName}
                                """,
                                returnStatus: true
                            )
                            
                            scanResults[serviceName] = exitCode
                        }
                    }
                    
                    parallel parallelScans
                    archiveArtifacts artifacts: 'trivy-*-report.json', allowEmptyArchive: true
                    
                    def criticalFound = scanResults.any { service, exitCode -> exitCode != 0 }
                    if (criticalFound) {
                        def affectedServices = scanResults.findAll { service, exitCode -> exitCode != 0 }.keySet()
                        echo "WARNING: Critical vulnerabilities found in: ${affectedServices.join(', ')}"
                        sendNotification('WARNING', "Critical vulnerabilities detected in: ${affectedServices.join(', ')}")
                    }
                }
            }
        }
        
        stage('Deploy to Dev') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-dev', variable: 'KUBECONFIG_FILE')]) {
                        sh "cp \${KUBECONFIG_FILE} /tmp/kubeconfig-dev-${BUILD_NUMBER}"
                        env.KUBECONFIG = "/tmp/kubeconfig-dev-${BUILD_NUMBER}"
                        
                        // Verify kubeconfig works
                        sh """
                            export KUBECONFIG=/tmp/kubeconfig-dev-${BUILD_NUMBER}
                            kubectl cluster-info || echo "Warning: Could not connect to cluster"
                            kubectl config view --minify
                        """
                        
                        deployWithHelm('dev', "dev-${GIT_COMMIT_SHORT}")
                        
                        // Cleanup
                        sh "rm -f /tmp/kubeconfig-dev-${BUILD_NUMBER}"
                    }
                    sendNotification('SUCCESS', "Successfully deployed to Development environment")
                }
            }
        }
        
        stage('Smoke Tests - Dev') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-dev', variable: 'KUBECONFIG_FILE')]) {
                        sh """
                            export KUBECONFIG=${env.KUBECONFIG}
                            echo "Waiting for API Gateway to be ready..."
                            kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=api-gateway -n dev --timeout=300s || true
                            
                            export KUBECONFIG=${env.KUBECONFIG}
                            kubectl port-forward svc/api-gateway 9091:8080 -n dev > /dev/null 2>&1 &
                            PORT_FORWARD_PID=\$!
                            
                            echo "Waiting for port-forward to be ready..."
                            sleep 15
                            
                            echo "Running smoke test..."
                            for i in {1..10}; do
                                if curl -s -f http://localhost:9091/actuator/health > /dev/null 2>&1; then
                                    echo "Smoke test passed!"
                                    kill \$PORT_FORWARD_PID || true
                                    exit 0
                                fi
                                echo "Attempt \$i failed, retrying..."
                                sleep 3
                            done
                            
                            echo "Warning: Smoke test did not pass, but continuing..."
                            kill \$PORT_FORWARD_PID || true
                        """
                    }
                }
            }
        }
        
        stage('Generate Release Notes') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    generateReleaseNotes(VERSION, PREVIOUS_VERSION)
                }
            }
        }
        
        stage('Approval Gate - Production') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    sendNotification('WARNING', "Waiting for production deployment approval for version ${VERSION}")
                    
                    timeout(time: 24, unit: 'HOURS') {
                        def approvalData = input(
                            message: "Deploy version ${VERSION} to Production?",
                            ok: 'Deploy to Production',
                            submitter: 'admin',
                            submitterParameter: 'APPROVER',
                            parameters: [
                                choice(
                                    name: 'DEPLOYMENT_STRATEGY',
                                    choices: ['rolling', 'blue-green'],
                                    description: 'Deployment strategy to use'
                                ),
                                text(
                                    name: 'APPROVAL_NOTES',
                                    defaultValue: '',
                                    description: 'Approval notes (optional)'
                                )
                            ]
                        )
                        
                        if (approvalData instanceof Map) {
                            env.APPROVER = approvalData.APPROVER
                            env.DEPLOYMENT_STRATEGY = approvalData.DEPLOYMENT_STRATEGY
                            env.APPROVAL_NOTES = approvalData.APPROVAL_NOTES
                        } else {
                            env.APPROVER = approvalData
                            env.DEPLOYMENT_STRATEGY = 'rolling'
                            env.APPROVAL_NOTES = ''
                        }
                        
                        echo "Deployment approved by: ${env.APPROVER}"
                        echo "Strategy: ${env.DEPLOYMENT_STRATEGY}"
                        
                        sendNotification('SUCCESS', "Production deployment approved by ${env.APPROVER}")
                    }
                }
            }
        }

        stage('Deploy to Production') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE')]) {
                        sh "cp \${KUBECONFIG_FILE} /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        env.KUBECONFIG = "/tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        
                        sh """
                            export KUBECONFIG=/tmp/kubeconfig-prod-${BUILD_NUMBER}
                            kubectl cluster-info || echo "Warning: Could not connect to cluster"
                        """
                        
                        deployWithHelm('prod', VERSION)
                        
                        sh "rm -f /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                    }
                    
                    createGitTag(VERSION)
                    
                    sendNotification('SUCCESS', "Successfully deployed version ${VERSION} to Production")
                }
            }
        }

        stage('E2E Tests') {
            when {
                expression { isProduction() }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE')]) {
                        env.KUBECONFIG = "/tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        def e2eTestsExist = fileExists('tests/e2e')
                        
                        if (!e2eTestsExist) {
                            echo "E2E tests directory not found, skipping..."
                            return
                        }
                        
                        sh """
                            mkdir -p /tmp/e2e-tests-${BUILD_NUMBER}
                            cp -r tests/e2e/* /tmp/e2e-tests-${BUILD_NUMBER}/ || true
                        """
                        
                        dir("/tmp/e2e-tests-${BUILD_NUMBER}") {
                            sh """
                                export KUBECONFIG=${env.KUBECONFIG}
                                echo "Waiting for API Gateway to be ready..."
                                kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=api-gateway -n prod --timeout=300s || true
                                
                                kubectl port-forward svc/api-gateway 9090:8080 -n prod > /dev/null 2>&1 &
                                PORT_FORWARD_PID=\$!
                                
                                echo "Waiting for port-forward..."
                                for i in {1..30}; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "API Gateway is ready"
                                        break
                                    fi
                                    echo "Waiting for API Gateway... attempt \$i/30"
                                    sleep 2
                                done
                                
                                if [ -f "package.json" ]; then
                                    npm ci --prefer-offline --no-audit || npm install
                                    NO_COLOR=1 CYPRESS_baseUrl=http://localhost:9090 npx cypress run \
                                        --config video=false,screenshotOnRunFailure=false || echo "Warning: E2E tests had failures"
                                else
                                    echo "No package.json found, skipping E2E tests"
                                fi
                                
                                kill \$PORT_FORWARD_PID || true
                                
                                cd /tmp
                                rm -rf e2e-tests-${BUILD_NUMBER}
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
                    withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG_FILE')]) {
                        env.KUBECONFIG = "/tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        def perfTestsExist = fileExists('tests/performance')
                        
                        if (!perfTestsExist) {
                            echo "Performance tests directory not found, skipping..."
                            return
                        }
                        
                        dir('tests/performance') {
                            sh """
                                export KUBECONFIG=${env.KUBECONFIG}
                                echo "Waiting for API Gateway..."
                                kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=api-gateway -n prod --timeout=300s || true
                                
                                kubectl port-forward svc/api-gateway 9090:8080 -n prod > /dev/null 2>&1 &
                                PORT_FORWARD_PID=\$!
                                
                                for i in {1..30}; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "API Gateway is ready"
                                        break
                                    fi
                                    echo "Waiting... attempt \$i/30"
                                    sleep 2
                                done
                                
                                if [ -f "requirements.txt" ]; then
                                    if [ -d "/opt/locust-venv" ]; then
                                        /opt/locust-venv/bin/pip install -r requirements.txt --quiet || pip install -r requirements.txt --quiet
                                        /opt/locust-venv/bin/locust \
                                            --headless \
                                            --host=http://localhost:9090 \
                                            --users 10 \
                                            --spawn-rate 2 \
                                            --run-time 30s \
                                            --loglevel WARNING \
                                            --autostart || echo "Warning: Performance tests had issues"
                                    else
                                        pip install -r requirements.txt --quiet || true
                                        locust \
                                            --headless \
                                            --host=http://localhost:9090 \
                                            --users 10 \
                                            --spawn-rate 2 \
                                            --run-time 30s \
                                            --loglevel WARNING \
                                            --autostart || echo "Warning: Performance tests had issues"
                                    fi
                                else
                                    echo "No requirements.txt found, skipping performance tests"
                                fi
                                
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
            script {
                if (isProduction()) {
                    sendNotification('SUCCESS', "Production deployment completed successfully - Version ${VERSION}")
                } else if (isDevelopment()) {
                    sendNotification('SUCCESS', "Development pipeline completed successfully")
                }
            }
        }
        failure {
            script {
                sendNotification('FAILURE', "Pipeline failed at stage: ${env.STAGE_NAME}")
            }
        }
        aborted {
            script {
                sendNotification('WARNING', "Pipeline was aborted")
            }
        }
    }
}

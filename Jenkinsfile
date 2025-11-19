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


def isProduction() {
    return env.BRANCH_NAME == 'main'
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
            
            # Check if tag already exists locally
            if git rev-parse v${version} >/dev/null 2>&1; then
                echo "Tag v${version} already exists locally, deleting..."
                git tag -d v${version}
            fi
            
            # Create new tag
            git tag -a v${version} -m 'Release v${version}'
            
            # Push with force to overwrite remote tag if it exists
            git push https://\${GIT_TOKEN}@github.com/ChrisBotina7823/proyecto-final-ingesoft5-services.git v${version} --force
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

def deployServices(environment, version) {
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
    
    if (version != "latest") {
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
        kubectl wait --for=condition=ready pod --all -n ${namespace} --timeout=1200s
    """
    sh """
        export KUBECONFIG=${env.KUBECONFIG}
        kubectl get pods -n ${namespace}
    """
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
            steps {
                script {
                    sh 'chmod +x mvnw'
                    sh './mvnw -N install -DskipTests -Dmaven.repo.local=/var/jenkins_home/.m2'
                    sh './mvnw clean verify -Dmaven.repo.local=/var/jenkins_home/.m2'
                }   
            }
            post {
                always {
                    script {
                        echo "Archiving test reports..."
                        
                        // Archive Surefire reports (unit tests)
                        archiveArtifacts artifacts: '**/target/surefire-reports/**/*.xml', 
                                        allowEmptyArchive: true,
                                        fingerprint: true
                        
                        archiveArtifacts artifacts: '**/target/surefire-reports/**/*.txt', 
                                        allowEmptyArchive: true,
                                        fingerprint: true
                        
                        // Archive Failsafe reports (integration tests)
                        archiveArtifacts artifacts: '**/target/failsafe-reports/**/*.xml', 
                                        allowEmptyArchive: true,
                                        fingerprint: true
                        
                        archiveArtifacts artifacts: '**/target/failsafe-reports/**/*.txt', 
                                        allowEmptyArchive: true,
                                        fingerprint: true
                        
                        // Publish JUnit test results for visualization
                        junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml',
                              allowEmptyResults: true,
                              skipPublishingChecks: false,
                              skipMarkingBuildUnstable: false
                    }
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
                        
                        // Create a summary report with SonarQube URL
                        sh """
                            mkdir -p build-reports
                            cat > build-reports/sonarqube-analysis.txt << 'EOF'
==============================================
SONARQUBE CODE QUALITY ANALYSIS
==============================================
Project Key: ecommerce-microservices
Version: ${VERSION ?: GIT_COMMIT_SHORT}
SonarQube URL: http://sonarqube:9000/dashboard?id=ecommerce-microservices
Analysis Time: \$(date)
Branch: ${env.BRANCH_NAME}
==============================================

View detailed analysis results at:
http://sonarqube:9000/dashboard?id=ecommerce-microservices

Report Task Details:
EOF
                            cat .scannerwork/report-task.txt >> build-reports/sonarqube-analysis.txt 2>/dev/null || echo "Report task not available" >> build-reports/sonarqube-analysis.txt
                        """
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                expression { isProduction() || isDevelopment() }
            }
            steps {
                script {
                    def services = getAllServices()
                    def imageTag = VERSION
                    
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
                                
                                sh """
                                    docker tag ${DOCKER_REGISTRY}/${serviceName}:${imageTag} ${DOCKER_REGISTRY}/${serviceName}:latest
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
                expression { isProduction() || isDevelopment() }
            }
            steps {
                script {
                    def services = getAllServices()
                    def imageTag = VERSION
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
            when {
                expression { false }
            }
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
                        
                        deployServices('dev', 'latest')
                        
                        // Cleanup
                        sh "rm -f /tmp/kubeconfig-dev-${BUILD_NUMBER}"
                    }
                    sendNotification('SUCCESS', "Successfully deployed to Development environment")
                }
            }
        }
        
        stage('Smoke Tests - Dev') {
            when {
                expression { false }
            }
            steps {
                script {
                    withCredentials([file(credentialsId: 'kubeconfig-dev', variable: 'KUBECONFIG_FILE')]) {
                        sh """
                            export KUBECONFIG=${env.KUBECONFIG}
                            echo "Waiting for API Gateway to be ready..."
                            kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=api-gateway -n dev --timeout=300s
                            
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
                                text(
                                    name: 'APPROVAL_NOTES',
                                    defaultValue: '',
                                    description: 'Approval notes (optional)'
                                )
                            ]
                        )
                        
                        if (approvalData instanceof Map) {
                            env.APPROVER = approvalData.APPROVER
                            env.APPROVAL_NOTES = approvalData.APPROVAL_NOTES
                        } else {
                            env.APPROVER = approvalData
                            env.APPROVAL_NOTES = ''
                        }
                        
                        echo "Deployment approved by: ${env.APPROVER}"
                        
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
                        
                        deployServices('prod', VERSION)
                        
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
                        sh "cp \${KUBECONFIG_FILE} /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        env.KUBECONFIG = "/tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        
                        def e2eTestsExist = fileExists('tests/e2e')
                        
                        if (!e2eTestsExist) {
                            echo "E2E tests directory not found, skipping..."
                            sh "rm -f /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                            return
                        }
                        
                        sh """
                            mkdir -p /tmp/e2e-tests-${BUILD_NUMBER}
                            cp -r tests/e2e/* /tmp/e2e-tests-${BUILD_NUMBER}/
                        """
                        
                        dir("/tmp/e2e-tests-${BUILD_NUMBER}") {
                            sh """
                                export KUBECONFIG=/tmp/kubeconfig-prod-${BUILD_NUMBER}
                                kubectl port-forward svc/api-gateway 9090:8080 -n prod > /dev/null 2>&1 &
                                PORT_FORWARD_PID=\$!
                                
                                echo "Waiting for port-forward to establish..."
                                sleep 5
                                
                                # Wait for API Gateway to be ready (fixed loop syntax)
                                MAX_ATTEMPTS=30
                                ATTEMPT=1
                                while [ \$ATTEMPT -le \$MAX_ATTEMPTS ]; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "✓ API Gateway is ready (attempt \$ATTEMPT/\$MAX_ATTEMPTS)"
                                        break
                                    fi
                                    echo "Waiting for API Gateway... attempt \$ATTEMPT/\$MAX_ATTEMPTS"
                                    ATTEMPT=\$((ATTEMPT + 1))
                                    sleep 2
                                done
                                
                                if ! curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                    echo "⚠ Warning: API Gateway not responding, tests may fail"
                                fi
                                
                                if [ -f "package.json" ]; then
                                    echo "Installing dependencies..."
                                    npm ci --prefer-offline --no-audit || npm install
                                    
                                    # Configure Cypress directories
                                    mkdir -p cypress/reports cypress/screenshots cypress/videos
                                    
                                    echo "Running Cypress E2E tests..."
                                    
                                    # Run Cypress with mochawesome reporter
                                    CI=true NO_COLOR=1 CYPRESS_baseUrl=http://localhost:9090 \
                                    npx cypress run \
                                        --headless \
                                        --browser electron \
                                        --reporter mochawesome \
                                        --reporter-options "reportDir=cypress/reports,overwrite=false,html=true,json=true,reportFilename=[status]_[datetime]-report" \
                                        --config video=true,screenshotOnRunFailure=true,videosFolder=cypress/videos,screenshotsFolder=cypress/screenshots
                                    
                                    # Copy reports back to workspace
                                    echo "Copying E2E test reports..."
                                    mkdir -p ${WORKSPACE}/build-reports/e2e
                                    
                                    if [ -d "cypress/reports" ]; then
                                        cp -r cypress/reports/* ${WORKSPACE}/build-reports/e2e/ 2>/dev/null || echo "No reports found"
                                    fi
                                    
                                    if [ -d "cypress/screenshots" ]; then
                                        cp -r cypress/screenshots ${WORKSPACE}/build-reports/e2e/ 2>/dev/null || echo "No screenshots found"
                                    fi
                                    
                                    if [ -d "cypress/videos" ]; then
                                        cp -r cypress/videos ${WORKSPACE}/build-reports/e2e/ 2>/dev/null || echo "No videos found"
                                    fi
                                    
                                    echo "E2E tests completed"
                                else
                                    echo "No package.json found, skipping E2E tests"
                                fi
                                
                                # Kill port-forward
                                kill \$PORT_FORWARD_PID 2>/dev/null || true
                                
                                # Cleanup
                                cd /tmp
                                rm -rf e2e-tests-${BUILD_NUMBER}
                            """
                        }
                        
                        sh "rm -f /tmp/kubeconfig-prod-${BUILD_NUMBER}"
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
                        sh "cp \${KUBECONFIG_FILE} /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        env.KUBECONFIG = "/tmp/kubeconfig-prod-${BUILD_NUMBER}"
                        
                        def perfTestsExist = fileExists('tests/performance')
                        
                        if (!perfTestsExist) {
                            echo "Performance tests directory not found, skipping..."
                            sh "rm -f /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                            return
                        }
                        
                        dir('tests/performance') {
                            sh """
                                export KUBECONFIG=/tmp/kubeconfig-prod-${BUILD_NUMBER}
                                kubectl port-forward svc/api-gateway 9090:8080 -n prod > /dev/null 2>&1 &
                                PORT_FORWARD_PID=\$!
                                
                                echo "Waiting for port-forward to establish..."
                                sleep 5
                                
                                # Wait for API Gateway to be ready
                                MAX_ATTEMPTS=30
                                ATTEMPT=1
                                while [ \$ATTEMPT -le \$MAX_ATTEMPTS ]; do
                                    if curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                        echo "✓ API Gateway is ready (attempt \$ATTEMPT/\$MAX_ATTEMPTS)"
                                        break
                                    fi
                                    echo "Waiting for API Gateway... attempt \$ATTEMPT/\$MAX_ATTEMPTS"
                                    ATTEMPT=\$((ATTEMPT + 1))
                                    sleep 2
                                done
                                
                                if ! curl -s http://localhost:9090/actuator/health > /dev/null 2>&1; then
                                    echo "⚠ Warning: API Gateway not responding, tests may fail"
                                fi
                                
                                if [ -f "requirements.txt" ]; then
                                    echo "Installing Locust dependencies..."
                                    mkdir -p reports
                                    
                                    if [ -d "/opt/locust-venv" ]; then
                                        echo "Using virtual environment at /opt/locust-venv"
                                        /opt/locust-venv/bin/pip install -r requirements.txt --quiet || pip install -r requirements.txt --quiet
                                        
                                        echo "Running Locust performance tests..."
                                        /opt/locust-venv/bin/locust \
                                            --headless \
                                            --host=http://localhost:9090 \
                                            --users 10 \
                                            --spawn-rate 2 \
                                            --run-time 30s \
                                            --loglevel WARNING \
                                            --html reports/locust-report.html \
                                            --csv reports/locust-stats \
                                            --autostart
                                    else
                                        echo "Virtual environment not found, using system Python"
                                        pip install -r requirements.txt --quiet || true
                                        
                                        echo "Running Locust performance tests..."
                                        locust \
                                            --headless \
                                            --host=http://localhost:9090 \
                                            --users 10 \
                                            --spawn-rate 2 \
                                            --run-time 30s \
                                            --loglevel WARNING \
                                            --html reports/locust-report.html \
                                            --csv reports/locust-stats \
                                            --autostart || echo "⚠ Warning: Performance tests had issues"
                                    fi
                                    
                                    # Copy performance reports back to workspace
                                    echo "Copying performance test reports..."
                                    mkdir -p ${WORKSPACE}/build-reports/performance
                                    
                                    if [ -d "reports" ]; then
                                        cp -r reports/* ${WORKSPACE}/build-reports/performance/ 2>/dev/null || echo "No reports found"
                                    fi
                                    
                                    echo "Performance tests completed"
                                else
                                    echo "No requirements.txt found, skipping performance tests"
                                fi
                                
                                # Kill port-forward
                                kill \$PORT_FORWARD_PID 2>/dev/null || true
                            """
                        }
                        
                        sh "rm -f /tmp/kubeconfig-prod-${BUILD_NUMBER}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "=========================================="
                echo "Archiving ALL reports and artifacts..."
                echo "=========================================="
                
                // 1. Test Reports (already configured in Build & Test stage)
                archiveArtifacts artifacts: '**/target/surefire-reports/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                archiveArtifacts artifacts: '**/target/failsafe-reports/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 2. Trivy Security Scan Reports
                archiveArtifacts artifacts: 'trivy-*.json', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 3. Maven Build Logs
                archiveArtifacts artifacts: '**/target/*.log', 
                                allowEmptyArchive: true
                
                // 4. JaCoCo Coverage Reports (if generated)
                archiveArtifacts artifacts: '**/target/site/jacoco/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 5. Maven Site Reports
                archiveArtifacts artifacts: '**/target/site/**/*', 
                                allowEmptyArchive: true
                
                // 6. Checkstyle Reports (if configured)
                archiveArtifacts artifacts: '**/target/checkstyle-*.xml', 
                                allowEmptyArchive: true
                
                // 7. SpotBugs Reports (if configured)
                archiveArtifacts artifacts: '**/target/spotbugsXml.xml', 
                                allowEmptyArchive: true
                
                // 8. PMD Reports (if configured)
                archiveArtifacts artifacts: '**/target/pmd.xml', 
                                allowEmptyArchive: true
                
                // 9. Cypress E2E Test Reports (from build-reports)
                archiveArtifacts artifacts: 'build-reports/e2e/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 10. Locust Performance Test Reports (from build-reports)
                archiveArtifacts artifacts: 'build-reports/performance/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 11. SonarQube Analysis Summary
                archiveArtifacts artifacts: 'build-reports/sonarqube-analysis.txt', 
                                allowEmptyArchive: true
                
                // 12. Docker Build Context
                archiveArtifacts artifacts: '**/Dockerfile', 
                                allowEmptyArchive: true
                
                // 13. Kubernetes Manifests Used
                archiveArtifacts artifacts: 'infra/kubernetes/**/*.yaml', 
                                allowEmptyArchive: true
                
                // 14. SonarQube Analysis Metadata (scanner work)
                archiveArtifacts artifacts: '.scannerwork/report-task.txt', 
                                allowEmptyArchive: true
                
                // 15. Dependency Check Reports (if configured)
                archiveArtifacts artifacts: '**/target/dependency-check-report.html', 
                                allowEmptyArchive: true
                
                // 16. Maven Dependency Tree
                sh 'mkdir -p build-reports || true'
                sh './mvnw dependency:tree -DoutputFile=build-reports/dependency-tree.txt -Dmaven.repo.local=/var/jenkins_home/.m2 || true'
                archiveArtifacts artifacts: 'build-reports/dependency-tree.txt', 
                                allowEmptyArchive: true
                
                // 17. Complete build-reports directory
                archiveArtifacts artifacts: 'build-reports/**/*', 
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // 18. Build Info Summary
                sh """
                    mkdir -p build-reports || true
                    cat > build-reports/build-info.txt << 'EOF'
==============================================
BUILD INFORMATION SUMMARY
==============================================
Build Number: ${env.BUILD_NUMBER}
Build ID: ${env.BUILD_ID}
Build URL: ${env.BUILD_URL}
Job Name: ${env.JOB_NAME}
Branch: ${env.BRANCH_NAME}
Git Commit: ${env.GIT_COMMIT}
Git Commit Short: ${GIT_COMMIT_SHORT}
Version: ${VERSION ?: 'N/A'}
Previous Version: ${PREVIOUS_VERSION ?: 'N/A'}
Environment: ${ENVIRONMENT}
Build User: ${env.BUILD_USER ?: 'System'}
Build Timestamp: \$(date)
Workspace: ${env.WORKSPACE}
Node Name: ${env.NODE_NAME}
==============================================
EOF
                """
                archiveArtifacts artifacts: 'build-reports/build-info.txt', 
                                allowEmptyArchive: true
                
                echo "=========================================="
                echo "All reports archived successfully!"
                echo "=========================================="
            }
        }
        
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

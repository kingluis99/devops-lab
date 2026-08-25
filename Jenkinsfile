// Week 3 lab — declarative Jenkins pipeline.
//
// Jenkins prerequisites (see docs/jenkins-setup.md):
//   - Plugins: Pipeline, Git, Docker Pipeline, JUnit, Warnings NG (optional)
//   - Credentials: 'dockerhub-creds' (username/password), 'kubeconfig-file' (secret file)
//   - Tools:  JDK named 'jdk21', Maven named 'maven3'   (Manage Jenkins > Tools)

pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    environment {
        IMAGE_NAME    = 'mikelam/task-api'
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        REGISTRY_CRED = 'dockerhub-creds'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Build Image') {
            steps {
                script {
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}",
                                 "--build-arg BUILD_VERSION=${IMAGE_TAG} .")
                }
            }
        }

        stage('Push Image') {
            when { branch 'main' }
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', REGISTRY_CRED) {
                        docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push()
                        docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push('latest')
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when { branch 'main' }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {
                    sh """
                        kubectl -n taskapi set image deployment/task-api \
                                task-api=${IMAGE_NAME}:${IMAGE_TAG}
                        kubectl -n taskapi rollout status deployment/task-api --timeout=180s
                    """
                }
            }
        }
    }

    post {
        success { echo "Build ${IMAGE_TAG} deployed." }
        failure { echo "Build ${IMAGE_TAG} failed — check the stage log above." }
        always  { sh 'docker image prune -f || true' }
    }
}

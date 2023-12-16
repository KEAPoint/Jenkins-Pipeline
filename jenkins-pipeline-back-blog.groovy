pipeline {
   agent any
    stages {
        stage('Git Clone') {
            steps {
                script {
                    try {
                        git url: "https://github.com/KEAPoint/OnLog_Post_Server", branch: "main", credentialsId: 'namsh1125'
                        env.cloneResult=true
                    } catch (error) {
                        print(error)
                        env.cloneResult=false
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
            post {
                success {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "=========================================\nPipeline Start: Job ${env.JOB_NAME} (${env.BUILD_NUMBER})"
                    )
                }
            }
        }
        stage('secret.yml download') {
            steps {
                sh 'chmod -R 777 ./src/main/resources/' // 쓰기 권한 부여
                withCredentials([file(credentialsId: 'blogSecretYml', variable: 'secretYml')]) {
                    script {
                        sh 'cp $secretYml ./src/main/resources/secret.yml'
                    }
                }
            }
        }
        stage('Build Jar') {
            steps {
                script {
                    sh './gradlew bootJar'
                }
            }
            post {
                success {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "Success: Build Jar"
                    )
                }
                failure {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#FF3232', 
                        message: "Fail: Build Jar"
                    )
                }
            }
        }
        stage('ECR Upload') {
            steps{
                script{
                    try {          
                        sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 629515838455.dkr.ecr.us-east-1.amazonaws.com'
                        sh "docker build --build-arg ENVIRONMENT=prod -t kea-004-onlog-blog-server:${env.BUILD_NUMBER} ."
                        sh "docker tag kea-004-onlog-blog-server:${env.BUILD_NUMBER} 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-blog-server:${env.BUILD_NUMBER}"
                        sh "docker tag kea-004-onlog-blog-server:${env.BUILD_NUMBER} 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-blog-server:latest"
                        sh "docker push 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-blog-server:${env.BUILD_NUMBER}"
                        sh "docker push 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-blog-server:latest"  // latest와 버전을 같이 업로드
                    } catch (error) {
                        print(error)
                        echo 'Remove Deploy Files'
                        sh "rm -rf /var/lib/jenkins/workspace/onlog-blog-ci-cd/*"
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
            post {
                success {
                    echo "The ECR Upload stage successfully."
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "Success: ECR Upload"
                    )
                }
                failure {
                    echo "The ECR Upload stage failed."
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#FF3232', 
                        message: "Fail: ECR Upload"
                    )
                }
            }
        }

        
        stage('Git Clone KEAPoint/OnLog-k8s') {
            steps {
                script {
                    try {
                        git url: "https://github.com/KEAPoint/OnLog-k8s.git", branch: "main", credentialsId: 'Bal1oon'
                        env.cloneResult=true
                    } catch (error) {
                        print(error)
                        env.cloneResult=false
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
            post {
                success {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "Success: Git Clone KEAPoint/OnLog-k8s"
                    )
                }
                failure {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#FF3232', 
                        message: "Fail: Git Clone KEAPoint/OnLog-k8s"
                    )
                }
            }
        }
        stage('Update Kubernetes Manifests') {
            steps {
                script {
                    // deploy-blog-server.yml 파일 내의 빌드 넘버를 바꿈
                    sh "sed -i 's|kea-004-onlog-blog-server:.*|kea-004-onlog-blog-server:${env.BUILD_NUMBER}|' /var/jenkins_home/workspace/onlog-back-blog/base/backend/deployment/deploy-blog-server.yml"
                }
            }
            post {
                success {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "Success: Update Kubernetes Manifests"
                    )
                }
                failure {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#FF3232', 
                        message: "Fail :Update Kubernetes Manifests"
                    )
                }
            }
        }
        stage('Git Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'Bal1oon', passwordVariable: 'GIT_TOKEN', usernameVariable: 'GIT_USERNAME')]) {
                        try {
                            sh 'git config --global user.name "${GIT_USERNAME}"'
                            sh 'git config --global user.email "ws32159@naver.com"'
                            sh 'cd /var/jenkins_home/workspace/onlog-back-blog/base && git add .'
                            sh 'cd /var/jenkins_home/workspace/onlog-back-blog/base && git commit -m "Update blog-server from Jenkins pipeline"'
                            sh 'git remote set-url origin https://Bal1oon:$GIT_TOKEN@github.com/KEAPoint/OnLog-k8s.git'
                            sh 'cd /var/jenkins_home/workspace/onlog-back-blog/base && git push origin main'
                        } catch (error) {
                            print(error)
                            currentBuild.result = 'FAILURE'
                        }
                    }
                }
            }
            post {
                success {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#2C953C', 
                        message: "Success: Git Push to K8S Manifest Repository"
                    )
                }
                failure {
                    slackSend (
                        channel: '#jenkins-notification', 
                        color: '#FF3232', 
                        message: "Fail: Git Push to K8S Manifest Repository"
                    )
                }
            }
        }
    }
    post {
        success {
            slackSend (
                channel: '#jenkins-notification', 
                color: '#2C953C', 
                message: "SUCCESS: Job ${env.JOB_NAME} (${env.BUILD_NUMBER})\n========================================="
            )
        }
        failure {
            slackSend (
                channel: '#jenkins-notification', 
                color: '#FF3232', 
                message: "FAIL: Job ${env.JOB_NAME} (${env.BUILD_NUMBER})\n========================================="
            )
        }
    }
}
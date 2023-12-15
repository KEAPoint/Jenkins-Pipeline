pipeline {
    agent any
    
    environment {
        PEM_KEY_PATH = credentials('keapointPemKey')
        REMOTE_WS1_HOST = '10.0.36.168'
        REMOTE_WS2_HOST = '10.0.36.22'
        PROXY_HOST = '10.0.14.190'
        PROXY_PORT = '4094'
        REGISTRY_URL = "629515838455.dkr.ecr.us-east-1.amazonaws.com"

    }
        
    stages {
        stage('Git Clone') {
            steps {
                script {
                    try {
                        git url: "https://github.com/KEAPoint/OnLog_Front", branch: "main", credentialsId: 'Bal1oon'
                        env.cloneResult=true
                        
                    } catch (error) {
                        print(error)
                        env.cloneResult=false
                        currentBuild.result = 'FAILURE'
                    }
                }
            }
        }
        
        stage('ECR Upload') {
            steps{
                script{
                    try {                       
                        sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 629515838455.dkr.ecr.us-east-1.amazonaws.com'
                        sh "docker build -t kea-004-onlog-front:${env.BUILD_NUMBER} ."
                        sh "docker tag kea-004-onlog-front:${env.BUILD_NUMBER} 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-front:${env.BUILD_NUMBER}"
                        sh "docker tag kea-004-onlog-front:${env.BUILD_NUMBER} 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-front:latest"
                        sh "docker push 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-front:${env.BUILD_NUMBER}"
                        sh "docker push 629515838455.dkr.ecr.us-east-1.amazonaws.com/kea-004-onlog-front:latest"
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
                }
                failure {
                    echo "The ECR Upload stage failed."
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
        }
        stage('Update Kubernetes Manifests') {
            steps {
                script {
                    sh "sed -i 's|kea-004-onlog-front:.*|kea-004-onlog-front:${env.BUILD_NUMBER}|' /var/jenkins_home/workspace/onlog-front-deploy/base/frontend/deployment/deploy-onlog-front.yaml"
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
                            sh 'cd /var/jenkins_home/workspace/onlog-front-deploy/base && git add .'
                            sh 'cd /var/jenkins_home/workspace/onlog-front-deploy/base && git commit -m "Update onlog-front from Jenkins pipeline"'
                            sh 'git remote set-url origin https://Bal1oon:$GIT_TOKEN@github.com/KEAPoint/OnLog-k8s.git'
                            sh 'cd /var/jenkins_home/workspace/onlog-front-deploy/base && git push origin main'
                        } catch (error) {
                            print(error)
                            currentBuild.result = 'FAILURE'
                        }
                    }
                }
            }
        }
    }
}
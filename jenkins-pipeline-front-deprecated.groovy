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
                        git url: "https://github.com/KEAPoint/OnLog_Front", branch: "main", credentialsId: 'namsh1125'
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

        stage('Update ws2') {
            steps {
                // ws2 업데이트 작업 수행
                withCredentials([sshUserPrivateKey(credentialsId: 'keapointPemKey', keyFileVariable: 'PEM_KEY_PATH', passphraseVariable: '', usernameVariable: '')]) {
                    sh """
                        ssh -i "\${PEM_KEY_PATH}" -o ProxyCommand='ssh -W %h:%p ubuntu@\${PROXY_HOST} -p \${PROXY_PORT} -i "\${PEM_KEY_PATH}"' \
                        -o "StrictHostKeyChecking=accept-new" ubuntu@\${REMOTE_WS2_HOST} -p 10524 '
                        aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${REGISTRY_URL}
                        sudo docker compose down --rmi all &&
                        docker compose up -d
                        '
                    """
                }
            }
        }

        stage('Switch traffic to ws2') {
            steps {
                script {
                    // ws2로 트래픽 전환 명령어 실행
                    sh "aws elbv2 modify-listener --listener-arn arn:aws:elasticloadbalancing:us-east-1:629515838455:listener/app/kea-004-ws-lb/21763be8a70a3337/bc555b33dced88f2 --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:us-east-1:629515838455:targetgroup/gcu-kea-keapoint-onlog-ws/bd36e2d89e2abf2b"
                }
            }
        }
        
        stage('Update ws1') {
            steps {
                // ws1 업데이트 작업 수행
                withCredentials([sshUserPrivateKey(credentialsId: 'keapointPemKey', keyFileVariable: 'PEM_KEY_PATH', passphraseVariable: '', usernameVariable: '')]) {
                    sh """
                        ssh -i "\${PEM_KEY_PATH}" -o ProxyCommand='ssh -W %h:%p ubuntu@\${PROXY_HOST} -p \${PROXY_PORT} -i "\${PEM_KEY_PATH}"' \
                        -o "StrictHostKeyChecking=accept-new" ubuntu@\${REMOTE_WS1_HOST} -p 10524 '
                        aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${REGISTRY_URL}
                        sudo docker compose down --rmi all &&
                        docker compose up -d
                        '
                    """
                }
            }
        }

        stage('Switch traffic to ws1') {
            steps {
                script {
                    // ws1으로 트래픽 전환 명령어 실행
                    sh "aws elbv2 modify-listener --listener-arn arn:aws:elasticloadbalancing:us-east-1:629515838455:listener/app/kea-004-ws-lb/21763be8a70a3337/bc555b33dced88f2 --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:us-east-1:629515838455:targetgroup/gcu-kea-keapoint-onlog-ws/bd36e2d89e2abf2b"
                }
            }
        }
    }
}
# Jenkins-Pipeline
해당 저장소는 Jenkins Pipeline의 Groovy 스크립트들을 관리하기 위한 곳입니다. 각각의 스크립트는 특정 작업을 자동화하기 위한 Jenkins Pipeline을 정의하며, 이는 프로젝트의 지속적인 통합 (CI) 및 지속적인 배포 (CD)를 위해 사용됩니다. Jenkins는 각 프로젝트가 업데이트될 때마다 Docker 이미지를 빌드하고, 이 이미지가 푸시될 때마다 이미지 버전의 변경이 발생하여 Argo CD를 통한 CI/CD가 이루어집니다.

## 🖐🏻 시작하기 전에
해당 스크립트를 사용하기 위해서는 Jenkins가 설치되어 있어야 하며, Github Webhook 설정과 Jenkins Credentials 설정이 되어있어야 합니다.

## ✅ 스크립트 사용 방법
1. Jenkins에서 새로운 Pipeline 작업을 생성합니다.
2. General 섹션에서 'GitHub project'를 선택 후, 빌드할 파일의 Github Repository URL을 입력합니다.
3. Build Trigger 섹션에서 'GitHub hook trigger for GITScm polling'를 선택합니다.
4. Pipeline 섹션에 실행할 스크립트를 작성합니다.
5. 저장하고 빌드를 실행하면 해당 스크립트로 정의된 Pipeline이 실행됩니다.

## 📝 각 스크립트 설명
### jenkins-pipeline-front.groovy
이 파이프라인은 프론트엔드 서비스에 대한 CI/CD 작업을 수행합니다. 해당 프로젝트의 깃허브 URL은 [여기](https://github.com/KEAPoint/OnLog_Front)를 클릭하세요.

> **기능**

1. 소스 코드 체크아웃: 프론트엔드 서비스의 소스 코드를 체크아웃합니다.
2. Docker 이미지 빌드 및 Amazon ECR에 업로드: Docker 이미지를 빌드하고, Amazon ECR에 업로드합니다. 빌드된 이미지는 특정 버전 뿐만 아니라 'latest' 태그도 함께 업로드됩니다.
3. Kubernetes 매니페스트 파일 업데이트 및 깃허브에 푸시: Kubernetes 배포 매니페스트 파일을 업데이트하고, 변경 사항을 깃허브에 푸시합니다. 이 과정에서 변경 사항은 자동으로 커밋되며, 빌드 번호에 따라 서비스를 배포하는 데 필요한 정보가 업데이트됩니다.
   
### jenkins-pipeline-back-blog.groovy
이 파이프라인은 백엔드 블로그 서비스에 대한 CI/CD 작업을 수행합니다. 해당 프로젝트의 깃허브 URL은 [여기](https://github.com/KEAPoint/OnLog_Post_Server)를 클릭하세요.

> **기능**

1. 소스 코드 체크아웃: 백엔드 블로그 서비스의 소스 코드를 체크아웃합니다.
2. Docker 이미지 빌드 및 Amazon ECR에 업로드: Docker 이미지를 빌드하고, Amazon ECR에 업로드합니다. 빌드된 이미지는 특정 버전 뿐만 아니라 'latest' 태그도 함께 업로드됩니다.
3. Kubernetes 매니페스트 파일 업데이트 및 깃허브에 푸시: Kubernetes 배포 매니페스트 파일을 업데이트하고, 변경 사항을 깃허브에 푸시합니다. 이 과정에서 변경 사항은 자동으로 커밋되며, 빌드 번호에 따라 서비스를 배포하는 데 필요한 정보가 업데이트됩니다.

### jenkins-pipeline-image.groovy
이 파이프라인은 이미지 추천 서비스에 대한 CI/CD 작업을 수행합니다. 해당 프로젝트의 깃허브 URL은 [여기](https://github.com/KEAPoint/OnLog_Image_Generation)를 클릭하세요.

> **기능**

1. 소스 코드 체크아웃: 이미지 추천 서비스의 소스 코드를 체크아웃합니다.
2. Docker 이미지 빌드 및 Amazon ECR에 업로드: Docker 이미지를 빌드하고, Amazon ECR에 업로드합니다. 빌드된 이미지는 특정 버전 뿐만 아니라 'latest' 태그도 함께 업로드됩니다.
3. Kubernetes 매니페스트 파일 업데이트 및 깃허브에 푸시: Kubernetes 배포 매니페스트 파일을 업데이트하고, 변경 사항을 깃허브에 푸시합니다. 이 과정에서 변경 사항은 자동으로 커밋되며, 빌드 번호에 따라 서비스를 배포하는 데 필요한 정보가 업데이트됩니다.

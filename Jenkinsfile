pipeline {
  agent any

  environment {
    IMAGE = 'dosyaas/numeric-app'
    TAG   = "${env.GIT_COMMIT}"      // можно заменить на 'latest' или BUILD_NUMBER
  }

  stages {

    stage('Build Artifact - Maven') {
      steps {
        sh 'mvn -B clean package -DskipTests=true'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Unit Tests - JUnit & Coverage') {
      steps {
        sh 'mvn test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          jacoco path: 'target/jacoco.exec'
        }
      }
    }

    stage('Docker Build and Push') {
      steps {
        withDockerRegistry([credentialsId: 'docker-hub', url: '']) {
          sh 'printenv | sort'
          sh 'docker build -t ${IMAGE}:${TAG} .'
          sh 'docker push ${IMAGE}:${TAG}'
        }
      }
    }

    stage('Kubernetes Deployment - DEV') {
      steps {
        withKubeConfig([credentialsId: 'kubeconfig']) {
          sh "sed -i 's#replace#dosyaas/numeric-:${GIT_COMMIT}#g' k8s_deployment_service.yaml"
          sh 'kubectl apply -f k8s_deployment_service.yaml'
        }
      }
    }
  }
}

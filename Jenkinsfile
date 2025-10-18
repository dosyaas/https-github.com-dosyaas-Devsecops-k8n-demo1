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
        sh 'mvn -B test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')],
                          failNoReports: true
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
          // Вариант 1: через sed (как у тебя)
          sh "sed -i 's#replace#${IMAGE}:${TAG}#g' k8s_deployment_service.yaml"
          sh 'kubectl apply -f k8s_deployment_service.yaml'
          // Вариант 2 (альтернатива, без sed):
          // sh 'kubectl set image deploy/numeric-app numeric-app=${IMAGE}:${TAG} -n default --record'
          sh 'kubectl rollout status deploy/numeric-app -n default'
        }
      }
    }

  }
}

pipeline {
  agent any

  environment {
    IMAGE = 'dosyaas/numeric-app'
    TAG   = "${GIT_COMMIT}"   // один источник истины
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
        withDockerRegistry(credentialsId: 'docker-hub', url: '') {
          sh 'docker build -t ${IMAGE}:${TAG} .'
          sh 'docker push ${IMAGE}:${TAG}'
        }
      }
    }

    stage('Kubernetes Deployment - DEV') {
      steps {
        withKubeConfig(credentialsId: 'kubeconfig') {
          // 1) Подставляем образ в манифест
          sh 'cp k8s_deployment_service.yaml k8s_deployment_service.rendered.yaml'
          sh 'sed -i "s#IMAGE_PLACEHOLDER#${IMAGE}:${TAG}#g" k8s_deployment_service.rendered.yaml'

          // 2) Применяем и ждём раскатку
          sh '''
            kubectl apply -f k8s_deployment_service.rendered.yaml
            APP_DEPLOY=$(kubectl get deploy -o name | grep -m1 numeric)
            kubectl rollout status "$APP_DEPLOY" --timeout=120s
          '''
        }
      }
      post {
        failure {
          script {
            sh '''
              set -e
              APP_DEPLOY=$(kubectl get deploy -o name | grep -m1 numeric || true)
              if [ -n "$APP_DEPLOY" ]; then
                echo "Rollout failed. Trying to undo..."
                kubectl rollout undo "$APP_DEPLOY" || true
              fi
            '''
          }
        }
      }
    }
  }
}

pipeline {
  agent any

  environment {
    IMAGE = 'dosyaas/numeric-app'
    TAG   = "${GIT_COMMIT}"
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

stage('Mutation Tests - PIT') {
    steps {
        sh "mvn org.pitest:pitest-maven:mutationCoverage"
    }
    post {
        always {
            pitmutation mutationStatsFile: '**/target/pit-reports/**/mutations.xml'
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
          sh 'cp k8s_deployment_service.yaml k8s_deployment_service.rendered.yaml'
          sh 'sed -i "s#IMAGE_PLACEHOLDER#${IMAGE}:${TAG}#g" k8s_deployment_service.rendered.yaml'

          // применяем, ждём раскатку и только тут работаем с kubectl
          sh '''
            set -e
            kubectl apply -f k8s_deployment_service.rendered.yaml
            # подберите -n <namespace> если не default
            APP_DEPLOY=$(kubectl get deploy -l app=devsecops -o name | head -n1)
            echo "APP_DEPLOY=${APP_DEPLOY}"
            kubectl rollout status "$APP_DEPLOY" --timeout=180s
          '''
        }
      }
      post {
        failure {
          // rollback тоже ДОЛЖЕН быть под kubeconfig
          withKubeConfig(credentialsId: 'kubeconfig') {
            sh '''
              set -e
              APP_DEPLOY=$(kubectl get deploy -l app=devsecops -o name | head -n1 || true)
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

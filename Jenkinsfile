pipeline {
  agent any

  tools {
    jdk 'JDK11'
    // maven 'Maven3' // добавь, если настроишь Maven в Tools
  }

  options {
    timestamps()
  }

  environment {
    IMAGE = 'dosyaas/numeric-app'
    TAG   = "${GIT_COMMIT}"
  }

  stages {

    stage('Build Artifact - Maven') {
      steps {
        // ВАЖНО: пропускаем КОМПИЛЯЦИЮ и запуск тестов на этапе сборки артефакта
        sh 'mvn -B -U clean package -Dmaven.test.skip=true'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Unit Tests - JUnit & Coverage') {
      steps {
        sh 'mvn -B -U test'
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
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          retry(2) {
            sh 'mvn -e -X -Dverbose=true org.pitest:pitest-maven:mutationCoverage'
          }
        }
      }
      post {
        always {
          pitmutation mutationStatsFile: '**/target/pit-reports/**/mutations.xml'
          archiveArtifacts artifacts: 'target/pit-reports/**', allowEmptyArchive: true
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
          sh '''
            set -e
            kubectl apply -f k8s_deployment_service.rendered.yaml
            APP_DEPLOY=$(kubectl get deploy -l app=devsecops -o name | head -n1)
            kubectl rollout status "$APP_DEPLOY" --timeout=180s
          '''
        }
      }
      post {
        failure {
          withKubeConfig(credentialsId: 'kubeconfig') {
            sh '''
              set -e
              APP_DEPLOY=$(kubectl get deploy -l app=devsecops -o name | head -n1 || true)
              [ -n "$APP_DEPLOY" ] && kubectl rollout undo "$APP_DEPLOY" || true
            '''
          }
        }
      }
    }
  }
}

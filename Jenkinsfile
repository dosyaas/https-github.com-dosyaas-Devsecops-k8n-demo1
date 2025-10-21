pipeline {
  agent any

  // Выбираем инструменты агента (важно для PIT: JDK 11)
  tools {
    jdk   'JDK11'   // укажи имя из Manage Jenkins → Tools
    maven 'Maven3'  // аналогично
  }

  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    IMAGE = 'dosyaas/numeric-app'
    TAG   = "${GIT_COMMIT}"
  }

  stages {

    stage('Build Artifact - Maven') {
      steps {
        sh 'mvn -B -U clean package -DskipTests=true'
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
          jacoco path: 'target/jacoco.exec', inclusionPattern: '**/*.exec'
        }
      }
    }

    stage('Mutation Tests - PIT') {
      steps {
        // чтобы пайплайн не падал насмерть во время доводки тестов:
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          retry(2) {
            sh 'mvn -B -U -Dpit.verbose=true org.pitest:pitest-maven:mutationCoverage'
          }
        }
      }
      post {
        always {
          // публикуем XML в “Pit Mutation Report” плагин (если установлен)
          pitmutation mutationStatsFile: '**/target/pit-reports/**/mutations.xml'
          // архивируем HTML-отчеты PIT
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
            echo "APP_DEPLOY=${APP_DEPLOY}"
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

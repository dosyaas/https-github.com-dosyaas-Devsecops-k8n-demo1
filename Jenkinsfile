pipeline {
  agent any

  // если в Tools есть JDK 11 с именем JDK11 – оставь этот блок
  tools {
    jdk 'JDK11'
    // maven 'Maven3'  // убрано, т.к. не настроен
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
          // правильные параметры jacoco
          jacoco execPattern: 'target/jacoco.exec',
                 classPattern: 'target/classes',
                 sourcePattern: 'src/main/java'
        }
      }
    }

    stage('Mutation Tests - PIT') {
      steps {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          retry(2) {
            sh 'mvn -B -U -Dpit.verbose=true org.pitest:pitest-maven:mutationCoverage'
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

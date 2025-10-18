pipeline {
  agent any

  stages {

    stage('Build Artifact - Maven') {
      steps {
        sh 'mvn clean package -DskipTests=true'
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
      // при желании пороги качества:
      // publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')],
      //   globalThresholds: [[thresholdTarget: 'Line', unstableThreshold: '60', unhealthyThreshold: '50']]

        }
      }
    }

    stage('Docker Build and Push') {
      steps {
        withDockerRegistry([credentialsId: 'docker-hub', url: '']) {
          sh 'printenv'
          sh 'docker build -t dosyaas/numeric-app:"$GIT_COMMIT" .'
          sh 'docker push dosyaas/numeric-app:"$GIT_COMMIT"'
        }
      }
stage('Kubernetes Deployment - DEV') {
    steps {
        withKubeConfig([credentialsId: 'kubeconfig']) {
            sh "sed -i 's#replace#dosyaas/numeric-app:${GIT_COMMIT}#g' k8s_deployment_service.yaml"
            sh "kubectl apply -f k8s_deployment_service.yaml"
        }    
}

  } // end stages
}   // end pipeline

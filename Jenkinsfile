pipeline {
  agent any

  stages {

    stage('Build Artifact - Maven') {
      steps {
        sh 'mvn clean package -DskipTests=true'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Unit Tests - JUnit and Jacoco') {
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
          sh 'printenv'
          sh 'docker build -t dosyaas/numeric-app:"$GIT_COMMIT" .'
          sh 'docker push dosyaas/numeric-app:"$GIT_COMMIT"'
        }
      }
    }

  } // end stages
}   // end pipeline

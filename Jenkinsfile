pipeline {
  agent any

  stages {
    stage('Build Artifact') {
      steps {
        sh 'mvn -B clean package -DskipTests=true'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Unit Tests - JUnit and Jacoco') {
      steps {
        sh 'mvn -B test'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          jacoco execPattern: 'target/jacoco.exec'
        }
      }
    }
  }
}

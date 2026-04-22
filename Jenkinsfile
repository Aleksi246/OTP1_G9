pipeline {
    agent any

    tools {
        maven "Maven"  // Make sure Maven is configured in Jenkins Global Tool Configuration
        jdk "JDK21"    // Make sure JDK is configured
    }

    environment {
        PATH = "C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-creds'
        DOCKERHUB_REPO = 'aleksi246/otp'
        DOCKER_IMAGE_TAG = 'latest'
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                git branch: 'main', url: 'https://github.com/Aleksi246/OTP1_G9.git'
            }
        }
        stage('Inject secrets') {
            steps {
                withCredentials([
                    file(credentialsId: 'otp_env_file', variable: 'ENV_FILE'),
                    file(credentialsId: 'otp_db_pro', variable: 'DB_FILE')
                ]) {
                    bat '''
                        if not exist src\\main\\resources mkdir src\\main\\resources
                        if not exist src\\test\\resources mkdir src\\test\\resources

                        copy "%ENV_FILE%" ".env"

                        copy "%DB_FILE%" "src\\main\\resources\\db.properties"
                        copy "%DB_FILE%" "src\\test\\resources\\db.properties"
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test -T 1'
            }
        }

        stage('Code Coverage') {
            steps {
                bat 'mvn jacoco:report'
            }
        }

        stage('Publish Test Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Publish Coverage Report') {
            steps {
                jacoco()
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQubeServer') {
                bat """
                ${tool 'SonarScanner'}\\bin\\sonar-scanner ^
                """
                }
            }
        }

        stage('build docker image') {
            steps{
                script{
                    docker.build("${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}")
                }
            }
        }
        stage('push docker image'){
            steps{
                script{
                    docker.withRegistry('https://index.docker.io/v1/', DOCKERHUB_CREDENTIALS_ID){
                    docker.image("${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}").push()
                    }
                }
            }
        }
    }
}
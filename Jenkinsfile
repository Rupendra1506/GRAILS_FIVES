#!groovy
// Using shared libraries per https://www.jenkins.io/blog/2017/02/15/declarative-notifications/
@Library('directly-jenkins-shared') _

pipeline {
    agent {
        label 'multiple_executor'
    }
    parameters {
        booleanParam(
            name: 'DEPLOY_TO_STAGE',
            description: 'Deploy to STAGE after building',
            defaultValue: true)
        booleanParam(name: 'DEBUG',
                description: 'Enable for Bash command trace and variable dumps',
                defaultValue: false)
    }
    options {
        timestamps()
        disableConcurrentBuilds()
        quietPeriod(5)
        buildDiscarder(logRotator(daysToKeepStr: '120'))
    }
    environment {
        DEBUG_FLAG = """${sh(
                returnStdout: true,
                script: 'if [ "${DEBUG}" = "true" ]; then echo "-debug"; else echo ""; fi'
            ).trim()}"""
    }
    stages {

        stage('Clone repository') {
            steps {
                checkout scm
            }
        }

        stage('Build WAR File') {
            steps {
                withEnv(['JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto.x86_64/jre']) {
                    sh '''
                        ./gradlew clean war
                        cp build/libs/all-new-directly-routing-engine-0.1.war build/libs/ROOT.war
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    app_image = docker.build('directly-andre', "--build-arg build_number=${env.BUILD_NUMBER} .")
                }
            }
        }

        stage('Run tests') {
            environment {
                JAVA_HOME = '/usr/lib/jvm/java-1.8.0-amazon-corretto.x86_64/jre'

                // try to make MYSQL_PORT unique across services so that we don't have collisions if multiple services
                // are building on the same agent at the same time
                DB_PORT = 3396
                DB_NAME = 'andre_test'

            }
            steps {
                script {
                    docker.image('mysql:5.7').withRun('-e "MYSQL_ALLOW_EMPTY_PASSWORD=yes" -e "MYSQL_DATABASE=${DB_NAME}" -p ${DB_PORT}:3306') { mysql ->
                        // wait for mysql to come up
                        sh 'while ! mysqladmin -h 127.0.0.1 -u root -P${DB_PORT} ping --silent; do sleep 1; done'
                        sh 'echo "MySQL is up!"'

                        docker.image('redis:5.0.5-alpine').withRun('-p 6379:6379') { redis ->
                            // wait for redis to come up
                            sh 'while ! nc -z 127.0.0.1 6379; do sleep 1; done'
                            sh 'echo "Redis is up!"'
                            sh './gradlew -Plocal.config.location=test-dbm-config.yml clean dbmUpdate check'
                        }
                    }
                }
            }
        }

        stage('Publish Coverage Reports') {
            steps {
                junit 'build/test-results/**/*.xml'
            }
        }

        stage('Git tag') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: "github-directly-andre", keyFileVariable: 'keyfile')]) {
                    sh("git tag -a ${env.BUILD_NUMBER} -m 'Jenkins Pipleline'")
                    sh '''
                        export GIT_SSH_COMMAND="ssh -i \'${keyfile}\'"
                        git push git@github.com:ondemand-answers/all-new-directly-routing-engine.git --tags
                    '''
                }
            }
        }

        stage('Push Image') {
            steps {
                script {
                    docker.withRegistry('https://222336342030.dkr.ecr.us-east-1.amazonaws.com', 'ecr:us-east-1:jenkins-iam') {
                        app_image.push("${env.BUILD_NUMBER}")
                        app_image.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Stage') {
            when {
                expression {
                    params.DEPLOY_TO_STAGE == true
                }
            }
            steps {
                build job: 'Deploy Service (ECS)',
                    wait: false,
                    parameters: [
                        string(name: 'SERVICE_NAME', value: "andre-ecs"),
                        string(name: 'RELEASE_TAG', value: "${env.BUILD_NUMBER}"),
                        string(name: 'ENVIRONMENT_NAME', value: "stage")
                    ]
            }
        }
    }

    post {
        always {
            sendNotifications currentBuild.result
        }
    }

}

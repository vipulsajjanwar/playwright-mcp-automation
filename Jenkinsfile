pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'prod'], description: 'Target Execution Environment')
        choice(name: 'BROWSER', choices: ['chromium', 'firefox', 'webkit'], description: 'Target Browser')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Run Browser Headless')
        choice(name: 'SUITE', choices: ['smoke', 'regression', 'api', 'database', 'parallel'], description: 'TestNG Suite File')
        string(name: 'THREAD_COUNT', defaultValue: '4', description: 'Parallel Thread Count')
        booleanParam(name: 'TRACE_ENABLED', defaultValue: true, description: 'Enable Playwright Tracing')
        string(name: 'RETRY_COUNT', defaultValue: '1', description: 'Failed Test Retry Count')
    }

    environment {
        MAVEN_OPTS = '-Dfile.encoding=UTF-8'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Install Playwright Browsers') {
            steps {
                bat 'mvn compile'
                bat 'npx playwright install --with-deps'
            }
        }

        stage('Execute Test Suite') {
            steps {
                bat """
                    mvn clean test ^
                    -Denv=${params.ENV} ^
                    -Dbrowser=${params.BROWSER} ^
                    -Dheadless=${params.HEADLESS} ^
                    -DsuiteXmlFile=src/test/resources/testng/${params.SUITE}.xml ^
                    -Dthreads=${params.THREAD_COUNT} ^
                    -Dtrace.enabled=${params.TRACE_ENABLED} ^
                    -Dretry.count=${params.RETRY_COUNT}
                """
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/reports/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/traces/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/logs/**', allowEmptyArchive: true
        }
    }
}

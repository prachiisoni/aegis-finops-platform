pipeline {
    agent any

    environment {
        IMAGE_NAME = "aegisfinops/finops-gateway-service"
        IMAGE_TAG  = "${env.BUILD_NUMBER ?: '1.0.0'}"
        K8S_NAMESPACE = "aegis-finops"
    }

    stages {
        stage('1. Checkout & Metadata') {
            steps {
                echo "Building AegisFinOps Gateway Service - Commit: ${env.GIT_COMMIT ?: 'local'}"
            }
        }

        stage('2. Maven Compile & Unit Tests') {
            steps {
                dir('finops-gateway-service') {
                    sh 'mvn clean test'
                }
            }
        }

        stage('3. FinOps Unit-Cost Regression Gate (PM Governance)') {
            steps {
                echo "Running Synthetic Token Benchmark against Production Baseline..."
                sh 'python3 simulation-and-benchmarks/run_ci_cost_regression_gate.py'
            }
        }

        stage('4. Build & Push Container Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ./finops-gateway-service"
            }
        }

        stage('5. Kubernetes Rolling Deployment & Canary Check') {
            steps {
                sh "kubectl apply -f k8s-manifests/aegis-finops-k8s-deployment.yaml"
                sh "kubectl rollout status deployment/aegis-finops-gateway -n ${K8S_NAMESPACE} --timeout=90s"
            }
        }
    }
}
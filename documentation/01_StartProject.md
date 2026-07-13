# 01 — Start the Project

AlNaoAwwotations is a serverless, post-it style annotation system organized in
boards. This guide covers prerequisites, project layout, and deployment.

## Architecture

- **Backend**: AWS Lambda (Python 3.12)
- **Database**: Amazon DynamoDB (single-table design), PAY_PER_REQUEST
- **API**: Amazon API Gateway (HTTP API) + JWT Lambda Authorizer
- **Auth**: Custom JWT (PyJWT) + bcrypt password hashing (no Cognito)
- **Infrastructure**: Terraform (modular)
- **Frontend**: React + Vite, hosted on S3 + CloudFront
- **Secrets**: AWS SSM Parameter Store (SecureString)
- **Region**: `eu-west-1`

```
Client → CloudFront → S3 (frontend)
Client → API Gateway HTTP API → Lambda Authorizer (JWT) → Lambda → DynamoDB
```

## Prerequisites

- An AWS account and credentials configured (`aws configure`)
- Terraform >= 1.5
- Python 3.12 and `pip`
- Node.js >= 18 and npm (for the frontend)

## Repository layout

```
AlNaoAwwotations/
├── frontend/         React + Vite SPA
├── backend/          Lambda source (functions/ + shared/) and build.sh
├── infrastructure/   Terraform root + modules
└── documentation/    This documentation and openapi.yaml
```

## One-time backend state setup

The Terraform state is stored on S3 with a DynamoDB lock table. Create them once
before the first `init` (names must match `providers.tf`):

```bash
aws s3 mb s3://alnao-awwotations-tfstate --region eu-west-1
aws dynamodb create-table \
  --table-name alnao-awwotations-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region eu-west-1
```

## Deploy

1. **Build the Lambda package** (installs dependencies for the Lambda runtime):

   ```bash
   cd backend
   ./build.sh
   ```

2. **Configure variables**: edit `infrastructure/terraform.tfvars` and set a
   strong `jwt_secret` and a globally-unique `frontend_bucket_name`.

3. **Apply Terraform**:

   ```bash
   cd ../infrastructure
   terraform init
   terraform apply
   ```

4. **Note the outputs**: `api_endpoint`, `frontend_bucket_name`,
   `frontend_cloudfront_domain`.

5. **Deploy the frontend**:

   ```bash
   cd ../frontend
   echo "VITE_API_BASE_URL=$(terraform -chdir=../infrastructure output -raw api_endpoint)" > .env
   npm install
   npm run build
   aws s3 sync dist/ s3://<frontend_bucket_name> --delete
   aws cloudfront create-invalidation \
     --distribution-id <distribution_id> --paths "/*"
   ```

## Teardown

```bash
cd infrastructure
terraform destroy
```

See the other documents for details on each feature.

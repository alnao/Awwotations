# Awwotations by AlNao

A post-it style annotation system organized in boards. Users own boards; each
board holds notes with a rich lifecycle (status transitions, pin, favorite),
positioning, icons, and links.

Fully serverless on AWS: **Lambda (Python 3.12)**, **DynamoDB (single table)**,
**API Gateway (HTTP API)** with a **custom JWT Lambda Authorizer** (no Cognito),
a **React + Vite** frontend on **S3 + CloudFront**, and **Terraform** for all
infrastructure.

## Features

- Custom JWT auth (PyJWT + bcrypt), access + refresh tokens
- Boards CRUD (owner-scoped)
- Notes CRUD with:
  - `textType`: `MD`, `HTML`, `TEXT`, or extensible `CODE_XXXX` (`^CODE_[A-Z]+$`)
  - Up to 10 links, optional Font Awesome icons, board positioning
  - Centralized status lifecycle with a validated transition matrix
  - Content lock for `DONE`/`REJECTED` notes (edit returns `403`)
  - Pin (owner-only, max 1 per board, auto-unpin) and favorite (all users)
- DynamoDB single-table design with GSI1 (users/boards) and GSI2 (notes)
- Modular Terraform (dynamodb, iam, lambda, api_gateway, s3_cloudfront)
- Secrets in SSM Parameter Store; region `eu-west-1`

## Repository layout

```
AlNaoAwwotations/
├── frontend/         React + Vite SPA (S3 + CloudFront)
├── backend/          Lambda source (functions/, shared/), build.sh, requirements.txt
├── infrastructure/   Terraform root + modules
└── documentation/    openapi.yaml + 10 topic guides
```

## Quick start

Prerequisites: AWS credentials, Terraform >= 1.5, Python 3.12, Node.js >= 18.

```bash
# 1. Create the Terraform state backend (once) if not exists!
aws s3 mb s3://alnao-dev-terraform --region eu-west-1

# 2. Build the Lambda package
cd backend && ./build.sh && cd ..

# 3. Configure and deploy infrastructure
#    Edit infrastructure/terraform.tfvars: set jwt_secret and frontend_bucket_name
cd infrastructure
terraform init
terraform apply
cd ..

# 4. Deploy the frontend
cd frontend
echo "VITE_API_BASE_URL=$(terraform -chdir=../infrastructure output -raw api_endpoint)" > .env
npm install && npm run build
aws s3 sync dist/ s3://$(terraform -chdir=../infrastructure output -raw frontend_bucket_name) --delete
aws cloudfront create-invalidation \
  --distribution-id $(terraform -chdir=../infrastructure output -raw frontend_cloudfront_distribution_id) \
  --paths "/*"
```

Open the CloudFront domain from `terraform output frontend_cloudfront_domain`.

## API

See [`documentation/openapi.yaml`](documentation/openapi.yaml) for the full
contract, and the numbered guides in [`documentation/`](documentation/):

| Doc | Topic |
| --- | ----- |
| 01  | Start the project / deploy |
| 02  | Authentication (JWT) |
| 03  | Boards |
| 04  | Notes |
| 05  | Note status & transition matrix |
| 06  | Pinned notes |
| 07  | Favorite notes |
| 08  | DynamoDB single-table design |
| 09  | Terraform |
| 10  | OpenAPI |

### Endpoint summary

```
POST   /auth/register
POST   /auth/login
POST   /auth/refresh

GET    /boards
POST   /boards
PUT    /boards/{boardId}
DELETE /boards/{boardId}

GET    /boards/{boardId}/notes
POST   /boards/{boardId}/notes
PUT    /boards/{boardId}/notes/{noteId}            # 403 if DONE/REJECTED
DELETE /boards/{boardId}/notes/{noteId}
PATCH  /boards/{boardId}/notes/{noteId}/status     # validated transition
PATCH  /boards/{boardId}/notes/{noteId}/pin        # owner only
PATCH  /boards/{boardId}/notes/{noteId}/favorite   # any board user
```

## Development

Backend logic (models, status matrix, textType validation) is unit-testable
without AWS:

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

## AWS Cost Estimate

The infrastructure is **fully serverless and pay-per-use** — there are no always-on
resources (no EC2, no RDS, no ECS). For a small user base (~30 active users, low
traffic), the monthly cost is negligible.

> **Note:** The figures below assume an AWS account older than 2 years.
> The free tier (Lambda 1M requests/month, DynamoDB 25 GB, etc.) applies only to
> accounts in their first 12 months. After that, all usage is billed at standard rates.

| Service | Config | Estimated cost/month |
|---|---|---|
| **Lambda** (15 functions) | Python 3.12, 256 MB, ~6 000 invocations/month | ~$0.01 |
| **API Gateway HTTP v2** | ~6 000 requests/month @ $1.00/1M | ~$0.01 |
| **DynamoDB** | PAY_PER_REQUEST, ~1 MB data, PITR enabled | ~$0.01 |
| **S3** | ~5 MB static bundle, served via CloudFront cache | ~$0.00 |
| **CloudFront** | ~300 MB transfer-out/month @ $0.085/GB (EU) | ~$0.03 |
| **SSM Parameter Store** | 1 SecureString (JWT secret) | **$0.05** |
| **CloudWatch Logs** | 15 log groups, 14-day retention, low volume | ~$0.00 |
| **Total** | | **~$0.10 – $0.15 / month** |

### Cost scaling

| Scenario | Monthly active users | Estimated cost |
|---|---|---|
| Current (low usage) | ~30 | ~$0.15 |
| Moderate growth | ~200 | ~$0.50 |
| Popular app | ~2 000 | ~$3–5 |

### Notes
- **DynamoDB PITR** is enabled (`point_in_time_recovery = true`). It adds cost as
  data grows ($0.20/GB/month on stored data). Disable it if point-in-time recovery
  is not required.
- **CloudFront** has no monthly minimum fee; you only pay for data transferred out
  and request counts. With aggressive caching (default TTL 1 h, max 24 h), S3 is
  rarely hit directly.
- The most cost-effective decision in the stack is **API Gateway HTTP v2** over REST
  API: $1.00/1M vs $3.50/1M calls.

## Teardown

```bash
aws s3 rm s3://awwotations-test-frontend --recursive

cd infrastructure && terraform destroy
```



# AlNao.com
All source codes and information present in this repository are the result of careful and patient development work by AlNao, who has committed to verifying their correctness to the maximum extent possible. If any part of the code or content has been drawn from external sources, its origin is always cited, in respect of transparency and intellectual property. 

Some content and portions of code in this repository have also been created with the support of artificial intelligence tools, whose contribution made it possible to enrich and speed up the production of the material. However, each piece of information and code fragment has been carefully verified and validated, with the aim of ensuring the maximum quality and reliability of the content offered. 

For further details, in-depth information, or requests for clarification, please visit the website [alnao.com](https://www.alnao.com/).

## License
Public projects 
<a href="https://en.wikipedia.org/wiki/GNU_General_Public_License"  valign="middle"><img src="https://img.shields.io/badge/License-GNU-blue" style="height:22px;"  valign="middle"></a> 
*Free Software!*

Permission is granted to copy, distribute and/or modify this document under the terms of the GNU Free Documentation License, Version 3 or any later version published by the Free Software Foundation.


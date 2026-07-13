# 09 — Terraform

Infrastructure is defined as modular Terraform under `infrastructure/`. State is
stored on S3 with a DynamoDB lock table.

## Layout

```
infrastructure/
├── providers.tf        AWS provider + S3 backend
├── variables.tf        Root input variables
├── terraform.tfvars    Values (git-ignored: contains the JWT secret)
├── main.tf             Wires modules together; function & route maps
├── outputs.tf          api_endpoint, bucket, cloudfront, etc.
└── modules/
    ├── dynamodb/       Single table + GSI1 + GSI2
    ├── iam/            Lambda execution role & policies
    ├── lambda/         Packaging + all Lambda functions + authorizer
    ├── api_gateway/    HTTP API, JWT authorizer, routes, integrations
    └── s3_cloudfront/  Frontend bucket + CloudFront (OAC)
```

Every module exposes `variables.tf` and `outputs.tf`.

## Root variables (`variables.tf`)

| Variable               | Default                         | Purpose                          |
| ---------------------- | ------------------------------- | -------------------------------- |
| `aws_region`           | `eu-west-1`                     | Region for all resources         |
| `project_name`         | `alnao-awwotations`             | Resource name prefix             |
| `environment`          | `dev`                           | Environment suffix               |
| `jwt_secret`           | (required, sensitive)           | Stored in SSM SecureString       |
| `access_token_ttl`     | `3600`                          | Access token TTL (s)             |
| `refresh_token_ttl`    | `2592000`                       | Refresh token TTL (s)            |
| `frontend_bucket_name` | `alnao-awwotations-frontend`    | Globally-unique S3 bucket        |

## Secrets

`jwt_secret` is written to SSM Parameter Store as a `SecureString`
(`/<project>/<env>/jwt_secret`). The Lambda module reads its value and injects
it as the `JWT_SECRET` environment variable; the IAM policy grants
`ssm:GetParameter` on that parameter.

## Lambda packaging

`backend/build.sh` installs dependencies (targeting `manylinux2014` / Python
3.12) and copies `shared/` and `functions/` into `backend/build/`. The `lambda`
module zips that directory once and deploys **one artifact per function**,
differing only by `handler` (e.g. `functions.notes.pin.handler`). Run
`./build.sh` before every `terraform apply` that changes backend code.

## Function & route maps

`main.tf` declares:

- `local.functions` — map of function key → handler path.
- `local.routes` — map of `"METHOD /path"` → `{ function_key, protected }`.

The `api_gateway` module creates one integration per function and one route per
entry, attaching the JWT authorizer to every `protected = true` route. Auth
routes are public (`protected = false`).

## Deploy commands

```bash
cd backend && ./build.sh
cd ../infrastructure
terraform init
terraform plan
terraform apply
```

## Outputs

- `api_endpoint` — base URL of the HTTP API
- `dynamodb_table_name`
- `frontend_bucket_name`, `frontend_cloudfront_domain`,
  `frontend_cloudfront_distribution_id`
- `jwt_secret_parameter` — SSM parameter name

output "api_endpoint" {
  description = "Base URL of the HTTP API"
  value       = module.api_gateway.api_endpoint
}

output "dynamodb_table_name" {
  description = "DynamoDB single table name"
  value       = module.dynamodb.table_name
}

output "frontend_bucket_name" {
  description = "S3 bucket hosting the frontend"
  value       = module.s3_cloudfront.bucket_name
}

output "frontend_cloudfront_domain" {
  description = "CloudFront domain for the frontend"
  value       = module.s3_cloudfront.cloudfront_domain_name
}

output "frontend_cloudfront_distribution_id" {
  description = "CloudFront distribution id (for cache invalidation)"
  value       = module.s3_cloudfront.cloudfront_distribution_id
}

output "jwt_secret_parameter" {
  description = "SSM parameter name holding the JWT secret"
  value       = aws_ssm_parameter.jwt_secret.name
}

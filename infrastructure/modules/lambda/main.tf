locals {
  # Common environment variables shared by all application Lambdas.
  common_env = {
    TABLE_NAME        = var.table_name
    JWT_SECRET        = var.jwt_secret
    ACCESS_TOKEN_TTL  = tostring(var.access_token_ttl)
    REFRESH_TOKEN_TTL = tostring(var.refresh_token_ttl)
  }
}

# Package the pre-built backend directory (code + dependencies) into a zip.
data "archive_file" "package" {
  type        = "zip"
  source_dir  = var.build_dir
  output_path = "${path.module}/build/package.zip"
}

# CloudWatch log groups with retention for every function.
resource "aws_cloudwatch_log_group" "fn" {
  for_each          = merge(var.functions, { authorizer = { handler = var.authorizer_handler } })
  name              = "/aws/lambda/${var.project_name}-${each.key}"
  retention_in_days = 14
  tags              = var.tags
}

# Application Lambda functions (auth, boards, notes).
resource "aws_lambda_function" "fn" {
  for_each = var.functions

  function_name    = "${var.project_name}-${each.key}"
  role             = var.lambda_role_arn
  runtime          = "python3.12"
  handler          = each.value.handler
  filename         = data.archive_file.package.output_path
  source_code_hash = data.archive_file.package.output_base64sha256
  timeout          = 15
  memory_size      = 256

  environment {
    variables = local.common_env
  }

  depends_on = [aws_cloudwatch_log_group.fn]
  tags       = var.tags
}

# JWT Lambda Authorizer function.
resource "aws_lambda_function" "authorizer" {
  function_name    = "${var.project_name}-authorizer"
  role             = var.lambda_role_arn
  runtime          = "python3.12"
  handler          = var.authorizer_handler
  filename         = data.archive_file.package.output_path
  source_code_hash = data.archive_file.package.output_base64sha256
  timeout          = 10
  memory_size      = 256

  environment {
    variables = local.common_env
  }

  depends_on = [aws_cloudwatch_log_group.fn]
  tags       = var.tags
}

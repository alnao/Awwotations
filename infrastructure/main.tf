locals {
  tags = {
    Project     = var.project_name
    Environment = var.environment
  }

  # Application Lambda functions: key => handler path within the package.
  functions = {
    auth_register  = { handler = "functions.auth.register.handler" }
    auth_login     = { handler = "functions.auth.login.handler" }
    auth_refresh   = { handler = "functions.auth.refresh.handler" }
    boards_create  = { handler = "functions.boards.create.handler" }
    boards_list    = { handler = "functions.boards.list.handler" }
    boards_update  = { handler = "functions.boards.update.handler" }
    boards_delete  = { handler = "functions.boards.delete.handler" }
    notes_create   = { handler = "functions.notes.create.handler" }
    notes_list     = { handler = "functions.notes.list.handler" }
    notes_update   = { handler = "functions.notes.update.handler" }
    notes_delete   = { handler = "functions.notes.delete.handler" }
    notes_status   = { handler = "functions.notes.update_status.handler" }
    notes_pin      = { handler = "functions.notes.pin.handler" }
    notes_favorite = { handler = "functions.notes.favorite.handler" }
  }

  # API routes: route_key => { function_key, protected }.
  routes = {
    "POST /auth/register" = { function_key = "auth_register", protected = false }
    "POST /auth/login"    = { function_key = "auth_login", protected = false }
    "POST /auth/refresh"  = { function_key = "auth_refresh", protected = false }

    "GET /boards"              = { function_key = "boards_list", protected = true }
    "POST /boards"             = { function_key = "boards_create", protected = true }
    "PUT /boards/{boardId}"    = { function_key = "boards_update", protected = true }
    "DELETE /boards/{boardId}" = { function_key = "boards_delete", protected = true }

    "GET /boards/{boardId}/notes"                     = { function_key = "notes_list", protected = true }
    "POST /boards/{boardId}/notes"                    = { function_key = "notes_create", protected = true }
    "PUT /boards/{boardId}/notes/{noteId}"            = { function_key = "notes_update", protected = true }
    "DELETE /boards/{boardId}/notes/{noteId}"         = { function_key = "notes_delete", protected = true }
    "PATCH /boards/{boardId}/notes/{noteId}/status"   = { function_key = "notes_status", protected = true }
    "PATCH /boards/{boardId}/notes/{noteId}/pin"      = { function_key = "notes_pin", protected = true }
    "PATCH /boards/{boardId}/notes/{noteId}/favorite" = { function_key = "notes_favorite", protected = true }
  }
}

# Sensitive JWT secret stored in SSM Parameter Store.
resource "aws_ssm_parameter" "jwt_secret" {
  name        = "/${var.project_name}/${var.environment}/jwt_secret"
  description = "JWT signing secret for ${var.project_name}"
  type        = "SecureString"
  value       = var.jwt_secret
  tags        = local.tags
}

module "dynamodb" {
  source     = "./modules/dynamodb"
  table_name = "${var.project_name}-${var.environment}"
  tags       = local.tags
}

module "iam" {
  source                   = "./modules/iam"
  project_name             = "${var.project_name}-${var.environment}"
  table_arn                = module.dynamodb.table_arn
  jwt_secret_parameter_arn = aws_ssm_parameter.jwt_secret.arn
  tags                     = local.tags
}

module "lambda" {
  source            = "./modules/lambda"
  project_name      = "${var.project_name}-${var.environment}"
  build_dir         = "${path.module}/../backend/build"
  lambda_role_arn   = module.iam.lambda_role_arn
  table_name        = module.dynamodb.table_name
  jwt_secret        = aws_ssm_parameter.jwt_secret.value
  access_token_ttl  = var.access_token_ttl
  refresh_token_ttl = var.refresh_token_ttl
  functions         = local.functions
  tags              = local.tags
}

module "api_gateway" {
  source                   = "./modules/api_gateway"
  project_name             = "${var.project_name}-${var.environment}"
  function_invoke_arns     = module.lambda.function_arns
  function_names           = module.lambda.function_names
  authorizer_invoke_arn    = module.lambda.authorizer_invoke_arn
  authorizer_function_name = module.lambda.authorizer_function_name
  routes                   = local.routes
  tags                     = local.tags
}

module "s3_cloudfront" {
  source       = "./modules/s3_cloudfront"
  project_name = "${var.project_name}-${var.environment}"
  bucket_name  = var.frontend_bucket_name
  tags         = local.tags
}

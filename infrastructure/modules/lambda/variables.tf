variable "project_name" {
  description = "Project name prefix"
  type        = string
}

variable "build_dir" {
  description = "Path to the built backend package (code + dependencies)"
  type        = string
}

variable "lambda_role_arn" {
  description = "ARN of the Lambda execution role"
  type        = string
}

variable "table_name" {
  description = "DynamoDB table name passed to functions as TABLE_NAME"
  type        = string
}

variable "jwt_secret" {
  description = "JWT secret value (sourced from SSM by the root module)"
  type        = string
  sensitive   = true
}

variable "access_token_ttl" {
  description = "Access token TTL in seconds"
  type        = number
}

variable "refresh_token_ttl" {
  description = "Refresh token TTL in seconds"
  type        = number
}

variable "functions" {
  description = "Map of function key => { handler } for application Lambdas"
  type = map(object({
    handler = string
  }))
}

variable "authorizer_handler" {
  description = "Handler path for the JWT authorizer function"
  type        = string
  default     = "functions.authorizer.handler.handler"
}

variable "tags" {
  description = "Tags to apply"
  type        = map(string)
  default     = {}
}

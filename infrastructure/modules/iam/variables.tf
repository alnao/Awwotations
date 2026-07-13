variable "project_name" {
  description = "Project name prefix"
  type        = string
}

variable "table_arn" {
  description = "ARN of the DynamoDB table the Lambdas access"
  type        = string
}

variable "jwt_secret_parameter_arn" {
  description = "ARN of the SSM parameter holding the JWT secret"
  type        = string
}

variable "tags" {
  description = "Tags to apply"
  type        = map(string)
  default     = {}
}

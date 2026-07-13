variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "eu-west-1"
}

variable "project_name" {
  description = "Project name used as a prefix for resources"
  type        = string
  default     = "alnao-awwotations"
}

variable "environment" {
  description = "Deployment environment (e.g. dev, prod)"
  type        = string
  default     = "dev"
}

variable "jwt_secret" {
  description = "Secret used to sign JWT tokens. Stored in SSM Parameter Store."
  type        = string
  sensitive   = true
}

variable "access_token_ttl" {
  description = "Access token time-to-live in seconds"
  type        = number
  default     = 3600
}

variable "refresh_token_ttl" {
  description = "Refresh token time-to-live in seconds"
  type        = number
  default     = 2592000
}

variable "frontend_bucket_name" {
  description = "Globally unique S3 bucket name for the frontend"
  type        = string
  default     = "alnao-awwotations-frontend"
}

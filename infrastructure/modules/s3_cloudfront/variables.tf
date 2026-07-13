variable "project_name" {
  description = "Project name prefix"
  type        = string
}

variable "bucket_name" {
  description = "Globally unique S3 bucket name for the frontend"
  type        = string
}

variable "tags" {
  description = "Tags to apply"
  type        = map(string)
  default     = {}
}

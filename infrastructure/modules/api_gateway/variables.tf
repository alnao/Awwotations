variable "project_name" {
  description = "Project name prefix"
  type        = string
}

variable "function_invoke_arns" {
  description = "Map of function key => Lambda invoke ARN"
  type        = map(string)
}

variable "function_names" {
  description = "Map of function key => Lambda function name"
  type        = map(string)
}

variable "authorizer_invoke_arn" {
  description = "Invoke ARN of the JWT authorizer Lambda"
  type        = string
}

variable "authorizer_function_name" {
  description = "Name of the JWT authorizer Lambda"
  type        = string
}

variable "routes" {
  description = "Map of route_key => { function_key, protected }"
  type = map(object({
    function_key = string
    protected    = bool
  }))
}

variable "tags" {
  description = "Tags to apply"
  type        = map(string)
  default     = {}
}

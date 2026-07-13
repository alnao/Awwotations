output "function_arns" {
  description = "Map of function key => invoke ARN for application Lambdas"
  value       = { for k, f in aws_lambda_function.fn : k => f.invoke_arn }
}

output "function_names" {
  description = "Map of function key => function name"
  value       = { for k, f in aws_lambda_function.fn : k => f.function_name }
}

output "authorizer_invoke_arn" {
  description = "Invoke ARN of the JWT authorizer function"
  value       = aws_lambda_function.authorizer.invoke_arn
}

output "authorizer_function_name" {
  description = "Name of the JWT authorizer function"
  value       = aws_lambda_function.authorizer.function_name
}

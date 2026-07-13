output "api_id" {
  description = "The HTTP API id"
  value       = aws_apigatewayv2_api.http.id
}

output "api_endpoint" {
  description = "The base invoke URL of the HTTP API"
  value       = aws_apigatewayv2_api.http.api_endpoint
}

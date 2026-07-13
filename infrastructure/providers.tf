terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state on S3 with a DynamoDB lock table.
  # Fill in the values below (or via -backend-config) before running init.
  backend "s3" {
    bucket = "alnao-dev-terraform"
    key    = "awwotations/test/terraform.tfstate"
    region = "eu-central-1"
    # encrypt = true  # optional - default is true
    # dynamodb_table = "awwotations-tflock" <-- Rimosso o commentato per evitare la tabella di lock
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

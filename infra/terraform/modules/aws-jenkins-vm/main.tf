# AWS Jenkins VM Module - Complete and Self-Contained
# Creates: VPC, Subnets, Internet Gateway, NAT Gateway, Security Group, EC2, IAM, EBS
# Fully independent - no external dependencies

# VPC for Jenkins
resource "aws_vpc" "jenkins" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(var.tags, {
    Name = "vpc-jenkins-${var.environment}"
  })
}

# Internet Gateway
resource "aws_internet_gateway" "jenkins" {
  vpc_id = aws_vpc.jenkins.id

  tags = merge(var.tags, {
    Name = "igw-jenkins-${var.environment}"
  })
}

# Public Subnet for Jenkins EC2
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.jenkins.id
  cidr_block              = var.subnet_cidr
  availability_zone       = var.availability_zone != "" ? var.availability_zone : "${var.location}a"
  map_public_ip_on_launch = true

  tags = merge(var.tags, {
    Name = "subnet-jenkins-public-${var.environment}"
  })
}

# Route Table for Public Subnet
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.jenkins.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.jenkins.id
  }

  tags = merge(var.tags, {
    Name = "rt-jenkins-public-${var.environment}"
  })
}

# Route Table Association
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Security Group for Jenkins
resource "aws_security_group" "jenkins" {
  name        = "jenkins-${var.environment}-sg"
  description = "Security group for Jenkins CI/CD server"
  vpc_id      = aws_vpc.jenkins.id


  # SSH access
  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Jenkins UI
  ingress {
    description = "Jenkins UI"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SonarQube
  ingress {
    description = "SonarQube"
    from_port   = 9000
    to_port     = 9000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "sg-jenkins-${var.environment}"
  })
}

# Elastic IP for Jenkins
resource "aws_eip" "jenkins" {
  domain = "vpc"

  tags = merge(var.tags, {
    Name = "eip-jenkins-${var.environment}"
  })
}

# Key Pair for SSH access
resource "aws_key_pair" "jenkins" {
  key_name   = "jenkins-${var.environment}"
  public_key = file(var.ssh_public_key_path)

  tags = var.tags
}

# Get latest Ubuntu 22.04 AMI
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# IAM Role for Jenkins EC2
resource "aws_iam_role" "jenkins" {
  name = "jenkins-ec2-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

# IAM Policy for Jenkins to manage resources
resource "aws_iam_role_policy" "jenkins" {
  name = "jenkins-policy"
  role = aws_iam_role.jenkins.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:Describe*",
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "s3:*"
        ]
        Resource = "*"
      }
    ]
  })
}

# IAM Instance Profile
resource "aws_iam_instance_profile" "jenkins" {
  name = "jenkins-instance-profile-${var.environment}"
  role = aws_iam_role.jenkins.name

  tags = var.tags
}

# Jenkins EC2 Instance
resource "aws_instance" "jenkins" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.vm_size
  key_name      = aws_key_pair.jenkins.key_name

  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.jenkins.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.jenkins.name

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.disk_size_gb
    delete_on_termination = true
    encrypted             = true
  }

  user_data = templatefile("${path.module}/user-data.sh", {
    environment = var.environment
  })

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  tags = merge(var.tags, {
    Name = "ec2-jenkins-${var.environment}"
    Role = "CI/CD"
  })
}

# Associate Elastic IP with Jenkins instance
resource "aws_eip_association" "jenkins" {
  instance_id   = aws_instance.jenkins.id
  allocation_id = aws_eip.jenkins.id
}

# EBS Volume for Jenkins data
resource "aws_ebs_volume" "jenkins_data" {
  availability_zone = aws_instance.jenkins.availability_zone
  size              = var.data_disk_size_gb
  type              = "gp3"
  encrypted         = true

  tags = merge(var.tags, {
    Name = "ebs-jenkins-data-${var.environment}"
  })
}

# Attach data volume
resource "aws_volume_attachment" "jenkins_data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.jenkins_data.id
  instance_id = aws_instance.jenkins.id
}

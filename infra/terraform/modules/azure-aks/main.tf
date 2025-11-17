# Azure AKS Module - Complete and Self-Contained
# Creates: VNet, Subnet, NSG, AKS Cluster
# Fully independent module - no external networking dependencies

# Virtual Network for AKS
resource "azurerm_virtual_network" "aks" {
  name                = "vnet-aks-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  address_space       = [var.vnet_address_space]

  tags = merge(var.tags, {
    Name = "vnet-aks-${var.environment}"
  })
}

# Subnet for AKS nodes
resource "azurerm_subnet" "aks" {
  name                 = "snet-aks"
  resource_group_name  = var.resource_group_name
  virtual_network_name = azurerm_virtual_network.aks.name
  address_prefixes     = [var.subnet_address_prefix]
}

# Network Security Group for AKS
resource "azurerm_network_security_group" "aks" {
  name                = "nsg-aks-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name

  # Allow HTTP traffic to API Gateway LoadBalancer
  security_rule {
    name                       = "AllowHTTP"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # Allow HTTPS traffic
  security_rule {
    name                       = "AllowHTTPS"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  tags = var.tags
}

# Associate NSG with AKS subnet
resource "azurerm_subnet_network_security_group_association" "aks" {
  subnet_id                 = azurerm_subnet.aks.id
  network_security_group_id = azurerm_network_security_group.aks.id
}

# AKS Cluster
resource "azurerm_kubernetes_cluster" "main" {
  name                = "aks-${var.environment}-${var.project_name}"
  location            = var.location
  resource_group_name = var.resource_group_name
  dns_prefix          = "aks-${var.environment}"

  default_node_pool {
    name                = "default"
    node_count          = var.enable_autoscaling ? null : var.node_count
    vm_size             = var.node_vm_size
    vnet_subnet_id      = azurerm_subnet.aks.id
    enable_auto_scaling = var.enable_autoscaling
    min_count           = var.enable_autoscaling ? var.min_node_count : null
    max_count           = var.enable_autoscaling ? var.max_node_count : null
    os_disk_size_gb     = 64
  }

  # Managed identity
  identity {
    type = "SystemAssigned"
  }

  # Network configuration
  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "standard"
    service_cidr      = var.service_cidr
    dns_service_ip    = var.dns_service_ip
  }

  tags = merge(var.tags, {
    Name        = "aks-${var.environment}"
    Environment = var.environment
  })
}

# Role assignment for AKS to pull images from container registry
resource "azurerm_role_assignment" "aks_acr_pull" {
  count                = var.container_registry_id != "" ? 1 : 0
  principal_id         = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
  role_definition_name = "AcrPull"
  scope                = var.container_registry_id
}

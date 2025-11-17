# Azure Jenkins VM Module - Complete and Self-Contained
# Creates: VNet, Subnet, NSG, Public IP, NIC, VM, Managed Disk

# Virtual Network for Jenkins
resource "azurerm_virtual_network" "jenkins" {
  name                = "vnet-jenkins-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  address_space       = [var.vpc_cidr]

  tags = merge(var.tags, {
    Name = "vnet-jenkins-${var.environment}"
  })
}

# Subnet for Jenkins VM
resource "azurerm_subnet" "jenkins" {
  name                 = "snet-jenkins"
  resource_group_name  = var.resource_group_name
  virtual_network_name = azurerm_virtual_network.jenkins.name
  address_prefixes     = [var.subnet_cidr]
}

# Network Security Group for Jenkins
resource "azurerm_network_security_group" "jenkins" {
  name                = "nsg-jenkins-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name

  # SSH
  security_rule {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # Jenkins
  security_rule {
    name                       = "Jenkins"
    priority                   = 1002
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "8080"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # SonarQube
  security_rule {
    name                       = "SonarQube"
    priority                   = 1003
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "9000"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # HTTP
  security_rule {
    name                       = "HTTP"
    priority                   = 1004
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  # HTTPS
  security_rule {
    name                       = "HTTPS"
    priority                   = 1005
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

# Associate NSG with subnet
resource "azurerm_subnet_network_security_group_association" "jenkins" {
  subnet_id                 = azurerm_subnet.jenkins.id
  network_security_group_id = azurerm_network_security_group.jenkins.id
}

# Public IP for Jenkins
resource "azurerm_public_ip" "jenkins" {
  name                = "pip-jenkins-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"

  tags = merge(var.tags, {
    Name = "jenkins-${var.environment}"
  })
}

# Network Interface
resource "azurerm_network_interface" "jenkins" {
  name                = "nic-jenkins-${var.environment}"
  location            = var.location
  resource_group_name = var.resource_group_name

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.jenkins.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.jenkins.id
  }

  tags = var.tags
}

# Virtual Machine for Jenkins
resource "azurerm_linux_virtual_machine" "jenkins" {
  name                  = "vm-jenkins-${var.environment}"
  location              = var.location
  resource_group_name   = var.resource_group_name
  network_interface_ids = [azurerm_network_interface.jenkins.id]
  size                  = var.vm_size

  # Ubuntu 22.04 LTS
  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  os_disk {
    name                 = "osdisk-jenkins-${var.environment}"
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"  # Standard HDD - cost-optimized
    disk_size_gb         = var.disk_size_gb
  }

  computer_name                   = "jenkins"
  admin_username                  = var.admin_username
  disable_password_authentication = true

  admin_ssh_key {
    username   = var.admin_username
    public_key = file(var.ssh_public_key_path)
  }

  # Managed identity for Azure resource access
  identity {
    type = "SystemAssigned"
  }

  tags = merge(var.tags, {
    Name = "jenkins-${var.environment}"
    Role = "CI/CD"
  })
}

# Managed Disk for Jenkins data
resource "azurerm_managed_disk" "jenkins_data" {
  name                 = "disk-jenkins-data-${var.environment}"
  location             = var.location
  resource_group_name  = var.resource_group_name
  storage_account_type = "Standard_LRS"  # Standard HDD - cost-optimized
  create_option        = "Empty"
  disk_size_gb         = var.data_disk_size_gb

  tags = var.tags
}

# Attach data disk
resource "azurerm_virtual_machine_data_disk_attachment" "jenkins_data" {
  managed_disk_id    = azurerm_managed_disk.jenkins_data.id
  virtual_machine_id = azurerm_linux_virtual_machine.jenkins.id
  lun                = 0
  caching            = "ReadWrite"
}

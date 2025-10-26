$originalPath = Get-Location
$prefix = "$originalPath/services"

$folders = @{
    "api-gateway" = $true
    "cloud-config" = $true
    "favourite-service" = $true
    "order-service" = $true
    "payment-service" = $true
    "product-service" = $true
    "proxy-client" = $true
    "service-discovery" = $true
    "shipping-service" = $true
    "user-service" = $true
}

foreach ($folder in $folders.Keys) {
    if ($folders[$folder]) {
        Write-Host "Building service in folder: $folder"
        Set-Location "$prefix/$folder"
        ./mvnw package
        Set-Location $originalPath
    }
}
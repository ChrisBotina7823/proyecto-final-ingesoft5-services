# Script to add CircuitBreaker exclusion and @MockBean RestTemplate to integration tests
# This is a helper script that will perform simple file edits across services. Use with caution.

$services = @("product-service","order-service","payment-service","shipping-service")
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
# Note: This script assumes it's placed under scripts/ and executed from the repo root.

foreach ($s in $services) {
    Write-Host "Processing $s..."
    $testDir = Join-Path "..\$s\src\test\java" "com\selimhorri\app\resource"
    if (Test-Path $testDir) {
        Get-ChildItem -Path $testDir -Filter "*IntegrationTest.java" -Recurse | ForEach-Object {
            $file = $_.FullName
            Write-Host " - Editing $file"
            (Get-Content $file) -replace 'import org.springframework.boot.test.context.SpringBootTest;', "import org.springframework.boot.test.context.SpringBootTest;`nimport org.springframework.boot.autoconfigure.EnableAutoConfiguration;`nimport org.springframework.boot.test.mock.mockito.MockBean;`nimport org.springframework.web.client.RestTemplate;`nimport io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakersHealthIndicatorAutoConfiguration;" | 
                Set-Content $file
            (Get-Content $file) -replace '@ActiveProfiles\("test"\)\r?\n@Transactional', '@ActiveProfiles("test")`n@Transactional`n@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})' | Set-Content $file
            (Get-Content $file) -replace 'private ObjectMapper objectMapper;','private ObjectMapper objectMapper;`n`n    @MockBean`n    private RestTemplate restTemplate;' | Set-Content $file
        }
    } else {
        Write-Host " - Test directory not found for $s"
    }
}
Write-Host "Done. Review files and run git add/commit as needed."
Set-Location "$PSScriptRoot\backend"

# Load .env file
Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.+)$') {
        [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), "Process")
    }
}

# Start Spring Boot
mvn spring-boot:run

param(
    [int] $BackendPort = 8080,
    [int] $FrontendPort = 4000,
    [string] $FrontendHost = "0.0.0.0",
    [string] $PistonBaseUrl = $(if ($env:PISTON_BASE_URL) { $env:PISTON_BASE_URL } else { "http://127.0.0.1:2238/api/v2" }),
    [string] $RedisHost = $(if ($env:REDIS_HOST) { $env:REDIS_HOST } else { "127.0.0.1" }),
    [int] $RedisPort = $(if ($env:REDIS_PORT) { [int] $env:REDIS_PORT } else { 6379 }),
    [string] $QdrantRestUrl = $(if ($env:QDRANT_REST_URL) { $env:QDRANT_REST_URL } else { "http://127.0.0.1:6333" }),
    [switch] $Production,
    [switch] $SkipDocker,
    [switch] $SkipPistonCheck,
    [switch] $SkipNpmInstall
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$RepoRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $RepoRoot "backend"
$FrontendDir = Join-Path $RepoRoot "frontend"
$LogDir = Join-Path $RepoRoot ".local-test-env"
$BackendUrl = "http://127.0.0.1:$BackendPort"
$FrontendUrl = "http://127.0.0.1:$FrontendPort"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Step {
    param([string] $Message)
    Write-Host ""
    Write-Host "== $Message =="
}

function Quote-PowerShellLiteral {
    param([string] $Value)
    return "'" + ($Value -replace "'", "''") + "'"
}

function Get-HttpStatus {
    param([string] $Url)
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $status = & curl.exe --noproxy "*" -sS -o NUL -w "%{http_code}" --max-time 8 $Url 2>$null
        if ($LASTEXITCODE -ne 0) {
            return "000"
        }
        return $status
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Wait-HttpReady {
    param(
        [string] $Url,
        [int] $TimeoutSeconds = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = Get-HttpStatus $Url
        if ($status -eq "200") {
            return $true
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    return $false
}

function Start-HiddenPowerShell {
    param(
        [string] $Name,
        [string] $WorkingDirectory,
        [string] $Command
    )
    $stdout = Join-Path $LogDir "$Name.out.log"
    $stderr = Join-Path $LogDir "$Name.err.log"
    $process = Start-Process `
        -FilePath "powershell" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $Command) `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru
    Write-Host "$Name started: pid=$($process.Id), stdout=$stdout, stderr=$stderr"
}

function Get-LanAddress {
    $ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            $_.PrefixOrigin -in @("Dhcp", "Manual")
        } |
        Sort-Object InterfaceMetric, InterfaceIndex |
        Select-Object -First 1 -ExpandProperty IPAddress

    if ([string]::IsNullOrWhiteSpace($ip)) {
        return "YOUR_LAN_IP"
    }
    return $ip
}

Write-Step "Runtime dependencies"
if (-not $SkipDocker) {
    $dockerVersion = & docker version --format "{{.Server.Version}}" 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dockerVersion)) {
        throw "Docker is not ready. Start Docker Desktop, then run this script again."
    }

    Push-Location $RepoRoot
    try {
        docker compose up -d redis qdrant
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose up -d redis qdrant failed."
        }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "SkipDocker enabled; not starting Redis or Qdrant."
}

if (-not $SkipPistonCheck) {
    $pistonStatus = Get-HttpStatus "$PistonBaseUrl/runtimes"
    if ($pistonStatus -ne "200") {
        Write-Warning "Piston is not ready: $PistonBaseUrl/runtimes => $pistonStatus. Start Piston separately; this script will not remove or recreate existing Piston containers."
    }
    else {
        Write-Host "Piston ready: $PistonBaseUrl/runtimes"
    }
}

Write-Step "Backend"
if (Wait-HttpReady "$BackendUrl/api/problems" 2) {
    Write-Host "Backend already ready: $BackendUrl"
}
else {
    $repoLiteral = Quote-PowerShellLiteral $RepoRoot
    $backendLiteral = Quote-PowerShellLiteral $BackendDir
    $pistonLiteral = Quote-PowerShellLiteral $PistonBaseUrl
    $redisHostLiteral = Quote-PowerShellLiteral $RedisHost
    $qdrantRestLiteral = Quote-PowerShellLiteral $QdrantRestUrl

    $backendCommand = @"
Set-Location $backendLiteral
`$envFile = Join-Path $repoLiteral '.env'
if (Test-Path -LiteralPath `$envFile) {
    Get-Content -LiteralPath `$envFile | ForEach-Object {
        if (`$_ -match '^([^#][^=]+)=(.+)$') {
            [Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim(), 'Process')
        }
    }
}
`$env:SERVER_PORT = '$BackendPort'
`$env:PISTON_BASE_URL = $pistonLiteral
`$env:REDIS_HOST = $redisHostLiteral
`$env:REDIS_PORT = '$RedisPort'
`$env:QDRANT_REST_URL = $qdrantRestLiteral
mvn spring-boot:run
"@
    Start-HiddenPowerShell "backend-$BackendPort" $BackendDir $backendCommand
}

if (-not (Wait-HttpReady "$BackendUrl/api/problems" 120)) {
    throw "Backend did not become ready: $BackendUrl/api/problems. Check .local-test-env backend logs."
}

Write-Step "Frontend"
if (Wait-HttpReady "$FrontendUrl/problem/1" 2) {
    Write-Host "Frontend already ready: $FrontendUrl"
}
else {
    $frontendLiteral = Quote-PowerShellLiteral $FrontendDir
    $frontendHostLiteral = Quote-PowerShellLiteral $FrontendHost
    $installCommand = ""
    if (-not $SkipNpmInstall) {
        $installCommand = "if (-not (Test-Path -LiteralPath 'node_modules')) { npm install; if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE } }"
    }

    if ($Production) {
        $frontendCommand = @"
Set-Location $frontendLiteral
$installCommand
npm run build
if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
npx next start -H $frontendHostLiteral -p $FrontendPort
"@
    }
    else {
        $frontendCommand = @"
Set-Location $frontendLiteral
$installCommand
npx next dev --hostname $frontendHostLiteral --port $FrontendPort
"@
    }

    Start-HiddenPowerShell "frontend-$FrontendPort" $FrontendDir $frontendCommand
}

if (-not (Wait-HttpReady "$FrontendUrl/problem/1" 120)) {
    throw "Frontend did not become ready: $FrontendUrl/problem/1. Check .local-test-env frontend logs."
}

Write-Step "Preflight"
$env:BACKEND_URL = $BackendUrl
$env:FRONTEND_URL = $FrontendUrl
$env:PISTON_BASE_URL = $PistonBaseUrl
$env:REDIS_HOST = $RedisHost
$env:REDIS_PORT = [string] $RedisPort
$env:QDRANT_REST_URL = $QdrantRestUrl
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "local_dependency_preflight.ps1") -FailOnMissing
if ($LASTEXITCODE -ne 0) {
    throw "Preflight failed. Fix the missing service above before sending the URL to testers."
}

$lanIp = Get-LanAddress
Write-Step "Ready for local testing"
Write-Host "Local URL: $FrontendUrl"
Write-Host "LAN URL:   http://$lanIp`:$FrontendPort"
Write-Host "Logs:      $LogDir"
Write-Host "Next: open the LAN URL from another device, then follow docs/MULTI_USER_TEST_GUIDE.md."

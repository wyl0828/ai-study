param(
    [string] $BackendUrl = $(if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://127.0.0.1:8080" }),
    [string] $FrontendUrl = $(if ($env:FRONTEND_URL) { $env:FRONTEND_URL } else { "http://127.0.0.1:4000" }),
    [string] $PistonBaseUrl = $(if ($env:PISTON_BASE_URL) { $env:PISTON_BASE_URL } else { "http://127.0.0.1:2238/api/v2" }),
    [string] $QdrantUrl = $(if ($env:QDRANT_REST_URL) { $env:QDRANT_REST_URL } else { "http://127.0.0.1:6333" }),
    [string] $RedisHost = $(if ($env:REDIS_HOST) { $env:REDIS_HOST } else { "127.0.0.1" }),
    [int] $RedisPort = $(if ($env:REDIS_PORT) { [int] $env:REDIS_PORT } else { 6379 }),
    [string] $MysqlExe = $(if ($env:MYSQL_EXE) { $env:MYSQL_EXE } else { "mysql" }),
    [string] $MysqlUser = $(if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }),
    [string] $MysqlPassword = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "123456" }),
    [switch] $FailOnMissing
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

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

function Test-TcpPort {
    param(
        [string] $HostName,
        [int] $Port
    )
    return Test-NetConnection -ComputerName $HostName -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
}

function Add-PreflightCheck {
    param(
        [System.Collections.Generic.List[object]] $Checks,
        [string] $Service,
        [bool] $Ready,
        [string] $Detail,
        [string] $NextAction
    )
    $Checks.Add([pscustomobject]@{
        Service = $Service
        Status = $(if ($Ready) { "READY" } else { "MISSING" })
        Detail = $Detail
        NEXT_ACTION = $(if ($Ready) { "-" } else { $NextAction })
    }) | Out-Null
}

$checks = [System.Collections.Generic.List[object]]::new()

$previousMysqlPwd = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $MysqlPassword
    $mysqlOutput = & $MysqlExe "--user=$MysqlUser" -N -B -e "SELECT 1;" 2>$null
    $mysqlReady = ($LASTEXITCODE -eq 0 -and $mysqlOutput.Trim() -eq "1")
    Add-PreflightCheck $checks "MySQL" $mysqlReady "SELECT 1 via $MysqlExe" "Start MySQL 8 and confirm database ai_interview_coach is reachable."
}
catch {
    Add-PreflightCheck $checks "MySQL" $false $_.Exception.Message "Start MySQL 8 and confirm MYSQL_USERNAME / MYSQL_PASSWORD."
}
finally {
    $env:MYSQL_PWD = $previousMysqlPwd
}

$backendStatus = Get-HttpStatus "$BackendUrl/api/problems"
Add-PreflightCheck $checks "Backend" ($backendStatus -eq "200") "$BackendUrl/api/problems => $backendStatus" "Run powershell -File start-backend.ps1."

$frontendStatus = Get-HttpStatus "$FrontendUrl/problem/1"
Add-PreflightCheck $checks "Frontend" ($frontendStatus -eq "200") "$FrontendUrl/problem/1 => $frontendStatus" "Run npm run dev under frontend; default URL is http://127.0.0.1:4000."

$pistonStatus = Get-HttpStatus "$PistonBaseUrl/runtimes"
Add-PreflightCheck $checks "Piston" ($pistonStatus -eq "200") "$PistonBaseUrl/runtimes => $pistonStatus" "Start local Piston and align PISTON_BASE_URL, commonly http://127.0.0.1:2238/api/v2."

$redisReady = Test-TcpPort $RedisHost $RedisPort
Add-PreflightCheck $checks "Redis" $redisReady "$RedisHost`:$RedisPort tcp => $(if ($redisReady) { "open" } else { "closed" })" "Start Redis, for this repo usually docker compose up -d redis."

$qdrantStatus = Get-HttpStatus "$QdrantUrl/healthz"
Add-PreflightCheck $checks "Qdrant" ($qdrantStatus -eq "200") "$QdrantUrl/healthz => $qdrantStatus" "Start Qdrant with docker compose up -d qdrant."

$previousErrorActionPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = "Continue"
    $dockerVersion = & docker version --format "{{.Server.Version}}" 2>$null
    $dockerReady = ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($dockerVersion))
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
Add-PreflightCheck $checks "Docker" $dockerReady "docker version => $(if ($dockerReady) { $dockerVersion.Trim() } else { "unavailable" })" "Start Docker Desktop before launching Redis / Qdrant / Piston containers."

$readyForE2e = -not ($checks | Where-Object { $_.Status -ne "READY" })
$firstMissing = $checks | Where-Object { $_.Status -ne "READY" } | Select-Object -First 1

Write-Host "local_dependency_preflight: AI Study local runtime check"
$checks | Format-Table -AutoSize
Write-Host "READY_FOR_E2E_SMOKE=$readyForE2e"
if ($firstMissing) {
    Write-Host "NEXT_ACTION=$($firstMissing.Service): $($firstMissing.NEXT_ACTION)"
}
else {
    Write-Host "NEXT_ACTION=Run scripts\e2e_demo_smoke.ps1"
}

if ($FailOnMissing -and -not $readyForE2e) {
    exit 1
}

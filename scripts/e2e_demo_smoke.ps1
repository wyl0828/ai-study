param(
    [int] $UserId = 1,
    [int] $ProblemId = 1,
    [string] $BackendUrl = $(if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://127.0.0.1:8080" }),
    [string[]] $FrontendUrls = $(if ($env:FRONTEND_URL) { @($env:FRONTEND_URL) } else { @("http://127.0.0.1:3000", "http://127.0.0.1:4000") }),
    [string] $PistonBaseUrl = $(if ($env:PISTON_BASE_URL) { $env:PISTON_BASE_URL } else { "http://127.0.0.1:2238/api/v2" }),
    [string] $QdrantUrl = $(if ($env:QDRANT_REST_URL) { $env:QDRANT_REST_URL } else { "http://127.0.0.1:6333" }),
    [string] $MysqlExe = $(if ($env:MYSQL_EXE) { $env:MYSQL_EXE } else { "D:\develop\mysql-8.0.34-winx64\bin\mysql.exe" }),
    [string] $MysqlUser = $(if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }),
    [string] $MysqlPassword = $(if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "123456" }),
    [string] $MysqlDatabase = $(if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "ai_interview_coach" }),
    [int] $EmbeddingTimeoutSeconds = 60,
    [switch] $StartQdrant,
    [switch] $RunRagRebuild,
    [switch] $SkipFrontend,
    [switch] $SkipEmbedding,
    [switch] $SkipMysql
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$RepoRoot = Split-Path -Parent $PSScriptRoot
$TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ai-study-e2e-demo-" + [System.Guid]::NewGuid())
$Evidence = [ordered]@{}

New-Item -ItemType Directory -Path $TempDir | Out-Null

function Write-Step {
    param([string] $Message)
    Write-Host ""
    Write-Host "== $Message =="
}

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-CurlText {
    param([string[]] $CurlArgs)
    $outputPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".curl.out")
    & curl.exe --noproxy "*" -sS --max-time 240 -o $outputPath @CurlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "curl failed with exit code $LASTEXITCODE`: $($CurlArgs -join ' ')"
    }
    return Get-Content -LiteralPath $outputPath -Raw -Encoding utf8
}

function Get-HttpStatus {
    param([string] $Url)
    $status = & curl.exe --noproxy "*" -sS -o NUL -w "%{http_code}" --max-time 20 $Url
    if ($LASTEXITCODE -ne 0) {
        return "000"
    }
    return $status
}

function ConvertTo-DemoJson {
    param([object] $Value)
    if ($null -eq $Value) {
        return "null"
    }
    if ($Value -is [string]) {
        $escaped = $Value `
            -replace "\\", "\\" `
            -replace '"', '\"' `
            -replace "`r", "\r" `
            -replace "`n", "\n" `
            -replace "`t", "\t"
        return '"' + $escaped + '"'
    }
    if ($Value -is [bool]) {
        if ($Value) { return "true" }
        return "false"
    }
    if ($Value -is [int] -or $Value -is [long] -or $Value -is [double] -or $Value -is [decimal]) {
        return [string]::Format([Globalization.CultureInfo]::InvariantCulture, "{0}", $Value)
    }
    if ($Value -is [System.Collections.IDictionary]) {
        $parts = @()
        foreach ($key in $Value.Keys) {
            $parts += (ConvertTo-DemoJson ([string] $key)) + ":" + (ConvertTo-DemoJson $Value[$key])
        }
        return "{" + ($parts -join ",") + "}"
    }
    if ($Value -is [System.Collections.IEnumerable]) {
        $parts = @()
        foreach ($item in $Value) {
            $parts += ConvertTo-DemoJson $item
        }
        return "[" + ($parts -join ",") + "]"
    }
    return ConvertTo-DemoJson ([string] $Value)
}

function Invoke-JsonPost {
    param(
        [string] $Url,
        [object] $Body
    )
    $bodyPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".json")
    $outputPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".json.out")
    Write-Host "preparing POST $Url ..."
    $json = ConvertTo-DemoJson $Body
    Set-Content -LiteralPath $bodyPath -Value $json -NoNewline -Encoding utf8
    Write-Host "calling POST $Url ..."
    & curl.exe --noproxy "*" -sS --max-time 240 -o $outputPath `
        "-H" "Content-Type: application/json" `
        "--data-binary" "@$bodyPath" `
        $Url
    if ($LASTEXITCODE -ne 0) {
        throw "curl POST failed with exit code $LASTEXITCODE`: $Url"
    }
    Write-Host "POST returned $Url"
    $text = Get-Content -LiteralPath $outputPath -Raw -Encoding utf8
    return $text | ConvertFrom-Json
}

function Invoke-JsonGet {
    param([string] $Url)
    $outputPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".json.out")
    & curl.exe --noproxy "*" -sS --max-time 240 -o $outputPath $Url
    if ($LASTEXITCODE -ne 0) {
        throw "curl GET failed with exit code $LASTEXITCODE`: $Url"
    }
    $text = Get-Content -LiteralPath $outputPath -Raw -Encoding utf8
    return $text | ConvertFrom-Json
}

function Invoke-MysqlScalar {
    param([string] $Sql)
    Assert-True (Test-Path -LiteralPath $MysqlExe) "mysql executable not found: $MysqlExe. Set MYSQL_EXE or pass -SkipMysql."
    $oldMysqlPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MysqlPassword
        $result = & $MysqlExe "-u$MysqlUser" "-N" "-B" "-e" "USE $MysqlDatabase; $Sql"
        if ($LASTEXITCODE -ne 0) {
            throw "mysql query failed: $Sql"
        }
    } finally {
        $env:MYSQL_PWD = $oldMysqlPwd
    }
    return $result
}

function Invoke-PowerShellScriptWithTimeout {
    param(
        [string] $ScriptPath,
        [int] $TimeoutSeconds,
        [string] $Name
    )
    $stdoutPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".out")
    $stderrPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".err")
    $process = Start-Process `
        -FilePath "powershell" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $ScriptPath) `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -WindowStyle Hidden `
        -PassThru

    $completed = $process.WaitForExit($TimeoutSeconds * 1000)
    if (-not $completed) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        throw "$Name timed out after $TimeoutSeconds seconds."
    }
    $process.Refresh()

    $stdout = if (Test-Path -LiteralPath $stdoutPath) { Get-Content -LiteralPath $stdoutPath -Raw } else { "" }
    $stderr = if (Test-Path -LiteralPath $stderrPath) { Get-Content -LiteralPath $stderrPath -Raw } else { "" }
    if (-not [string]::IsNullOrWhiteSpace($stdout)) {
        Write-Host $stdout.TrimEnd()
    }
    $exitCode = $process.ExitCode
    if ($null -eq $exitCode) {
        $exitCode = 0
    }
    if ($exitCode -ne 0) {
        throw "$Name failed with exit code $exitCode. $stderr"
    }
}

function Assert-ApiSuccess {
    param(
        [object] $Response,
        [string] $Name
    )
    Assert-True ($null -ne $Response) "$Name returned empty response."
    Assert-True ($Response.code -eq 0) "$Name failed: code=$($Response.code), message=$($Response.message)"
}

function Submit-Code {
    param(
        [string] $CodePath,
        [string] $ExpectedStatus
    )
    $code = Get-Content -LiteralPath $CodePath -Raw
    Write-Host "submitting $([System.IO.Path]::GetFileName($CodePath)) ..."
    $response = Invoke-JsonPost -Url "$BackendUrl/api/submissions" -Body @{
        userId = $UserId
        problemId = $ProblemId
        language = "JAVA"
        code = $code
    }
    Assert-ApiSuccess $response "submit $CodePath"
    Assert-True ($response.data.status -eq $ExpectedStatus) "Expected submission status $ExpectedStatus, got $($response.data.status)."
    Write-Host "submissionId=$($response.data.submissionId), status=$($response.data.status)"
    return $response.data
}

function Read-Sse {
    param([long] $SubmissionId)
    Write-Host "reading SSE for submissionId=$SubmissionId ..."
    $outputPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".sse.out")
    & curl.exe --noproxy "*" -sS --no-buffer --max-time 180 -o $outputPath "$BackendUrl/api/submissions/$SubmissionId/diagnosis/stream"
    if ($LASTEXITCODE -ne 0) {
        throw "curl SSE failed with exit code $LASTEXITCODE for submissionId=$SubmissionId"
    }
    return Get-Content -LiteralPath $outputPath -Raw -Encoding utf8
}

function Assert-SseSuccessSteps {
    param(
        [string] $SseText,
        [string[]] $ExpectedSteps
    )
    $lastIndex = -1
    foreach ($step in $ExpectedSteps) {
        $needle = '"stepName":"' + $step + '"'
        $idx = $SseText.IndexOf($needle)
        Assert-True ($idx -ge 0) "SSE did not contain step $step."
        Assert-True ($idx -gt $lastIndex) "SSE step order is wrong around $step."
        $pattern = '"stepName":"' + [regex]::Escape($step) + '".*?"status":"SUCCESS"'
        Assert-True ([regex]::IsMatch($SseText, $pattern)) "SSE step $step did not finish with SUCCESS."
        $lastIndex = $idx
    }
    Assert-True ($SseText.Contains("event:done")) "SSE did not emit done event."
}

try {
    Write-Step "Prerequisite checks"
    if ($StartQdrant) {
        docker compose up -d qdrant
        Assert-True ($LASTEXITCODE -eq 0) "docker compose up -d qdrant failed."
    }

    $backendStatus = Get-HttpStatus "$BackendUrl/api/problems"
    Assert-True ($backendStatus -eq "200") "Backend is not ready: $BackendUrl/api/problems returned $backendStatus."
    $Evidence["backend"] = "$BackendUrl/api/problems => $backendStatus"

    $pistonStatus = Get-HttpStatus "$PistonBaseUrl/runtimes"
    Assert-True ($pistonStatus -eq "200") "Piston is not ready: $PistonBaseUrl/runtimes returned $pistonStatus."
    $Evidence["piston"] = "$PistonBaseUrl/runtimes => $pistonStatus"

    $qdrantHealth = Get-HttpStatus "$QdrantUrl/healthz"
    Assert-True ($qdrantHealth -eq "200") "Qdrant is not ready: $QdrantUrl/healthz returned $qdrantHealth."
    $Evidence["qdrant"] = "$QdrantUrl/healthz => $qdrantHealth"

    if (-not $SkipEmbedding) {
        Invoke-PowerShellScriptWithTimeout `
            -ScriptPath (Join-Path $RepoRoot "scripts\embedding_smoke.ps1") `
            -TimeoutSeconds $EmbeddingTimeoutSeconds `
            -Name "Embedding smoke"
        $Evidence["embedding"] = "scripts/embedding_smoke.ps1 passed"
    }

    if (-not $SkipFrontend) {
        $frontendOk = $false
        foreach ($frontendUrl in $FrontendUrls) {
            $problemStatus = Get-HttpStatus "$frontendUrl/problem/$ProblemId"
            $dashboardStatus = Get-HttpStatus "$frontendUrl/dashboard"
            if ($problemStatus -eq "200" -and $dashboardStatus -eq "200") {
                $frontendOk = $true
                $Evidence["frontend"] = "$frontendUrl/problem/$ProblemId => 200; $frontendUrl/dashboard => 200"
                break
            }
        }
        Assert-True $frontendOk "Frontend is not ready. Tried: $($FrontendUrls -join ', ')"
    }

    Write-Step "Backend API baseline"
    $problemList = Invoke-JsonGet "$BackendUrl/api/problems"
    Assert-ApiSuccess $problemList "problem list"
    $problemIds = @($problemList.data | ForEach-Object { [int] $_.id })
    Assert-True ($problemIds.Count -ge 20) "Problem list should include at least 20 stable training problems."
    Assert-True ($problemIds -contains 1) "Problem list missing demo problem 1."
    Assert-True ($problemIds -contains 206) "Problem list missing demo problem 206."
    Assert-True ($problemIds -contains 121) "Problem list missing demo problem 121."
    $Evidence["problem_list"] = "count=$($problemIds.Count), demoIds=1/206/121"

    $problem = Invoke-JsonGet "$BackendUrl/api/problems/$ProblemId"
    Assert-ApiSuccess $problem "problem detail"
    Assert-True ($null -ne $problem.data.presetHints) "Problem detail did not include presetHints."
    $unifiedCacheStatus = Invoke-JsonGet "$BackendUrl/api/cache/status"
    Assert-ApiSuccess $unifiedCacheStatus "unified cache status"
    Assert-True ($unifiedCacheStatus.data.provider -eq "Redis") "Unified cache provider is not Redis."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem) "Unified cache status missing problem summary."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge) "Unified cache status missing knowledge summary."
    Assert-True ($null -ne $unifiedCacheStatus.data.checkedAt) "Unified cache status missing checkedAt."
    Assert-True ($unifiedCacheStatus.data.maintenanceAction -match "/api/cache/refresh") "Unified cache status missing actionable maintenanceAction."
    Assert-True ($null -ne $unifiedCacheStatus.data.cachedKeyCount) "Unified cache status missing cachedKeyCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.allEnabled) "Unified cache status missing allEnabled."
    Assert-True ($null -ne $unifiedCacheStatus.data.allRedisAvailable) "Unified cache status missing allRedisAvailable."
    Assert-True ($null -ne $unifiedCacheStatus.data.statusLabel) "Unified cache status missing statusLabel."
    Assert-True ($null -ne $unifiedCacheStatus.data.summary) "Unified cache status missing readable summary."
    Assert-True ($null -ne $unifiedCacheStatus.data.cacheBenefitSummary) "Unified cache status missing cacheBenefitSummary."
    Assert-True ($null -ne $unifiedCacheStatus.data.fallbackRiskSummary) "Unified cache status missing fallbackRiskSummary."
    Assert-True ($null -ne $unifiedCacheStatus.data.protectedDataSummary) "Unified cache status missing protectedDataSummary."
    Assert-True ($unifiedCacheStatus.data.protectedDataSummary -match "MySQL") "Unified cache protectedDataSummary does not mention MySQL boundary."
    Assert-True ($null -ne $unifiedCacheStatus.data.hitCount) "Unified cache status missing hitCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.missCount) "Unified cache status missing missCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.fallbackCount) "Unified cache status missing fallbackCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.hitRate) "Unified cache status missing hitRate."
    Assert-True ($unifiedCacheStatus.data.PSObject.Properties.Name -contains "lastFallbackReason") "Unified cache status missing lastFallbackReason."
    Assert-True ($unifiedCacheStatus.data.PSObject.Properties.Name -contains "probeWarning") "Unified cache status missing probeWarning."
    Assert-True ($unifiedCacheStatus.data.summary -match "problem=") "Unified cache status summary missing problem sub-status."
    Assert-True ($unifiedCacheStatus.data.summary -match "knowledge=") "Unified cache status summary missing knowledge sub-status."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.checkedAt) "Unified problem cache status missing checkedAt."
    Assert-True ($unifiedCacheStatus.data.problem.maintenanceAction -match "/api/problems/cache/refresh") "Unified problem cache status missing maintenanceAction."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.hitCount) "Unified problem cache status missing hitCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.missCount) "Unified problem cache status missing missCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.fallbackCount) "Unified problem cache status missing fallbackCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.hitRate) "Unified problem cache status missing hitRate."
    Assert-True ($unifiedCacheStatus.data.problem.PSObject.Properties.Name -contains "lastFallbackReason") "Unified problem cache status missing lastFallbackReason."
    Assert-True ($unifiedCacheStatus.data.problem.PSObject.Properties.Name -contains "probeWarning") "Unified problem cache status missing probeWarning."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.listCached) "Unified problem cache status missing listCached."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.detailCachedKeyCount) "Unified problem cache status missing detailCachedKeyCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.problem.templateCachedKeyCount) "Unified problem cache status missing templateCachedKeyCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.checkedAt) "Unified knowledge cache status missing checkedAt."
    Assert-True ($unifiedCacheStatus.data.knowledge.maintenanceAction -match "/api/knowledge/cache/refresh") "Unified knowledge cache status missing maintenanceAction."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.hitCount) "Unified knowledge cache status missing hitCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.missCount) "Unified knowledge cache status missing missCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.fallbackCount) "Unified knowledge cache status missing fallbackCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.hitRate) "Unified knowledge cache status missing hitRate."
    Assert-True ($unifiedCacheStatus.data.knowledge.PSObject.Properties.Name -contains "lastFallbackReason") "Unified knowledge cache status missing lastFallbackReason."
    Assert-True ($unifiedCacheStatus.data.knowledge.PSObject.Properties.Name -contains "probeWarning") "Unified knowledge cache status missing probeWarning."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.categoryCached) "Unified knowledge cache status missing categoryCached."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.listCachedKeyCount) "Unified knowledge cache status missing listCachedKeyCount."
    Assert-True ($null -ne $unifiedCacheStatus.data.knowledge.detailCachedKeyCount) "Unified knowledge cache status missing detailCachedKeyCount."
    Assert-True ($unifiedCacheStatus.data.boundary -match "MySQL-backed") "Unified cache boundary does not mention MySQL-backed state."
    $cacheStatus = Invoke-JsonGet "$BackendUrl/api/problems/cache/status"
    Assert-ApiSuccess $cacheStatus "problem cache status"
    Assert-True ($null -ne $cacheStatus.data.enabled) "Problem cache status missing enabled."
    Assert-True ($cacheStatus.data.provider -eq "Redis") "Problem cache provider is not Redis."
    Assert-True ($null -ne $cacheStatus.data.redisAvailable) "Problem cache status missing redisAvailable."
    Assert-True ($null -ne $cacheStatus.data.statusLabel) "Problem cache status missing statusLabel."
    Assert-True ($null -ne $cacheStatus.data.summary) "Problem cache status missing readable summary."
    Assert-True ($null -ne $cacheStatus.data.checkedAt) "Problem cache status missing checkedAt."
    Assert-True ($cacheStatus.data.maintenanceAction -match "/api/problems/cache/refresh") "Problem cache status missing maintenanceAction."
    Assert-True ($null -ne $cacheStatus.data.cachedKeyCount) "Problem cache status missing cachedKeyCount."
    Assert-True ($null -ne $cacheStatus.data.listTtlSeconds) "Problem cache status missing listTtlSeconds."
    Assert-True ($null -ne $cacheStatus.data.detailTtlSeconds) "Problem cache status missing detailTtlSeconds."
    Assert-True ($null -ne $cacheStatus.data.templateTtlSeconds) "Problem cache status missing templateTtlSeconds."
    Assert-True ($cacheStatus.data.fallback -match "MySQL") "Problem cache status missing fallback boundary."
    Assert-True ($null -ne $cacheStatus.data.hitCount) "Problem cache status missing hitCount."
    Assert-True ($null -ne $cacheStatus.data.missCount) "Problem cache status missing missCount."
    Assert-True ($null -ne $cacheStatus.data.fallbackCount) "Problem cache status missing fallbackCount."
    Assert-True ($null -ne $cacheStatus.data.hitRate) "Problem cache status missing hitRate."
    Assert-True ($cacheStatus.data.PSObject.Properties.Name -contains "lastFallbackReason") "Problem cache status missing lastFallbackReason."
    Assert-True ($cacheStatus.data.PSObject.Properties.Name -contains "probeWarning") "Problem cache status missing probeWarning."
    Assert-True ($null -ne $cacheStatus.data.listCached) "Problem cache status missing listCached."
    Assert-True ($null -ne $cacheStatus.data.detailCachedKeyCount) "Problem cache status missing detailCachedKeyCount."
    Assert-True ($null -ne $cacheStatus.data.templateCachedKeyCount) "Problem cache status missing templateCachedKeyCount."
    Assert-True ($cacheStatus.data.listKey -eq "coach:problem:list:v1") "Problem cache list key is unexpected: $($cacheStatus.data.listKey)"
    Assert-True ($cacheStatus.data.detailKeyPattern -eq "coach:problem:detail:v1:{problemId}") "Problem cache detail key pattern is unexpected: $($cacheStatus.data.detailKeyPattern)"
    Assert-True ($cacheStatus.data.templateKeyPattern -eq "coach:problem:template:v1:{problemId}") "Problem cache template key pattern is unexpected: $($cacheStatus.data.templateKeyPattern)"
    $cacheRefresh = Invoke-JsonPost -Url "$BackendUrl/api/problems/cache/refresh" -Body @{}
    Assert-ApiSuccess $cacheRefresh "problem cache refresh"
    Assert-True ($null -ne $cacheRefresh.data.listWarmAttempted) "Problem cache refresh missing listWarmAttempted."
    Assert-True ($null -ne $cacheRefresh.data.totalWarmAttemptedCount) "Problem cache refresh missing totalWarmAttemptedCount."
    Assert-True ($null -ne $cacheRefresh.data.failedCount) "Problem cache refresh missing failedCount."
    Assert-True ($null -ne $cacheRefresh.data.message) "Problem cache refresh missing message."
    Assert-True ($null -ne $cacheRefresh.data.refreshedAt) "Problem cache refresh missing refreshedAt."
    Assert-True ($null -ne $cacheRefresh.data.statusLabel) "Problem cache refresh missing statusLabel."
    Assert-True ($null -ne $cacheRefresh.data.maintenanceAction) "Problem cache refresh missing maintenanceAction."
    Assert-True ($cacheRefresh.data.summary -match "warm-up attempted|refresh skipped") "Problem cache refresh missing readable summary."
    $knowledgeCacheStatus = Invoke-JsonGet "$BackendUrl/api/knowledge/cache/status"
    Assert-ApiSuccess $knowledgeCacheStatus "knowledge cache status"
    Assert-True ($null -ne $knowledgeCacheStatus.data.enabled) "Knowledge cache status missing enabled."
    Assert-True ($knowledgeCacheStatus.data.provider -eq "Redis") "Knowledge cache provider is not Redis."
    Assert-True ($null -ne $knowledgeCacheStatus.data.redisAvailable) "Knowledge cache status missing redisAvailable."
    Assert-True ($null -ne $knowledgeCacheStatus.data.statusLabel) "Knowledge cache status missing statusLabel."
    Assert-True ($null -ne $knowledgeCacheStatus.data.summary) "Knowledge cache status missing readable summary."
    Assert-True ($null -ne $knowledgeCacheStatus.data.checkedAt) "Knowledge cache status missing checkedAt."
    Assert-True ($knowledgeCacheStatus.data.maintenanceAction -match "/api/knowledge/cache/refresh") "Knowledge cache status missing maintenanceAction."
    Assert-True ($null -ne $knowledgeCacheStatus.data.cachedKeyCount) "Knowledge cache status missing cachedKeyCount."
    Assert-True ($null -ne $knowledgeCacheStatus.data.categoryTtlSeconds) "Knowledge cache status missing categoryTtlSeconds."
    Assert-True ($null -ne $knowledgeCacheStatus.data.listTtlSeconds) "Knowledge cache status missing listTtlSeconds."
    Assert-True ($null -ne $knowledgeCacheStatus.data.detailTtlSeconds) "Knowledge cache status missing detailTtlSeconds."
    Assert-True ($knowledgeCacheStatus.data.fallback -match "MySQL") "Knowledge cache status missing fallback boundary."
    Assert-True ($null -ne $knowledgeCacheStatus.data.hitCount) "Knowledge cache status missing hitCount."
    Assert-True ($null -ne $knowledgeCacheStatus.data.missCount) "Knowledge cache status missing missCount."
    Assert-True ($null -ne $knowledgeCacheStatus.data.fallbackCount) "Knowledge cache status missing fallbackCount."
    Assert-True ($null -ne $knowledgeCacheStatus.data.hitRate) "Knowledge cache status missing hitRate."
    Assert-True ($knowledgeCacheStatus.data.PSObject.Properties.Name -contains "lastFallbackReason") "Knowledge cache status missing lastFallbackReason."
    Assert-True ($knowledgeCacheStatus.data.PSObject.Properties.Name -contains "probeWarning") "Knowledge cache status missing probeWarning."
    Assert-True ($null -ne $knowledgeCacheStatus.data.categoryCached) "Knowledge cache status missing categoryCached."
    Assert-True ($null -ne $knowledgeCacheStatus.data.listCachedKeyCount) "Knowledge cache status missing listCachedKeyCount."
    Assert-True ($null -ne $knowledgeCacheStatus.data.detailCachedKeyCount) "Knowledge cache status missing detailCachedKeyCount."
    Assert-True ($knowledgeCacheStatus.data.categoryKey -eq "coach:knowledge:categories:v1") "Knowledge cache category key is unexpected: $($knowledgeCacheStatus.data.categoryKey)"
    Assert-True ($knowledgeCacheStatus.data.listKeyPattern -eq "coach:knowledge:cards:v1:{category|ALL}") "Knowledge cache list key pattern is unexpected: $($knowledgeCacheStatus.data.listKeyPattern)"
    Assert-True ($knowledgeCacheStatus.data.detailKeyPattern -eq "coach:knowledge:card:v1:{cardId}") "Knowledge cache detail key pattern is unexpected: $($knowledgeCacheStatus.data.detailKeyPattern)"
    $knowledgeCacheRefresh = Invoke-JsonPost -Url "$BackendUrl/api/knowledge/cache/refresh" -Body @{}
    Assert-ApiSuccess $knowledgeCacheRefresh "knowledge cache refresh"
    Assert-True ($null -ne $knowledgeCacheRefresh.data.categoryWarmAttempted) "Knowledge cache refresh missing categoryWarmAttempted."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.totalWarmAttemptedCount) "Knowledge cache refresh missing totalWarmAttemptedCount."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.failedCount) "Knowledge cache refresh missing failedCount."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.message) "Knowledge cache refresh missing message."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.refreshedAt) "Knowledge cache refresh missing refreshedAt."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.statusLabel) "Knowledge cache refresh missing statusLabel."
    Assert-True ($null -ne $knowledgeCacheRefresh.data.maintenanceAction) "Knowledge cache refresh missing maintenanceAction."
    Assert-True ($knowledgeCacheRefresh.data.summary -match "warm-up attempted|refresh skipped") "Knowledge cache refresh missing readable summary."
    $unifiedCacheRefresh = Invoke-JsonPost -Url "$BackendUrl/api/cache/refresh" -Body @{}
    Assert-ApiSuccess $unifiedCacheRefresh "unified cache refresh"
    Assert-True ($null -ne $unifiedCacheRefresh.data.problem) "Unified cache refresh missing problem result."
    Assert-True ($null -ne $unifiedCacheRefresh.data.knowledge) "Unified cache refresh missing knowledge result."
    Assert-True ($null -ne $unifiedCacheRefresh.data.totalWarmAttemptedCount) "Unified cache refresh missing totalWarmAttemptedCount."
    Assert-True ($null -ne $unifiedCacheRefresh.data.failedCount) "Unified cache refresh missing failedCount."
    Assert-True ($null -ne $unifiedCacheRefresh.data.message) "Unified cache refresh missing message."
    Assert-True ($null -ne $unifiedCacheRefresh.data.refreshedAt) "Unified cache refresh missing refreshedAt."
    Assert-True ($null -ne $unifiedCacheRefresh.data.statusLabel) "Unified cache refresh missing statusLabel."
    Assert-True ($null -ne $unifiedCacheRefresh.data.maintenanceAction) "Unified cache refresh missing maintenanceAction."
    Assert-True ($null -ne $unifiedCacheRefresh.data.boundary) "Unified cache refresh missing boundary."
    Assert-True ($null -ne $unifiedCacheRefresh.data.refreshScopeSummary) "Unified cache refresh missing refreshScopeSummary."
    Assert-True ($null -ne $unifiedCacheRefresh.data.warmupResultSummary) "Unified cache refresh missing warmupResultSummary."
    Assert-True ($null -ne $unifiedCacheRefresh.data.protectedDataSummary) "Unified cache refresh missing protectedDataSummary."
    Assert-True ($unifiedCacheRefresh.data.protectedDataSummary -match "MySQL") "Unified cache refresh protectedDataSummary does not mention MySQL boundary."
    Assert-True ($unifiedCacheRefresh.data.summary -match "Cache warm-up attempted") "Unified cache refresh missing readable summary."
    Assert-True ($unifiedCacheRefresh.data.summary -match "problem:") "Unified cache refresh summary missing problem child result."
    Assert-True ($unifiedCacheRefresh.data.summary -match "knowledge:") "Unified cache refresh summary missing knowledge child result."
    $template = Invoke-JsonGet "$BackendUrl/api/problems/$ProblemId/template"
    Assert-ApiSuccess $template "problem template"
    Assert-True ($template.data.templateCode -match "class Solution") "Template did not contain class Solution."
    $Evidence["problem"] = "problem=$($problem.data.id), title=$($problem.data.title)"
    $Evidence["cache"] = "status=$($unifiedCacheStatus.data.statusLabel), allEnabled=$($unifiedCacheStatus.data.allEnabled), allRedisAvailable=$($unifiedCacheStatus.data.allRedisAvailable), cachedKeys=$($unifiedCacheStatus.data.cachedKeyCount), hitRate=$($unifiedCacheStatus.data.hitRate), hits=$($unifiedCacheStatus.data.hitCount), misses=$($unifiedCacheStatus.data.missCount), fallbacks=$($unifiedCacheStatus.data.fallbackCount), lastFallback=$($unifiedCacheStatus.data.lastFallbackReason), benefit=$($unifiedCacheStatus.data.cacheBenefitSummary), fallbackRisk=$($unifiedCacheStatus.data.fallbackRiskSummary), protected=$($unifiedCacheStatus.data.protectedDataSummary), summary=$($unifiedCacheStatus.data.summary), checkedAt=$($unifiedCacheStatus.data.checkedAt), action=$($unifiedCacheStatus.data.maintenanceAction), refreshStatus=$($unifiedCacheRefresh.data.statusLabel), refreshAction=$($unifiedCacheRefresh.data.maintenanceAction), refreshedAt=$($unifiedCacheRefresh.data.refreshedAt), refreshTotal=$($unifiedCacheRefresh.data.totalWarmAttemptedCount), failedRefresh=$($unifiedCacheRefresh.data.failedCount), refreshScope=$($unifiedCacheRefresh.data.refreshScopeSummary), refreshWarmup=$($unifiedCacheRefresh.data.warmupResultSummary), refreshProtected=$($unifiedCacheRefresh.data.protectedDataSummary), refreshMessage=$($unifiedCacheRefresh.data.message), refreshBoundary=$($unifiedCacheRefresh.data.boundary)"
    $Evidence["problem_cache"] = "status=$($cacheStatus.data.statusLabel), enabled=$($cacheStatus.data.enabled), redisAvailable=$($cacheStatus.data.redisAvailable), cachedKeys=$($cacheStatus.data.cachedKeyCount), hitRate=$($cacheStatus.data.hitRate), hits=$($cacheStatus.data.hitCount), misses=$($cacheStatus.data.missCount), fallbacks=$($cacheStatus.data.fallbackCount), lastFallback=$($cacheStatus.data.lastFallbackReason), checkedAt=$($cacheStatus.data.checkedAt), action=$($cacheStatus.data.maintenanceAction), listCached=$($cacheStatus.data.listCached), detailKeys=$($cacheStatus.data.detailCachedKeyCount), templateKeys=$($cacheStatus.data.templateCachedKeyCount), ttl=$($cacheStatus.data.listTtlSeconds)/$($cacheStatus.data.detailTtlSeconds)/$($cacheStatus.data.templateTtlSeconds), fallback=$($cacheStatus.data.fallback)"
    $Evidence["problem_cache_refresh"] = "status=$($cacheRefresh.data.statusLabel), action=$($cacheRefresh.data.maintenanceAction), refreshedAt=$($cacheRefresh.data.refreshedAt), total=$($cacheRefresh.data.totalWarmAttemptedCount), listWarmAttempted=$($cacheRefresh.data.listWarmAttempted), detailWarmAttempted=$($cacheRefresh.data.detailWarmAttemptedCount), failed=$($cacheRefresh.data.failedCount), message=$($cacheRefresh.data.message)"
    $Evidence["knowledge_cache"] = "status=$($knowledgeCacheStatus.data.statusLabel), enabled=$($knowledgeCacheStatus.data.enabled), redisAvailable=$($knowledgeCacheStatus.data.redisAvailable), cachedKeys=$($knowledgeCacheStatus.data.cachedKeyCount), hitRate=$($knowledgeCacheStatus.data.hitRate), hits=$($knowledgeCacheStatus.data.hitCount), misses=$($knowledgeCacheStatus.data.missCount), fallbacks=$($knowledgeCacheStatus.data.fallbackCount), lastFallback=$($knowledgeCacheStatus.data.lastFallbackReason), checkedAt=$($knowledgeCacheStatus.data.checkedAt), action=$($knowledgeCacheStatus.data.maintenanceAction), categoryCached=$($knowledgeCacheStatus.data.categoryCached), listKeys=$($knowledgeCacheStatus.data.listCachedKeyCount), detailKeys=$($knowledgeCacheStatus.data.detailCachedKeyCount), ttl=$($knowledgeCacheStatus.data.categoryTtlSeconds)/$($knowledgeCacheStatus.data.listTtlSeconds)/$($knowledgeCacheStatus.data.detailTtlSeconds), fallback=$($knowledgeCacheStatus.data.fallback)"
    $Evidence["knowledge_cache_refresh"] = "status=$($knowledgeCacheRefresh.data.statusLabel), action=$($knowledgeCacheRefresh.data.maintenanceAction), refreshedAt=$($knowledgeCacheRefresh.data.refreshedAt), total=$($knowledgeCacheRefresh.data.totalWarmAttemptedCount), categoryWarmAttempted=$($knowledgeCacheRefresh.data.categoryWarmAttempted), lists=$($knowledgeCacheRefresh.data.listWarmAttemptedCount), details=$($knowledgeCacheRefresh.data.detailWarmAttemptedCount), failed=$($knowledgeCacheRefresh.data.failedCount), message=$($knowledgeCacheRefresh.data.message)"

    Write-Step "Failed submission SSE flow"
    $bugPath = Join-Path $RepoRoot "docs\demo-cases\1-two-sum-bug.java"
    $bugSubmission = Submit-Code -CodePath $bugPath -ExpectedStatus "WRONG_ANSWER"
    $bugSse = Read-Sse -SubmissionId $bugSubmission.submissionId
    Assert-SseSuccessSteps -SseText $bugSse -ExpectedSteps @(
        "PLANNING",
        "CODE_EXECUTION",
        "OBSERVATION",
        "RAG_RETRIEVAL",
        "ERROR_CLASSIFICATION",
        "MEMORY_UPDATE",
        "TRAINING_PLAN",
        "COMPLETED"
    )
    Assert-True ($bugSse -match '"trainingPlanTitle":') "Failed submission did not return trainingPlanTitle."
    $Evidence["failed_submission"] = "submissionId=$($bugSubmission.submissionId), status=$($bugSubmission.status), passed=$($bugSubmission.passedCount)/$($bugSubmission.totalCount)"

    Write-Step "Accepted submission SSE flow"
    $fixedPath = Join-Path $RepoRoot "docs\demo-cases\1-two-sum-fixed.java"
    $acSubmission = Submit-Code -CodePath $fixedPath -ExpectedStatus "ACCEPTED"
    $acSse = Read-Sse -SubmissionId $acSubmission.submissionId
    Assert-SseSuccessSteps -SseText $acSse -ExpectedSteps @(
        "PLANNING",
        "CODE_EXECUTION",
        "OBSERVATION",
        "RAG_RETRIEVAL",
        "CODE_REVIEW",
        "COMPLETED"
    )
    Assert-True ($acSse -match '"codeReview":\{') "Accepted submission did not return codeReview."
    $Evidence["accepted_submission"] = "submissionId=$($acSubmission.submissionId), status=$($acSubmission.status), passed=$($acSubmission.passedCount)/$($acSubmission.totalCount)"

    Write-Step "Dashboard, RAG chat, and persistence checks"
    $latestPlan = Invoke-JsonGet "$BackendUrl/api/users/$UserId/training-plans/latest"
    Assert-ApiSuccess $latestPlan "latest training plan"
    Assert-True ($null -ne $latestPlan.data.id) "Latest training plan missing id."
    Assert-True ($null -ne $latestPlan.data.title) "Latest training plan missing title."
    Assert-True ($null -ne $latestPlan.data.summary) "Latest training plan missing summary."
    Assert-True ($null -ne $latestPlan.data.status) "Latest training plan missing status."
    Assert-True ($null -ne $latestPlan.data.statusLabel) "Latest training plan missing statusLabel."
    Assert-True ($latestPlan.data.items.Count -gt 0) "Latest training plan has no items."
    Assert-True ($null -ne $latestPlan.data.items[0].id) "Latest training plan first item missing id."
    Assert-True ($null -ne $latestPlan.data.items[0].itemType) "Latest training plan first item missing itemType."
    Assert-True ($null -ne $latestPlan.data.items[0].dayIndex) "Latest training plan first item missing dayIndex."
    Assert-True ($null -ne $latestPlan.data.items[0].knowledgePoint) "Latest training plan first item missing knowledgePoint."
    Assert-True ($null -ne $latestPlan.data.items[0].reason) "Latest training plan first item missing reason."
    Assert-True ($null -ne $latestPlan.data.items[0].reviewFocus) "Latest training plan first item missing reviewFocus."
    Assert-True ($null -ne $latestPlan.data.items[0].sourceType) "Latest training plan first item missing sourceType."
    Assert-True ($null -ne $latestPlan.data.items[0].sourceSummary) "Latest training plan first item missing sourceSummary."
    Assert-True ($null -ne $latestPlan.data.items[0].targetHref) "Latest training plan first item missing targetHref."
    Assert-True ($null -ne $latestPlan.data.items[0].targetLabel) "Latest training plan first item missing targetLabel."
    Assert-True ($null -ne $latestPlan.data.items[0].status) "Latest training plan first item missing status."
    $trainingPlanHistory = Invoke-JsonGet "$BackendUrl/api/users/$UserId/training-plans/history?limit=5"
    Assert-ApiSuccess $trainingPlanHistory "training plan history"
    if ($trainingPlanHistory.data.Count -gt 0) {
        Assert-True ($null -ne $trainingPlanHistory.data[0].id) "Training plan history missing id."
        Assert-True ($null -ne $trainingPlanHistory.data[0].title) "Training plan history missing title."
        Assert-True ($null -ne $trainingPlanHistory.data[0].status) "Training plan history missing status."
        Assert-True ($null -ne $trainingPlanHistory.data[0].statusLabel) "Training plan history missing statusLabel."
        Assert-True ($null -ne $trainingPlanHistory.data[0].startDate) "Training plan history missing startDate."
        Assert-True ($null -ne $trainingPlanHistory.data[0].endDate) "Training plan history missing endDate."
        Assert-True ($null -ne $trainingPlanHistory.data[0].itemCount) "Training plan history missing itemCount."
        Assert-True ($null -ne $trainingPlanHistory.data[0].completedCount) "Training plan history missing completedCount."
        Assert-True ($null -ne $trainingPlanHistory.data[0].skippedCount) "Training plan history missing skippedCount."
        Assert-True ($null -ne $trainingPlanHistory.data[0].pendingCount) "Training plan history missing pendingCount."
        Assert-True ($null -ne $trainingPlanHistory.data[0].handledCount) "Training plan history missing handledCount."
        Assert-True ($null -ne $trainingPlanHistory.data[0].completionRate) "Training plan history missing completionRate."
        Assert-True ($null -ne $trainingPlanHistory.data[0].handledRate) "Training plan history missing handledRate."
        Assert-True ($null -ne $trainingPlanHistory.data[0].createdAt) "Training plan history missing createdAt."
        $trainingPlanHistorySummary = "count=$($trainingPlanHistory.data.Count), latestId=$($trainingPlanHistory.data[0].id), status=$($trainingPlanHistory.data[0].statusLabel), window=$($trainingPlanHistory.data[0].startDate)->$($trainingPlanHistory.data[0].endDate), items=$($trainingPlanHistory.data[0].itemCount), completed=$($trainingPlanHistory.data[0].completedCount), skipped=$($trainingPlanHistory.data[0].skippedCount), latestCompletionRate=$($trainingPlanHistory.data[0].completionRate), latestHandledRate=$($trainingPlanHistory.data[0].handledRate), latestPending=$($trainingPlanHistory.data[0].pendingCount), createdAt=$($trainingPlanHistory.data[0].createdAt)"
    } else {
        $trainingPlanHistorySummary = "count=0"
    }
    $planTrace = Invoke-JsonGet "$BackendUrl/api/users/$UserId/training-plans/trace"
    Assert-ApiSuccess $planTrace "training plan trace"
    Assert-True ($planTrace.data.itemCount -gt 0) "Training plan trace has no items."
    Assert-True ($null -ne $planTrace.data.planCreatedAt) "Training plan trace missing planCreatedAt."
    Assert-True ($null -ne $planTrace.data.statusLabel) "Training plan trace missing statusLabel."
    Assert-True ($null -ne $planTrace.data.startDate) "Training plan trace missing startDate."
    Assert-True ($null -ne $planTrace.data.endDate) "Training plan trace missing endDate."
    Assert-True ($null -ne $planTrace.data.daysSinceCreated) "Training plan trace missing daysSinceCreated."
    Assert-True ($null -ne $planTrace.data.daysRemaining) "Training plan trace missing daysRemaining."
    Assert-True ($null -ne $planTrace.data.overdue) "Training plan trace missing overdue."
    Assert-True ($null -ne $planTrace.data.pendingCount) "Training plan trace missing pendingCount."
    Assert-True ($null -ne $planTrace.data.completedCount) "Training plan trace missing completedCount."
    Assert-True ($null -ne $planTrace.data.skippedCount) "Training plan trace missing skippedCount."
    Assert-True ($null -ne $planTrace.data.handledCount) "Training plan trace missing handledCount."
    Assert-True ($null -ne $planTrace.data.completionRate) "Training plan trace missing completionRate."
    Assert-True ($null -ne $planTrace.data.handledRate) "Training plan trace missing handledRate."
    Assert-True ($null -ne $planTrace.data.sourceTypeCounts) "Training plan trace has no sourceTypeCounts."
    Assert-True ($null -ne $planTrace.data.progressSummary) "Training plan trace missing progressSummary."
    Assert-True ($null -ne $planTrace.data.sourceTypeSummary) "Training plan trace missing sourceTypeSummary."
    Assert-True ($null -ne $planTrace.data.latestActivitySummary) "Training plan trace missing latestActivitySummary."
    Assert-True ($planTrace.data.PSObject.Properties.Name -contains "latestActivityAt") "Training plan trace missing latestActivityAt."
    Assert-True ($null -ne $planTrace.data.nextAction) "Training plan trace missing nextAction."
    Assert-True ($null -ne $planTrace.data.nextActionReason) "Training plan trace missing nextActionReason."
    Assert-True ($planTrace.data.nextActionPriority -match "HIGH|MEDIUM") "Training plan trace missing nextActionPriority."
    Assert-True ($null -ne $planTrace.data.nextTargetHref) "Training plan trace missing nextTargetHref."
    Assert-True ($null -ne $planTrace.data.nextTargetLabel) "Training plan trace missing nextTargetLabel."
    if ($null -ne $planTrace.data.nextItem) {
        Assert-True ($null -ne $planTrace.data.nextItem.targetHref) "Training plan next item missing targetHref."
        Assert-True ($null -ne $planTrace.data.nextItem.targetLabel) "Training plan next item missing targetLabel."
        Assert-True ($null -ne $planTrace.data.nextItem.sourceType) "Training plan next item missing sourceType."
        Assert-True ($null -ne $planTrace.data.nextItem.reason) "Training plan next item missing reason."
    }
    $trainingActivities = Invoke-JsonGet "$BackendUrl/api/users/$UserId/training-plans/activities/recent?limit=5"
    Assert-ApiSuccess $trainingActivities "recent training activities"
    if ($trainingActivities.data.Count -gt 0) {
        Assert-True ($null -ne $trainingActivities.data[0].itemId) "Recent training activity missing itemId."
        Assert-True ($null -ne $trainingActivities.data[0].planId) "Recent training activity missing planId."
        Assert-True ($null -ne $trainingActivities.data[0].planTitle) "Recent training activity missing planTitle."
        Assert-True ($null -ne $trainingActivities.data[0].itemType) "Recent training activity missing itemType."
        Assert-True ($null -ne $trainingActivities.data[0].taskTitle) "Recent training activity missing taskTitle."
        Assert-True ($null -ne $trainingActivities.data[0].knowledgePoint) "Recent training activity missing knowledgePoint."
        Assert-True ($null -ne $trainingActivities.data[0].learningImpactSummary) "Recent training activity missing learningImpactSummary."
        Assert-True ($null -ne $trainingActivities.data[0].sourceType) "Recent training activity missing sourceType."
        Assert-True ($null -ne $trainingActivities.data[0].sourceSummary) "Recent training activity missing sourceSummary."
        Assert-True ($null -ne $trainingActivities.data[0].status) "Recent training activity missing status."
        Assert-True ($null -ne $trainingActivities.data[0].statusLabel) "Recent training activity missing statusLabel."
        Assert-True ($null -ne $trainingActivities.data[0].statusUpdatedAt) "Recent training activity missing statusUpdatedAt."
        $trainingActivitySummary = "count=$($trainingActivities.data.Count), latestItem=$($trainingActivities.data[0].itemId), plan=$($trainingActivities.data[0].planId), type=$($trainingActivities.data[0].itemType), status=$($trainingActivities.data[0].statusLabel), source=$($trainingActivities.data[0].sourceType), updatedAt=$($trainingActivities.data[0].statusUpdatedAt)"
    } else {
        $trainingActivitySummary = "count=0"
    }
    $mockTrace = Invoke-JsonGet "$BackendUrl/api/users/$UserId/mock-interviews/trace"
    Assert-ApiSuccess $mockTrace "mock interview trace"
    Assert-True ($null -ne $mockTrace.data.sessionCount) "Mock interview trace missing sessionCount."
    Assert-True ($null -ne $mockTrace.data.reportedSessionCount) "Mock interview trace missing reportedSessionCount."
    Assert-True ($null -ne $mockTrace.data.answeredTurnCount) "Mock interview trace missing answeredTurnCount."
    Assert-True ($null -ne $mockTrace.data.lowScoreTurnCount) "Mock interview trace missing lowScoreTurnCount."
    Assert-True ($null -ne $mockTrace.data.weaknessEventCount) "Mock interview trace missing weaknessEventCount."
    Assert-True ($null -ne $mockTrace.data.trainingPlanItemCount) "Mock interview trace missing trainingPlanItemCount."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "latestWeaknessTags") "Mock interview trace missing latestWeaknessTags."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "latestInterviewAt") "Mock interview trace missing latestInterviewAt."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "latestSessionStatusLabel") "Mock interview trace missing latestSessionStatusLabel."
    Assert-True ($null -ne $mockTrace.data.closureStatus) "Mock interview trace missing closureStatus."
    Assert-True ($null -ne $mockTrace.data.closureStatusLabel) "Mock interview trace missing closureStatusLabel."
    Assert-True ($null -ne $mockTrace.data.nextActionReason) "Mock interview trace missing nextActionReason."
    Assert-True ($mockTrace.data.nextActionPriority -match "HIGH|MEDIUM") "Mock interview trace missing nextActionPriority."
    Assert-True ($null -ne $mockTrace.data.nextTargetHref) "Mock interview trace missing nextTargetHref."
    Assert-True ($null -ne $mockTrace.data.nextTargetLabel) "Mock interview trace missing nextTargetLabel."
    Assert-True ($null -ne $mockTrace.data.closureSummary) "Mock interview trace missing closureSummary."
    Assert-True ($null -ne $mockTrace.data.reviewPathSummary) "Mock interview trace missing reviewPathSummary."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "recommendedCardIds") "Mock interview trace missing recommendedCardIds."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "reportTrainingPlanLinked") "Mock interview trace missing reportTrainingPlanLinked."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "reportReviewHref") "Mock interview trace missing reportReviewHref."
    Assert-True ($mockTrace.data.PSObject.Properties.Name -contains "reportReviewLabel") "Mock interview trace missing reportReviewLabel."
    $mockReportSummary = "none"
    if ($null -ne $mockTrace.data.latestSessionId) {
        Assert-True ($null -ne $mockTrace.data.latestCategory) "Mock interview trace missing latestCategory for latest session."
        $mockSession = Invoke-JsonGet "$BackendUrl/api/mock-interviews/$($mockTrace.data.latestSessionId)"
        Assert-ApiSuccess $mockSession "mock interview session detail"
        if ($null -ne $mockSession.data.report) {
            Assert-True ($mockSession.data.report.PSObject.Properties.Name -contains "trainingPlanLinked") "Mock interview report missing trainingPlanLinked."
            Assert-True ($null -ne $mockSession.data.report.trainingPlanItemCount) "Mock interview report missing trainingPlanItemCount."
            Assert-True ($null -ne $mockSession.data.report.reviewPathSummary) "Mock interview report missing reviewPathSummary."
            $mockReportSummary = "reportId=$($mockSession.data.report.id), trainingLinked=$($mockSession.data.report.trainingPlanLinked), trainingItems=$($mockSession.data.report.trainingPlanItemCount), reviewPath=$($mockSession.data.report.reviewPathSummary)"
        }
    }
    $mockTrends = Invoke-JsonGet "$BackendUrl/api/users/$UserId/mock-interviews/trends?limit=5"
    Assert-ApiSuccess $mockTrends "mock interview trends"
    if ($mockTrends.data.Count -gt 0) {
        Assert-True ($null -ne $mockTrends.data[0].knowledgeCardId) "Mock interview trend missing knowledgeCardId."
        Assert-True ($null -ne $mockTrends.data[0].knowledgePoint) "Mock interview trend missing knowledgePoint."
        Assert-True ($null -ne $mockTrends.data[0].category) "Mock interview trend missing category."
        Assert-True ($null -ne $mockTrends.data[0].latestSessionId) "Mock interview trend missing latestSessionId."
        Assert-True ($null -ne $mockTrends.data[0].latestScore) "Mock interview trend missing latestScore."
        Assert-True ($mockTrends.data[0].PSObject.Properties.Name -contains "previousScore") "Mock interview trend missing previousScore."
        Assert-True ($mockTrends.data[0].PSObject.Properties.Name -contains "deltaScore") "Mock interview trend missing deltaScore."
        Assert-True ($null -ne $mockTrends.data[0].trendLabel) "Mock interview trend missing trendLabel."
        Assert-True ($null -ne $mockTrends.data[0].interviewCount) "Mock interview trend missing interviewCount."
        Assert-True ($null -ne $mockTrends.data[0].lastInterviewAt) "Mock interview trend missing lastInterviewAt."
        Assert-True ($null -ne $mockTrends.data[0].latestIssue) "Mock interview trend missing latestIssue."
        Assert-True ($null -ne $mockTrends.data[0].latestIssueType) "Mock interview trend missing latestIssueType."
        Assert-True ($null -ne $mockTrends.data[0].latestIssueTypeLabel) "Mock interview trend missing latestIssueTypeLabel."
        $mockTrendSummary = "count=$($mockTrends.data.Count), cardId=$($mockTrends.data[0].knowledgeCardId), point=$($mockTrends.data[0].knowledgePoint), category=$($mockTrends.data[0].category), latestSession=$($mockTrends.data[0].latestSessionId), latestScore=$($mockTrends.data[0].latestScore), previousScore=$($mockTrends.data[0].previousScore), deltaScore=$($mockTrends.data[0].deltaScore), label=$($mockTrends.data[0].trendLabel), interviews=$($mockTrends.data[0].interviewCount), issueType=$($mockTrends.data[0].latestIssueType)"
    } else {
        $mockTrendSummary = "count=0"
    }
    $dashboard = Invoke-JsonGet "$BackendUrl/api/users/$UserId/dashboard/stats"
    Assert-ApiSuccess $dashboard "dashboard stats"
    $ragHealth = Invoke-JsonGet "$BackendUrl/api/rag/health"
    Assert-ApiSuccess $ragHealth "rag health"
    Assert-True ($ragHealth.data.tablesAvailable -eq $true) "RAG health reports missing tables."
    Assert-True ($ragHealth.data.systemChunkCount -gt 0) "RAG health reports empty system chunk index."
    Assert-True ($null -ne $ragHealth.data.vectorIndexedChunkCount) "RAG health missing vectorIndexedChunkCount."
    Assert-True ($null -ne $ragHealth.data.vectorFailedChunkCount) "RAG health missing vectorFailedChunkCount."
    Assert-True ($null -ne $ragHealth.data.vectorPendingChunkCount) "RAG health missing vectorPendingChunkCount."
    Assert-True ($null -ne $ragHealth.data.maintenanceActions) "RAG health missing maintenanceActions."
    Assert-True ($null -ne $ragHealth.data.statusLabel) "RAG health missing statusLabel."
    Assert-True ($null -ne $ragHealth.data.maintenanceSummary) "RAG health missing maintenanceSummary."
    Assert-True ($null -ne $ragHealth.data.maintenancePriority) "RAG health missing maintenancePriority."
    Assert-True ($null -ne $ragHealth.data.maintenanceReason) "RAG health missing maintenanceReason."
    Assert-True ($null -ne $ragHealth.data.checkedAt) "RAG health missing checkedAt."
    Assert-True ($null -ne $ragHealth.data.enabledProblemCount) "RAG health missing enabledProblemCount."
    Assert-True ($null -ne $ragHealth.data.enabledKnowledgeCardCount) "RAG health missing enabledKnowledgeCardCount."
    Assert-True ($null -ne $ragHealth.data.userMemoryDocumentCount) "RAG health missing userMemoryDocumentCount."
    Assert-True ($null -ne $ragHealth.data.userMemoryChunkCount) "RAG health missing userMemoryChunkCount."
    Assert-True ($null -ne $ragHealth.data.duplicateSystemDocumentCount) "RAG health missing duplicateSystemDocumentCount."
    Assert-True ($null -ne $ragHealth.data.vectorEnabled) "RAG health missing vectorEnabled."
    Assert-True ($null -ne $ragHealth.data.warnings) "RAG health missing warnings."
    Assert-True ($null -ne $ragHealth.data.missingSystemProblemDocumentCount) "RAG health missing missingSystemProblemDocumentCount."
    Assert-True ($null -ne $ragHealth.data.missingSystemKnowledgeCardDocumentCount) "RAG health missing missingSystemKnowledgeCardDocumentCount."
    Assert-True ($null -ne $ragHealth.data.staleProblemDocumentCount) "RAG health missing staleProblemDocumentCount."
    Assert-True ($null -ne $ragHealth.data.staleKnowledgeCardDocumentCount) "RAG health missing staleKnowledgeCardDocumentCount."
    Assert-True ($null -ne $ragHealth.data.documentSourceTypeCounts) "RAG health missing documentSourceTypeCounts."
    Assert-True ($null -ne $ragHealth.data.chunkSourceTypeCounts) "RAG health missing chunkSourceTypeCounts."
    Assert-True ($null -ne $ragHealth.data.preferredMaintenanceAction) "RAG health missing preferredMaintenanceAction."
    Assert-True (($null -eq $ragHealth.data.nextMaintenanceEndpoint) -or ($ragHealth.data.nextMaintenanceEndpoint -match "/api/rag/")) "RAG health nextMaintenanceEndpoint is not a RAG maintenance path."
    if ($RunRagRebuild) {
        $ragRebuild = Invoke-JsonPost -Url "$BackendUrl/api/rag/system-index/rebuild" -Body @{}
        Assert-ApiSuccess $ragRebuild "rag system index rebuild"
        Assert-True ($ragRebuild.data.attempted -eq $true) "RAG system index rebuild was not attempted."
        Assert-True ($null -ne $ragRebuild.data.success) "RAG system index rebuild missing success."
        Assert-True ($null -ne $ragRebuild.data.vectorEnabled) "RAG system index rebuild missing vectorEnabled."
        Assert-True ($null -ne $ragRebuild.data.beforeSystemDocumentCount) "RAG system index rebuild missing beforeSystemDocumentCount."
        Assert-True ($null -ne $ragRebuild.data.afterSystemDocumentCount) "RAG system index rebuild missing afterSystemDocumentCount."
        Assert-True ($null -ne $ragRebuild.data.afterSystemChunkCount) "RAG system index rebuild missing afterSystemChunkCount."
        Assert-True ($null -ne $ragRebuild.data.beforeUserMemoryChunkCount) "RAG system index rebuild missing beforeUserMemoryChunkCount."
        Assert-True ($null -ne $ragRebuild.data.afterUserMemoryChunkCount) "RAG system index rebuild missing afterUserMemoryChunkCount."
        Assert-True ($null -ne $ragRebuild.data.warnings) "RAG system index rebuild missing warnings."
        Assert-True ($null -ne $ragRebuild.data.message) "RAG system index rebuild missing message."
        Assert-True ($null -ne $ragRebuild.data.rebuiltAt) "RAG system index rebuild missing rebuiltAt."
        Assert-True ($null -ne $ragRebuild.data.statusLabel) "RAG system index rebuild missing statusLabel."
        Assert-True ($null -ne $ragRebuild.data.maintenanceAction) "RAG system index rebuild missing maintenanceAction."
        Assert-True ($null -ne $ragRebuild.data.boundary) "RAG system index rebuild missing boundary."
        Assert-True ($ragRebuild.data.boundary -match "does not delete user memory") "RAG system index rebuild boundary does not mention user memory preservation."
        Assert-True ($ragRebuild.data.afterUserMemoryDocumentCount -ge $ragRebuild.data.beforeUserMemoryDocumentCount) "RAG rebuild appears to have removed user memory documents."
        Assert-True ($ragRebuild.data.afterUserMemoryChunkCount -ge $ragRebuild.data.beforeUserMemoryChunkCount) "RAG rebuild appears to have removed user memory chunks."
        Assert-True ($ragRebuild.data.summary -match "System RAG rebuild summary") "RAG system index rebuild missing readable summary."
        $Evidence["rag_system_rebuild"] = "status=$($ragRebuild.data.statusLabel), success=$($ragRebuild.data.success), vectorEnabled=$($ragRebuild.data.vectorEnabled), action=$($ragRebuild.data.maintenanceAction), rebuiltAt=$($ragRebuild.data.rebuiltAt), message=$($ragRebuild.data.message), boundary=$($ragRebuild.data.boundary), summary=$($ragRebuild.data.summary), indexedProblems=$($ragRebuild.data.indexedProblemCount), indexedCards=$($ragRebuild.data.indexedKnowledgeCardCount), systemDocuments=$($ragRebuild.data.beforeSystemDocumentCount)->$($ragRebuild.data.afterSystemDocumentCount), systemChunks=$($ragRebuild.data.beforeSystemChunkCount)->$($ragRebuild.data.afterSystemChunkCount), userMemoryDocuments=$($ragRebuild.data.beforeUserMemoryDocumentCount)->$($ragRebuild.data.afterUserMemoryDocumentCount), userMemoryChunks=$($ragRebuild.data.beforeUserMemoryChunkCount)->$($ragRebuild.data.afterUserMemoryChunkCount), warnings=$($ragRebuild.data.warnings.Count)"
    } else {
        $Evidence["rag_system_rebuild"] = "skipped; pass -RunRagRebuild to rebuild system problem/knowledge_card index"
    }
    $ragVectorRetry = Invoke-JsonPost -Url "$BackendUrl/api/rag/vector/retry-failed?limit=50" -Body @{}
    Assert-ApiSuccess $ragVectorRetry "rag vector retry"
    Assert-True ($null -ne $ragVectorRetry.data.enabled) "RAG vector retry missing enabled flag."
    Assert-True ($null -ne $ragVectorRetry.data.requestedLimit) "RAG vector retry missing requestedLimit."
    Assert-True ($null -ne $ragVectorRetry.data.effectiveLimit) "RAG vector retry missing effectiveLimit."
    Assert-True ($null -ne $ragVectorRetry.data.retriedAt) "RAG vector retry missing retriedAt."
    Assert-True ($null -ne $ragVectorRetry.data.statusLabel) "RAG vector retry missing statusLabel."
    Assert-True ($null -ne $ragVectorRetry.data.maintenanceAction) "RAG vector retry missing maintenanceAction."
    Assert-True ($null -ne $ragVectorRetry.data.attemptedCount) "RAG vector retry missing attemptedCount."
    Assert-True ($null -ne $ragVectorRetry.data.matchedRetryableCount) "RAG vector retry missing matchedRetryableCount."
    Assert-True ($null -ne $ragVectorRetry.data.indexedCount) "RAG vector retry missing indexedCount."
    Assert-True ($null -ne $ragVectorRetry.data.failedCount) "RAG vector retry missing failedCount."
    Assert-True ($null -ne $ragVectorRetry.data.skippedCount) "RAG vector retry missing skippedCount."
    Assert-True ($null -ne $ragVectorRetry.data.message) "RAG vector retry missing message."
    Assert-True ($ragVectorRetry.data.summary -match "Vector retry summary|Vector RAG disabled") "RAG vector retry missing readable summary."
    $chat = Invoke-JsonPost -Url "$BackendUrl/api/rag/chat" -Body @{
        userId = $UserId
        question = "Why should Two Sum check the HashMap before inserting the current element?"
    }
    Assert-ApiSuccess $chat "rag chat"
    Assert-True ($chat.data.sources.Count -gt 0) "RAG chat returned no sources."
    $Evidence["latest_plan"] = "planId=$($latestPlan.data.id), title=$($latestPlan.data.title), status=$($latestPlan.data.statusLabel), items=$($latestPlan.data.items.Count), firstItem=$($latestPlan.data.items[0].id), firstType=$($latestPlan.data.items[0].itemType), firstDay=$($latestPlan.data.items[0].dayIndex), firstSource=$($latestPlan.data.items[0].sourceType), firstTarget=$($latestPlan.data.items[0].targetHref)"
    $Evidence["training_plan_history"] = $trainingPlanHistorySummary
    $Evidence["training_plan_trace"] = "planId=$($planTrace.data.planId), status=$($planTrace.data.statusLabel), window=$($planTrace.data.startDate)->$($planTrace.data.endDate), completionRate=$($planTrace.data.completionRate), handledRate=$($planTrace.data.handledRate), pending=$($planTrace.data.pendingCount), completed=$($planTrace.data.completedCount), skipped=$($planTrace.data.skippedCount), handled=$($planTrace.data.handledCount), daysRemaining=$($planTrace.data.daysRemaining), overdue=$($planTrace.data.overdue), nextAction=$($planTrace.data.nextAction), progress=$($planTrace.data.progressSummary), sources=$($planTrace.data.sourceTypeSummary), latestActivity=$($planTrace.data.latestActivitySummary), latestActivityAt=$($planTrace.data.latestActivityAt), activities=$($trainingActivities.data.Count)"
    $Evidence["training_activity"] = $trainingActivitySummary
    $Evidence["mock_interview_trace"] = "status=$($mockTrace.data.closureStatus), label=$($mockTrace.data.closureStatusLabel), sessionStatus=$($mockTrace.data.latestSessionStatusLabel), sessions=$($mockTrace.data.sessionCount), reported=$($mockTrace.data.reportedSessionCount), answeredTurns=$($mockTrace.data.answeredTurnCount), lowScoreTurns=$($mockTrace.data.lowScoreTurnCount), weaknessEvents=$($mockTrace.data.weaknessEventCount), trainingPlanItems=$($mockTrace.data.trainingPlanItemCount), latestInterviewAt=$($mockTrace.data.latestInterviewAt), latestWeaknessTags=$($mockTrace.data.latestWeaknessTags.Count), recommendedCards=$($mockTrace.data.recommendedCardIds.Count), trainingLinked=$($mockTrace.data.reportTrainingPlanLinked), reportReview=$($mockTrace.data.reportReviewHref)"
    $Evidence["mock_interview_report"] = $mockReportSummary
    $Evidence["mock_interview_trends"] = $mockTrendSummary
    $Evidence["dashboard"] = "submissions=$($dashboard.data.totalSubmissions), mistakes=$($dashboard.data.mistakeCount)"
    $Evidence["rag_health"] = "healthy=$($ragHealth.data.healthy), status=$($ragHealth.data.statusLabel), priority=$($ragHealth.data.maintenancePriority), reason=$($ragHealth.data.maintenanceReason), summary=$($ragHealth.data.maintenanceSummary), checkedAt=$($ragHealth.data.checkedAt), enabledProblems=$($ragHealth.data.enabledProblemCount), enabledKnowledgeCards=$($ragHealth.data.enabledKnowledgeCardCount), userMemoryDocuments=$($ragHealth.data.userMemoryDocumentCount), userMemoryChunks=$($ragHealth.data.userMemoryChunkCount), duplicateSystemDocuments=$($ragHealth.data.duplicateSystemDocumentCount), missingProblems=$($ragHealth.data.missingSystemProblemDocumentCount), missingKnowledgeCards=$($ragHealth.data.missingSystemKnowledgeCardDocumentCount), staleProblems=$($ragHealth.data.staleProblemDocumentCount), staleKnowledgeCards=$($ragHealth.data.staleKnowledgeCardDocumentCount), systemChunks=$($ragHealth.data.systemChunkCount), vectorEnabled=$($ragHealth.data.vectorEnabled), vectorIndexed=$($ragHealth.data.vectorIndexedChunkCount), vectorFailed=$($ragHealth.data.vectorFailedChunkCount), vectorPending=$($ragHealth.data.vectorPendingChunkCount), preferredAction=$($ragHealth.data.preferredMaintenanceAction), nextEndpoint=$($ragHealth.data.nextMaintenanceEndpoint), warnings=$($ragHealth.data.warnings.Count)"
    $Evidence["rag_vector_retry"] = "enabled=$($ragVectorRetry.data.enabled), status=$($ragVectorRetry.data.statusLabel), action=$($ragVectorRetry.data.maintenanceAction), retriedAt=$($ragVectorRetry.data.retriedAt), requestedLimit=$($ragVectorRetry.data.requestedLimit), effectiveLimit=$($ragVectorRetry.data.effectiveLimit), message=$($ragVectorRetry.data.message), summary=$($ragVectorRetry.data.summary), attempted=$($ragVectorRetry.data.attemptedCount), matched=$($ragVectorRetry.data.matchedRetryableCount), indexed=$($ragVectorRetry.data.indexedCount), failed=$($ragVectorRetry.data.failedCount), skipped=$($ragVectorRetry.data.skippedCount)"
    $Evidence["rag_chat"] = "sources=$($chat.data.sources.Count)"

    if (-not $SkipMysql) {
        $indexed = Invoke-MysqlScalar "SELECT COUNT(*) FROM rag_chunk WHERE vector_status='INDEXED';"
        $activePlan = Invoke-MysqlScalar "SELECT CONCAT(id, ':', status) FROM training_plan WHERE user_id=$UserId ORDER BY id DESC LIMIT 1;"
        $indexedValue = [int] (@($indexed) | Select-Object -First 1)
        $activePlanValue = [string] (@($activePlan) | Select-Object -First 1)
        Assert-True ($indexedValue -gt 0) "No INDEXED vector chunks found in MySQL."
        Assert-True ($activePlanValue -match "ACTIVE") "Latest training plan is not ACTIVE: $activePlanValue"
        $Evidence["mysql"] = "indexedVectorChunks=$indexedValue; latestPlan=$activePlanValue"
    }

    $collectionStatus = Get-HttpStatus "$QdrantUrl/collections/ai_study_rag_chunks"
    Assert-True ($collectionStatus -eq "200") "Qdrant collection ai_study_rag_chunks is not readable: $collectionStatus."
    $Evidence["qdrant_collection"] = "ai_study_rag_chunks => $collectionStatus"
    $Evidence["goal_coverage"] = "training=$($Evidence["training_plan_trace"]); rag=$($Evidence["rag_health"]); mockInterview=$($Evidence["mock_interview_trace"]); cache=$($Evidence["cache"])"

    Write-Step "E2E demo smoke passed"
    foreach ($item in $Evidence.GetEnumerator()) {
        Write-Host "$($item.Key): $($item.Value)"
    }
} finally {
    Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}

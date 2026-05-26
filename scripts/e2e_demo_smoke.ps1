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
    $problem = Invoke-JsonGet "$BackendUrl/api/problems/$ProblemId"
    Assert-ApiSuccess $problem "problem detail"
    Assert-True ($null -ne $problem.data.presetHints) "Problem detail did not include presetHints."
    $template = Invoke-JsonGet "$BackendUrl/api/problems/$ProblemId/template"
    Assert-ApiSuccess $template "problem template"
    Assert-True ($template.data.templateCode -match "class Solution") "Template did not contain class Solution."
    $Evidence["problem"] = "problem=$($problem.data.id), title=$($problem.data.title)"

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
    Assert-True ($latestPlan.data.items.Count -gt 0) "Latest training plan has no items."
    $dashboard = Invoke-JsonGet "$BackendUrl/api/users/$UserId/dashboard/stats"
    Assert-ApiSuccess $dashboard "dashboard stats"
    $chat = Invoke-JsonPost -Url "$BackendUrl/api/rag/chat" -Body @{
        userId = $UserId
        question = "Why should Two Sum check the HashMap before inserting the current element?"
    }
    Assert-ApiSuccess $chat "rag chat"
    Assert-True ($chat.data.sources.Count -gt 0) "RAG chat returned no sources."
    $Evidence["latest_plan"] = "planId=$($latestPlan.data.id), status=$($latestPlan.data.status), items=$($latestPlan.data.items.Count)"
    $Evidence["dashboard"] = "submissions=$($dashboard.data.totalSubmissions), mistakes=$($dashboard.data.mistakeCount)"
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

    Write-Step "E2E demo smoke passed"
    foreach ($item in $Evidence.GetEnumerator()) {
        Write-Host "$($item.Key): $($item.Value)"
    }
} finally {
    Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}

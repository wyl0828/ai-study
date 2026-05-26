param(
    [switch] $StartCompose
)

$ErrorActionPreference = "Stop"

$BaseUrl = "http://127.0.0.1:6333"
$Collection = "ai_study_qdrant_smoke"
$TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ai-study-qdrant-smoke-" + [System.Guid]::NewGuid())

New-Item -ItemType Directory -Path $TempDir | Out-Null

function Invoke-QdrantCurl {
    param(
        [string[]] $CurlArgs
    )

    $output = & curl.exe --noproxy "*" -sS @CurlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "curl failed with exit code $LASTEXITCODE`: $($CurlArgs -join ' ')"
    }
    return $output
}

function Invoke-QdrantJson {
    param(
        [string] $Method,
        [string] $Url,
        [string] $Body
    )

    $bodyPath = Join-Path $TempDir ([System.Guid]::NewGuid().ToString() + ".json")
    Set-Content -LiteralPath $bodyPath -Value $Body -NoNewline -Encoding ascii
    return Invoke-QdrantCurl -CurlArgs @(
        "-f",
        "-X", $Method, $Url,
        "-H", "Content-Type: application/json",
        "--data-binary", "@$bodyPath"
    )
}

try {
    if ($StartCompose) {
        docker compose up -d qdrant
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose up -d qdrant failed"
        }
    }

    $health = Invoke-QdrantCurl -CurlArgs @("$BaseUrl/healthz")
    Write-Host "health=$health"

    Invoke-QdrantCurl -CurlArgs @("-X", "DELETE", "$BaseUrl/collections/$Collection") | Out-Null

    $create = Invoke-QdrantJson `
        -Method "PUT" `
        -Url "$BaseUrl/collections/$Collection" `
        -Body '{"vectors":{"size":4,"distance":"Cosine"}}'
    Write-Host "create=$create"

    $upsert = Invoke-QdrantJson `
        -Method "PUT" `
        -Url "$BaseUrl/collections/$Collection/points?wait=true" `
        -Body '{"points":[{"id":1,"vector":[1,0,0,0],"payload":{"chunkId":1,"sourceType":"KNOWLEDGE_CARD","userId":null,"title":"smoke"}},{"id":2,"vector":[0,1,0,0],"payload":{"chunkId":2,"sourceType":"PROBLEM","userId":7,"title":"other"}}]}'
    Write-Host "upsert=$upsert"

    $query = Invoke-QdrantJson `
        -Method "POST" `
        -Url "$BaseUrl/collections/$Collection/points/query" `
        -Body '{"query":[1,0,0,0],"limit":1,"with_payload":true}'
    Write-Host "query=$query"

    if ($query -notmatch '"chunkId":1') {
        throw "Qdrant smoke query did not return chunkId=1"
    }

    $delete = Invoke-QdrantCurl -CurlArgs @("-X", "DELETE", "$BaseUrl/collections/$Collection")
    Write-Host "delete=$delete"
    Write-Host "Qdrant smoke passed."
} finally {
    Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}

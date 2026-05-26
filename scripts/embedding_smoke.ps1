param(
    [string] $InputText = "two sum hash map complement lookup",
    [int] $TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

$BaseUrl = $env:EMBEDDING_BASE_URL
$ApiKey = $env:EMBEDDING_API_KEY
$Model = $env:EMBEDDING_MODEL
$Dimensions = $env:EMBEDDING_DIMENSIONS

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "https://api.openai.com"
}
if ([string]::IsNullOrWhiteSpace($Model)) {
    $Model = "text-embedding-3-small"
}
if ([string]::IsNullOrWhiteSpace($Dimensions)) {
    $Dimensions = "1536"
}
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "EMBEDDING_API_KEY is required for embedding smoke."
}

$Uri = $BaseUrl.TrimEnd("/") + "/v1/embeddings"
$TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("ai-study-embedding-smoke-" + [System.Guid]::NewGuid())
New-Item -ItemType Directory -Path $TempDir | Out-Null

$Body = @{
    model = $Model
    input = @($InputText)
    dimensions = [int] $Dimensions
} | ConvertTo-Json -Depth 5

try {
    $bodyPath = Join-Path $TempDir "request.json"
    $responsePath = Join-Path $TempDir "response.json"
    Set-Content -LiteralPath $bodyPath -Value $Body -NoNewline -Encoding utf8

    & curl.exe -sS --max-time $TimeoutSeconds `
        -o $responsePath `
        -H "Authorization: Bearer $ApiKey" `
        -H "Content-Type: application/json" `
        --data-binary "@$bodyPath" `
        $Uri
    if ($LASTEXITCODE -ne 0) {
        throw "Embedding request failed or timed out with curl exit code $LASTEXITCODE."
    }

    $Response = Get-Content -LiteralPath $responsePath -Raw -Encoding utf8 | ConvertFrom-Json
} finally {
    Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
}

$Embedding = $Response.data[0].embedding
if ($null -eq $Embedding -or $Embedding.Count -le 0) {
    throw "Embedding response did not contain data[0].embedding."
}

Write-Host "embedding_model=$Model"
Write-Host "embedding_dimensions=$($Embedding.Count)"
Write-Host "Embedding smoke passed."

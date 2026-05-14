$ErrorActionPreference = 'Continue'

$root = 'D:\code\ai-study'
$frontend = Join-Path $root 'frontend'
$backendStart = Join-Path $root 'start-backend.ps1'
$piston = 'D:\code\piston'
$desktop = [Environment]::GetFolderPath('Desktop')
$log = Join-Path $desktop 'real-dev-memory-monitor.csv'
$summary = Join-Path $desktop 'real-dev-memory-monitor-summary.txt'
$backendLog = Join-Path $desktop 'real-dev-backend.log'
$backendErr = Join-Path $desktop 'real-dev-backend.err.log'
$frontendLog = Join-Path $desktop 'real-dev-frontend.log'
$frontendErr = Join-Path $desktop 'real-dev-frontend.err.log'

function Sum-PrivateMb($name) {
    $items = Get-Process -Name $name -ErrorAction SilentlyContinue
    if (-not $items) { return 0 }
    [math]::Round(($items | Measure-Object PrivateMemorySize64 -Sum).Sum / 1MB, 1)
}

function Sum-WorkingMb($name) {
    $items = Get-Process -Name $name -ErrorAction SilentlyContinue
    if (-not $items) { return 0 }
    [math]::Round(($items | Measure-Object WorkingSet64 -Sum).Sum / 1MB, 1)
}

function Is-PortListening($port) {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    return $null -ne $conn
}

function Wait-DockerReady {
    for ($i = 0; $i -lt 36; $i++) {
        docker info *> $null
        if ($LASTEXITCODE -eq 0) { return $true }
        Start-Sleep -Seconds 5
    }
    return $false
}

Remove-Item -LiteralPath $log, $summary -ErrorAction SilentlyContinue

"Started real dev monitor: $(Get-Date)" | Out-File $summary -Encoding utf8

$dockerExe = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
if (-not (Get-Process -Name 'Docker Desktop' -ErrorAction SilentlyContinue)) {
    if (Test-Path $dockerExe) {
        Start-Process -FilePath $dockerExe -WindowStyle Hidden
        "Started Docker Desktop" | Out-File $summary -Append -Encoding utf8
    }
}

if (Wait-DockerReady) {
    Push-Location $piston
    docker compose up -d api | Out-File $summary -Append -Encoding utf8
    Pop-Location
}
else {
    "Docker did not become ready in time" | Out-File $summary -Append -Encoding utf8
}

if (-not (Is-PortListening 8080)) {
    Start-Process -FilePath powershell.exe `
        -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$backendStart`"" `
        -WorkingDirectory $root `
        -RedirectStandardOutput $backendLog `
        -RedirectStandardError $backendErr `
        -WindowStyle Hidden
    "Started backend" | Out-File $summary -Append -Encoding utf8
}
else {
    "Backend already listening on 8080" | Out-File $summary -Append -Encoding utf8
}

if (-not (Is-PortListening 3000)) {
    Start-Process -FilePath powershell.exe `
        -ArgumentList "-NoProfile -Command `"npm run dev -- -p 3000`"" `
        -WorkingDirectory $frontend `
        -RedirectStandardOutput $frontendLog `
        -RedirectStandardError $frontendErr `
        -WindowStyle Hidden
    "Started frontend" | Out-File $summary -Append -Encoding utf8
}
else {
    "Frontend already listening on 3000" | Out-File $summary -Append -Encoding utf8
}

Start-Sleep -Seconds 35

'Time,UsedGB,FreeGB,CommitGB,CommitLimitGB,AvailableMB,PagesPerSec,NonPagedMB,PagedMB,StandbyMB,VmmemPrivateMB,VmmemWorkingSetMB,DockerPrivateMB,DockerBackendPrivateMB,MsMpEngPrivateMB,NodePrivateMB,NodeWorkingMB,JavaPrivateMB,JavaWorkingMB,MySQLPrivateMB,CodexPrivateMB,TopPrivate1,TopPrivate2,TopPrivate3,Port3000,Port8080,Port2238' | Out-File $log -Encoding utf8

for ($i = 1; $i -le 90; $i++) {
    $os = Get-CimInstance Win32_OperatingSystem
    $counters = Get-Counter @(
        '\Memory\Available MBytes',
        '\Memory\Committed Bytes',
        '\Memory\Commit Limit',
        '\Memory\Pool Nonpaged Bytes',
        '\Memory\Pool Paged Bytes',
        '\Memory\Standby Cache Normal Priority Bytes',
        '\Memory\Pages/sec'
    ) | Select-Object -ExpandProperty CounterSamples

    $availableMb = ($counters | Where-Object Path -like '*available mbytes').CookedValue
    $commitGb = ($counters | Where-Object Path -like '*committed bytes').CookedValue / 1GB
    $commitLimitGb = ($counters | Where-Object Path -like '*commit limit').CookedValue / 1GB
    $nonPagedMb = ($counters | Where-Object Path -like '*pool nonpaged bytes').CookedValue / 1MB
    $pagedMb = ($counters | Where-Object Path -like '*pool paged bytes').CookedValue / 1MB
    $standbyMb = ($counters | Where-Object Path -like '*standby cache normal priority bytes').CookedValue / 1MB
    $pagesPerSec = ($counters | Where-Object Path -like '*pages/sec').CookedValue

    $top = Get-Process |
        Sort-Object PrivateMemorySize64 -Descending |
        Select-Object -First 3 |
        ForEach-Object { "$($_.ProcessName):$([math]::Round($_.PrivateMemorySize64 / 1MB, 0))MB" }

    $row = [pscustomobject]@{
        Time = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        UsedGB = [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1MB, 2)
        FreeGB = [math]::Round($os.FreePhysicalMemory / 1MB, 2)
        CommitGB = [math]::Round($commitGb, 2)
        CommitLimitGB = [math]::Round($commitLimitGb, 2)
        AvailableMB = [math]::Round($availableMb, 1)
        PagesPerSec = [math]::Round($pagesPerSec, 1)
        NonPagedMB = [math]::Round($nonPagedMb, 1)
        PagedMB = [math]::Round($pagedMb, 1)
        StandbyMB = [math]::Round($standbyMb, 1)
        VmmemPrivateMB = Sum-PrivateMb 'vmmemWSL'
        VmmemWorkingSetMB = Sum-WorkingMb 'vmmemWSL'
        DockerPrivateMB = Sum-PrivateMb 'Docker Desktop'
        DockerBackendPrivateMB = Sum-PrivateMb 'com.docker.backend'
        MsMpEngPrivateMB = Sum-PrivateMb 'MsMpEng'
        NodePrivateMB = Sum-PrivateMb 'node'
        NodeWorkingMB = Sum-WorkingMb 'node'
        JavaPrivateMB = Sum-PrivateMb 'java'
        JavaWorkingMB = Sum-WorkingMb 'java'
        MySQLPrivateMB = Sum-PrivateMb 'mysqld'
        CodexPrivateMB = Sum-PrivateMb 'Codex'
        TopPrivate1 = $top[0]
        TopPrivate2 = $top[1]
        TopPrivate3 = $top[2]
        Port3000 = Is-PortListening 3000
        Port8080 = Is-PortListening 8080
        Port2238 = Is-PortListening 2238
    }

    $line = '"{0}",{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18},{19},{20},"{21}","{22}","{23}",{24},{25},{26}' -f `
        $row.Time, $row.UsedGB, $row.FreeGB, $row.CommitGB, $row.CommitLimitGB, $row.AvailableMB, $row.PagesPerSec, `
        $row.NonPagedMB, $row.PagedMB, $row.StandbyMB, $row.VmmemPrivateMB, $row.VmmemWorkingSetMB, `
        $row.DockerPrivateMB, $row.DockerBackendPrivateMB, $row.MsMpEngPrivateMB, $row.NodePrivateMB, $row.NodeWorkingMB, `
        $row.JavaPrivateMB, $row.JavaWorkingMB, $row.MySQLPrivateMB, $row.CodexPrivateMB, $row.TopPrivate1, $row.TopPrivate2, `
        $row.TopPrivate3, $row.Port3000, $row.Port8080, $row.Port2238
    $line | Out-File $log -Encoding utf8 -Append

    Start-Sleep -Seconds 30
}

$data = Import-Csv $log
$first = $data[0]
$last = $data[-1]
@"

Finished real dev monitor: $(Get-Date)

Log: $log

First sample:
UsedGB=$($first.UsedGB), FreeGB=$($first.FreeGB), CommitGB=$($first.CommitGB), VmmemPrivateMB=$($first.VmmemPrivateMB), NodePrivateMB=$($first.NodePrivateMB), JavaPrivateMB=$($first.JavaPrivateMB)

Last sample:
UsedGB=$($last.UsedGB), FreeGB=$($last.FreeGB), CommitGB=$($last.CommitGB), VmmemPrivateMB=$($last.VmmemPrivateMB), NodePrivateMB=$($last.NodePrivateMB), JavaPrivateMB=$($last.JavaPrivateMB)

Min FreeGB=$(($data | Measure-Object FreeGB -Minimum).Minimum)
Max CommitGB=$(($data | Measure-Object CommitGB -Maximum).Maximum)
Max PagesPerSec=$(($data | Measure-Object PagesPerSec -Maximum).Maximum)
Max VmmemPrivateMB=$(($data | Measure-Object VmmemPrivateMB -Maximum).Maximum)
Max NodePrivateMB=$(($data | Measure-Object NodePrivateMB -Maximum).Maximum)
Max JavaPrivateMB=$(($data | Measure-Object JavaPrivateMB -Maximum).Maximum)
"@ | Out-File $summary -Append -Encoding utf8

$ErrorActionPreference = 'Continue'

$log = 'C:\Users\32957\Desktop\real-dev-memory-monitor.csv'
$summary = 'C:\Users\32957\Desktop\real-dev-memory-monitor-summary.txt'

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

Remove-Item -LiteralPath $log, $summary -ErrorAction SilentlyContinue
"Started monitor-only real dev run: $(Get-Date)" | Out-File $summary -Encoding utf8
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

    $line = '"{0}",{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18},{19},{20},"{21}","{22}","{23}",{24},{25},{26}' -f `
        (Get-Date).ToString('yyyy-MM-dd HH:mm:ss'),
        [math]::Round(($os.TotalVisibleMemorySize - $os.FreePhysicalMemory) / 1MB, 2),
        [math]::Round($os.FreePhysicalMemory / 1MB, 2),
        [math]::Round($commitGb, 2),
        [math]::Round($commitLimitGb, 2),
        [math]::Round($availableMb, 1),
        [math]::Round($pagesPerSec, 1),
        [math]::Round($nonPagedMb, 1),
        [math]::Round($pagedMb, 1),
        [math]::Round($standbyMb, 1),
        (Sum-PrivateMb 'vmmemWSL'),
        (Sum-WorkingMb 'vmmemWSL'),
        (Sum-PrivateMb 'Docker Desktop'),
        (Sum-PrivateMb 'com.docker.backend'),
        (Sum-PrivateMb 'MsMpEng'),
        (Sum-PrivateMb 'node'),
        (Sum-WorkingMb 'node'),
        (Sum-PrivateMb 'java'),
        (Sum-WorkingMb 'java'),
        (Sum-PrivateMb 'mysqld'),
        (Sum-PrivateMb 'Codex'),
        $top[0], $top[1], $top[2],
        (Is-PortListening 3000),
        (Is-PortListening 8080),
        (Is-PortListening 2238)
    $line | Out-File $log -Encoding utf8 -Append
    Start-Sleep -Seconds 30
}

$data = Import-Csv $log
$first = $data[0]
$last = $data[-1]
@"
Finished monitor-only real dev run: $(Get-Date)

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

param([Parameter(Mandatory=$true)][string]$JarPath,
      [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
      [Parameter(Mandatory=$true)][string]$RunDirectory,
      [Parameter(Mandatory=$true)][string]$SourceCommit)
$ErrorActionPreference = 'Stop'
$nativeSource = (Resolve-Path -LiteralPath $JarPath).Path
$nativeOutput = [IO.Path]::GetFullPath($EvidenceDirectory)
$nativeRun = [IO.Path]::GetFullPath($RunDirectory)
if (Test-Path -LiteralPath $nativeRun) { throw 'A new empty run directory is required' }
New-Item -ItemType Directory -Path $nativeOutput,$nativeRun -Force | Out-Null
Start-Transcript -Path (Join-Path $nativeOutput 'transcript.txt') -Force
$nativeProcess = $null
try {
    $nativeJava = (Get-Command java.exe).Source
    $nativeRuntime = & $nativeJava -XshowSettings:properties -version 2>&1 | Out-String
    $nativeRuntime | Set-Content -LiteralPath (Join-Path $nativeOutput 'runtime.txt')
    $nativeJavaHome = [regex]::Match($nativeRuntime,'java.home = ([^\r\n]+)').Groups[1].Value.Trim()
    if ($nativeRuntime -notmatch 'java.version = 25\.') { throw 'Java 25 required' }
    $nativeApp = Join-Path $nativeRun 'app'
    $nativeClasses = Join-Path $nativeRun 'classes'
    New-Item -ItemType Directory -Path $nativeApp,$nativeClasses -Force | Out-Null
    $nativeJar = Join-Path $nativeApp 'ponhub.jar'
    Copy-Item -LiteralPath $nativeSource -Destination $nativeJar
    $nativeHash = (Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant()
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $nativeZip = [IO.Compression.ZipFile]::OpenRead($nativeJar)
    try {
        $nativeReader = [IO.StreamReader]::new($nativeZip.GetEntry('META-INF/MANIFEST.MF').Open())
        $nativeManifest = $nativeReader.ReadToEnd()
        $nativeReader.Dispose()
        $nativeNotices = @($nativeZip.Entries | Where-Object { $_.FullName -like 'META-INF/licenses/*' -and $_.Length -gt 0 } | ForEach-Object FullName)
    } finally { $nativeZip.Dispose() }
    @{ SourceCommit=$SourceCommit; Bytes=(Get-Item -LiteralPath $nativeJar).Length;
       SHA256=$nativeHash; Manifest=$nativeManifest; JavaHome=$nativeJavaHome;
       PackagedNotices=$nativeNotices; ProductJarModified=$false } |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $nativeOutput 'identity.json')
    & "$nativeJavaHome\bin\javac.exe" -cp $nativeJar -d $nativeClasses (Join-Path $PSScriptRoot 'ScrollbarAgent.java')
    if ($LASTEXITCODE -ne 0) { throw 'Diagnostic agent compilation failed' }
    $nativeAgentManifest = Join-Path $nativeRun 'agent.mf'
    Set-Content -LiteralPath $nativeAgentManifest -Value "Premain-Class: ScrollbarAgent`n" -Encoding ascii
    $nativeAgent = Join-Path $nativeRun 'agent.jar'
    & "$nativeJavaHome\bin\jar.exe" cfm $nativeAgent $nativeAgentManifest -C $nativeClasses .
    if ($LASTEXITCODE -ne 0) { throw 'Diagnostic agent packaging failed' }
    Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public class Round1NativeWindow {
    public delegate bool EnumCallback(IntPtr handle, IntPtr ignored);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumCallback callback, IntPtr ignored);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr handle, out uint process);
    [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr handle, StringBuilder title, int size);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr handle, int command);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr handle);
    public static IntPtr Find(int process) {
        IntPtr found = IntPtr.Zero;
        EnumWindows((handle, ignored) => {
            uint owner; GetWindowThreadProcessId(handle, out owner);
            var title = new StringBuilder(512); GetWindowText(handle, title, 512);
            if (owner == process && title.ToString().Contains("PonHub")) { found = handle; return false; }
            return true;
        }, IntPtr.Zero);
        return found;
    }
}
'@
    $nativeProcess = Start-Process -WindowStyle Hidden -FilePath "$nativeJavaHome\bin\java.exe" -ArgumentList @("`"-javaagent:$nativeAgent=$nativeOutput`"",'-jar','ponhub.jar') -WorkingDirectory $nativeApp -PassThru -RedirectStandardOutput (Join-Path $nativeOutput 'stdout.log') -RedirectStandardError (Join-Path $nativeOutput 'stderr.log')
    # Keep the console helper hidden, but explicitly show the actual application under test.
    for ($nativeAttempt = 0; $nativeAttempt -lt 80; $nativeAttempt++) {
        $nativeHandle = [Round1NativeWindow]::Find($nativeProcess.Id)
        if ($nativeHandle -ne [IntPtr]::Zero) {
            [Round1NativeWindow]::ShowWindow($nativeHandle, 5) | Out-Null
            [Round1NativeWindow]::SetForegroundWindow($nativeHandle) | Out-Null
            Write-Output "Showing only test application's window for process $($nativeProcess.Id)."
            break
        }
        Start-Sleep -Milliseconds 100
    }
    if (-not $nativeProcess.WaitForExit(180000)) { throw 'Native diagnostic exceeded its three-minute bound' }
    Get-Content -LiteralPath (Join-Path $nativeOutput 'result.txt')
    if (-not (Select-String -LiteralPath (Join-Path $nativeOutput 'result.txt') -Pattern '^Windows scrollbar recheck=PASS$' -Quiet)) { throw 'Native diagnostic did not pass' }
    if ((Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $nativeHash) { throw 'Product JAR changed' }
    Write-Output "Unchanged product JAR: $nativeHash"
} finally {
    if ($nativeProcess -and -not $nativeProcess.HasExited) { Stop-Process -Id $nativeProcess.Id }
    Stop-Transcript
}

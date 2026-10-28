param([Parameter(Mandatory=$true)][string]$JarPath,
      [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
      [string]$SourceCommit)
$ErrorActionPreference='Stop'
if (-not $SourceCommit) {$SourceCommit=(& git rev-parse HEAD)}
$nativeJarSource=(Resolve-Path -LiteralPath $JarPath).Path
$nativeOutput=[IO.Path]::GetFullPath($EvidenceDirectory)
New-Item -ItemType Directory -Path $nativeOutput -Force | Out-Null
Start-Transcript -Path (Join-Path $nativeOutput 'transcript.txt') -Force
$nativeJava=(Get-Command java.exe).Source
$nativeRuntime=& $nativeJava -XshowSettings:properties -version 2>&1 | Out-String
$nativeRuntime | Set-Content -LiteralPath (Join-Path $nativeOutput 'runtime.txt')
$nativeJavaHome=[regex]::Match($nativeRuntime,'java.home = ([^\r\n]+)').Groups[1].Value.Trim()
if (-not $nativeJavaHome -or $nativeRuntime -notmatch 'java.version = 25\.') {throw 'Java 25 required'}
$nativeRun=Join-Path $nativeOutput ('run-'+[Guid]::NewGuid().ToString('N'))
$nativeApp=Join-Path $nativeRun 'app'
$nativeClasses=Join-Path $nativeRun 'classes'
New-Item -ItemType Directory -Path $nativeApp,$nativeClasses -Force | Out-Null
$nativeJar=Join-Path $nativeApp 'ponhub.jar'
Copy-Item -LiteralPath $nativeJarSource -Destination $nativeJar
$nativeHash=(Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant()
Add-Type -AssemblyName System.IO.Compression.FileSystem
$nativeZip=[IO.Compression.ZipFile]::OpenRead($nativeJar)
try { $nativeReader=[IO.StreamReader]::new($nativeZip.GetEntry('META-INF/MANIFEST.MF').Open());
      $nativeManifest=$nativeReader.ReadToEnd(); $nativeReader.Dispose() } finally {$nativeZip.Dispose()}
@{SourceCommit=$SourceCommit;Source=$nativeJarSource;Bytes=(Get-Item -LiteralPath $nativeJar).Length;
  SHA256=$nativeHash;Manifest=$nativeManifest;JavaHome=$nativeJavaHome;FreshFolder=$nativeApp} |
 ConvertTo-Json | Set-Content -LiteralPath (Join-Path $nativeOutput 'identity.json')
& "$nativeJavaHome\bin\javac.exe" -cp $nativeJar -d $nativeClasses (Join-Path $PSScriptRoot 'Round1Agent.java')
if ($LASTEXITCODE -ne 0) {throw 'Agent compilation failed'}
$nativeManifestPath=Join-Path $nativeRun 'agent.mf'
Set-Content -LiteralPath $nativeManifestPath -Value "Premain-Class: Round1Agent`n" -Encoding ascii
$nativeAgent=Join-Path $nativeRun 'agent.jar'
& "$nativeJavaHome\bin\jar.exe" cfm $nativeAgent $nativeManifestPath -C $nativeClasses .
if ($LASTEXITCODE -ne 0) {throw 'Agent packaging failed'}
$nativeProcess=Start-Process -WindowStyle Normal -FilePath "$nativeJavaHome\bin\java.exe" -ArgumentList @("`"-javaagent:$nativeAgent=$nativeOutput`"",'-jar','ponhub.jar') -WorkingDirectory $nativeApp -PassThru -RedirectStandardOutput (Join-Path $nativeOutput 'stdout.log') -RedirectStandardError (Join-Path $nativeOutput 'stderr.log')
try {
 if (-not $nativeProcess.WaitForExit(45000)) {throw 'Bounded native check timed out'}
 Get-Content -LiteralPath (Join-Path $nativeOutput 'result.txt')
 if (-not (Select-String -LiteralPath (Join-Path $nativeOutput 'result.txt') -Pattern '^Diagnostic completed=true$' -Quiet)) {throw 'Native assertions failed'}
 if ((Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $nativeHash) {throw 'JAR bytes changed'}
} finally {if (-not $nativeProcess.HasExited) {Stop-Process -Id $nativeProcess.Id}; Stop-Transcript}

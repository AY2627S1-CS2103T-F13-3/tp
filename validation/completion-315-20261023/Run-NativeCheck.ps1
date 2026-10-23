param([Parameter(Mandatory=$true)][string]$JarPath,
      [Parameter(Mandatory=$true)][string]$JavaHome,
      [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
      [switch]$Protected)
$ErrorActionPreference='Stop'
$nativeJarSource=(Resolve-Path -LiteralPath $JarPath).Path
$nativeOutput=[IO.Path]::GetFullPath($EvidenceDirectory)
$nativeRun=Join-Path ([IO.Path]::GetTempPath()) ('ponhub-completion-'+[Guid]::NewGuid().ToString('N'))
$nativeApp=Join-Path $nativeRun 'app'
$nativeClasses=Join-Path $nativeRun 'classes'
New-Item -ItemType Directory -Path $nativeOutput,$nativeApp,$nativeClasses -Force | Out-Null
$nativeJar=Join-Path $nativeApp 'ponhub.jar'
Copy-Item -LiteralPath $nativeJarSource -Destination $nativeJar
$nativeHash=(Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant()
Add-Type -AssemblyName System.IO.Compression.FileSystem
$nativeZip=[IO.Compression.ZipFile]::OpenRead($nativeJar)
try { $nativeReader=[IO.StreamReader]::new($nativeZip.GetEntry('META-INF/MANIFEST.MF').Open());
      $nativeManifest=$nativeReader.ReadToEnd(); $nativeReader.Dispose() } finally {$nativeZip.Dispose()}
@{Source=$nativeJarSource;Bytes=(Get-Item -LiteralPath $nativeJar).Length;SHA256=$nativeHash;
  Manifest=$nativeManifest;JavaHome=$JavaHome;FreshFolder=$nativeApp;Protected=[bool]$Protected} |
 ConvertTo-Json | Set-Content -LiteralPath (Join-Path $nativeOutput 'identity.json')
& "$JavaHome\bin\java.exe" -version 2>&1 | Set-Content -LiteralPath (Join-Path $nativeOutput 'java-version.txt')
& "$JavaHome\bin\javac.exe" -cp $nativeJar -d $nativeClasses (Join-Path $PSScriptRoot 'CompletionAgent.java')
if ($LASTEXITCODE -ne 0) {throw 'Agent compilation failed'}
$nativeManifestPath=Join-Path $nativeRun 'agent.mf'
Set-Content -LiteralPath $nativeManifestPath -Value "Premain-Class: CompletionAgent`n" -Encoding ascii
$nativeAgent=Join-Path $nativeRun 'agent.jar'
& "$JavaHome\bin\jar.exe" cfm $nativeAgent $nativeManifestPath -C $nativeClasses .
if ($LASTEXITCODE -ne 0) {throw 'Agent packaging failed'}
if ($Protected) {
 New-Item -ItemType Directory -Path (Join-Path $nativeApp 'data') | Out-Null
 Set-Content -LiteralPath (Join-Path $nativeApp 'data\ponhub.json') -Value '{broken json' -NoNewline
}
$nativeProcess=Start-Process -FilePath "$JavaHome\bin\java.exe" -ArgumentList @("`"-javaagent:$nativeAgent=$nativeOutput`"",'-jar',"`"$nativeJar`"") -WorkingDirectory $nativeApp -PassThru -RedirectStandardOutput (Join-Path $nativeOutput 'stdout.log') -RedirectStandardError (Join-Path $nativeOutput 'stderr.log')
try {
 if (-not $nativeProcess.WaitForExit(60000)) {throw 'Bounded native check timed out'}
 Get-Content -LiteralPath (Join-Path $nativeOutput 'result.txt')
 if (-not (Select-String -LiteralPath (Join-Path $nativeOutput 'result.txt') -Pattern '^Packaged completion checks=PASS$' -Quiet)) {throw 'Native assertions failed'}
 if ((Get-FileHash -LiteralPath $nativeJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $nativeHash) {throw 'JAR bytes changed'}
} finally {if (-not $nativeProcess.HasExited) {Stop-Process -Id $nativeProcess.Id}}

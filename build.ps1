param(
 [string]$BuildTools,
 [string]$AndroidJar,
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$BuildDirectory = (Join-Path $PSScriptRoot '.build'),
 [switch]$Watch
)
$ErrorActionPreference = 'Stop'
if (-not $BuildTools -or -not $AndroidJar) { throw 'Pass -BuildTools (SDK build-tools/36.0.0 folder) and -AndroidJar (SDK platforms/android-36/android.jar).' }
if (-not $JavaHome) { $JavaHome = Split-Path (Split-Path (Get-Command javac).Source) }
$BuildTools = (Resolve-Path -LiteralPath $BuildTools).Path
$AndroidJar = (Resolve-Path -LiteralPath $AndroidJar).Path
New-Item -ItemType Directory -Force -Path $BuildDirectory | Out-Null
$BuildDirectory = (Resolve-Path -LiteralPath $BuildDirectory).Path
$classes = Join-Path $BuildDirectory 'classes'
$dex = Join-Path $BuildDirectory 'dex'
New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
function Checked([string]$Program,[string[]]$Arguments) {
 & $Program @Arguments
 if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE" }
}
$key = Join-Path $BuildDirectory 'development.keystore'
$main = Join-Path $PSScriptRoot 'app/src/main'
$apkName = 'DeviceVitals.apk'
if ($Watch) {
 $main = Join-Path $PSScriptRoot 'wear/src/main'
 $apkName = 'DeviceVitals-Watch.apk'
}
if ($Watch) {
 $BuildDirectory = Join-Path $BuildDirectory 'wear'
 $classes = Join-Path $BuildDirectory 'classes'
 $dex = Join-Path $BuildDirectory 'dex'
 New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
}
Checked "$BuildTools/aapt2.exe" @('compile','--dir',"$main/res",'-o',"$BuildDirectory/resources.zip")
Checked "$BuildTools/aapt2.exe" @('link','-o',"$BuildDirectory/unsigned.apk",'-I',$AndroidJar,'--manifest',"$main/AndroidManifest.xml",'--version-code','7','--version-name','1.6','--min-sdk-version','26','--target-sdk-version','36',"$BuildDirectory/resources.zip")
$sources = @(Get-ChildItem "$main/java" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
Checked "$JavaHome/bin/javac.exe" (@('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',($AndroidJar + ';' + "$BuildTools/core-lambda-stubs.jar"),'-d',$classes) + $sources)
Checked "$JavaHome/bin/jar.exe" @("-J-Djava.io.tmpdir=$BuildDirectory",'cf',"$BuildDirectory/classes.jar",'-C',$classes,'.')
Checked "$JavaHome/bin/java.exe" @('-cp',"$BuildTools/lib/d8.jar",'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',$dex,"$BuildDirectory/classes.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open("$BuildDirectory/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try {
 Get-ChildItem $dex -Filter '*.dex' | ForEach-Object { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$_.FullName,$_.Name) | Out-Null }
} finally { $archive.Dispose() }
Checked "$BuildTools/zipalign.exe" @('-f','-p','4',"$BuildDirectory/unsigned.apk","$BuildDirectory/aligned.apk")
if (-not (Test-Path -LiteralPath $key)) {
 Checked "$JavaHome/bin/keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Device Vitals Development,O=Local,C=US')
}
$apk = Join-Path (Split-Path $PSScriptRoot -Parent) $apkName
Checked "$JavaHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,"$BuildDirectory/aligned.apk")
Checked "$JavaHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'verify','--verbose',$apk)
Checked "$BuildTools/zipalign.exe" @('-c','4',$apk)
Write-Output "Built and verified: $apk"




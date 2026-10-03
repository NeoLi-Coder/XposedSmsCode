param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$taskRoot = Split-Path -Parent $PSScriptRoot
$checkOutput = Join-Path $taskRoot '.gradle/core-check'
New-Item -ItemType Directory -Path $checkOutput -Force | Out-Null
$compiler = if ($JavaHome) { Join-Path $JavaHome 'bin/javac.exe' } else { 'javac' }
$runner = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { 'java' }
$sources = @(
    (Join-Path $taskRoot 'app/src/main/java/com/tianma/xsmscode/common/constant/SmsCodeConst.java'),
    (Join-Path $taskRoot 'app/src/main/java/com/tianma/xsmscode/common/utils/SmsCodeParser.java'),
    (Join-Path $taskRoot 'app/src/test/java/com/tianma/xsmscode/common/utils/SmsCodeParserCheck.java')
)
& $compiler -encoding UTF-8 --release 11 -d $checkOutput @sources
if ($LASTEXITCODE -ne 0) { throw 'Core check compilation failed' }
& $runner -cp $checkOutput com.tianma.xsmscode.common.utils.SmsCodeParserCheck
if ($LASTEXITCODE -ne 0) { throw 'Core regression check failed' }

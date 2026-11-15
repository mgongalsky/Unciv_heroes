param(
    [int]$Width = 1568,
    [int]$Height = 690
)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDirectory = Join-Path $projectRoot 'desktop/build/city-preview'
[System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$initPath = Join-Path $outputDirectory 'classpath.init.gradle'
$initScript = @'
gradle.projectsEvaluated {
    def desktop = gradle.rootProject.findProject(':desktop')
    if (desktop == null) return
    desktop.tasks.register('writeCityPreviewClasspath') {
        doLast {
            def output = new File(desktop.buildDir, 'city-preview/runtime-classpath.txt')
            output.parentFile.mkdirs()
            output.setText(desktop.sourceSets.main.runtimeClasspath.asPath, 'UTF-8')
        }
    }
}
'@
[System.IO.File]::WriteAllText($initPath, $initScript, $utf8)
Push-Location $projectRoot
try {
    # Только получение classpath: задача не зависит от компиляции или тестов.
    & (Join-Path $projectRoot 'gradlew.bat') --offline --console=plain -q -I $initPath :desktop:writeCityPreviewClasspath
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve city preview runtime classpath' }
    $classpath = [System.IO.File]::ReadAllText((Join-Path $outputDirectory 'runtime-classpath.txt')).Trim()
    $javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java.exe).Source }
    $output = Join-Path $outputDirectory "city-$Width-$Height.png"
    $stdout = Join-Path $outputDirectory "city-$Width-$Height.stdout.log"
    $stderr = Join-Path $outputDirectory "city-$Width-$Height.stderr.log"
    $arguments = @('-cp', ('"' + $classpath + '"'), 'com.unciv.app.desktop.CityPreviewCapture',
        ('"--output=' + $output + '"'), "--width=$Width", "--height=$Height")
    $process = Start-Process -FilePath $javaCommand -ArgumentList $arguments -WorkingDirectory (Join-Path $projectRoot 'android/assets') -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $processHandle = $process.Handle
    if (-not $process.WaitForExit(55000)) {
        $process.Kill()
        throw "City preview timed out. Logs: $outputDirectory"
    }
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        Get-Content -LiteralPath $stderr -Tail 60
        throw "City preview failed with exit code $($process.ExitCode)"
    }
    if (-not (Test-Path -LiteralPath $output)) { throw 'Capture finished without a PNG' }
    Get-Content -LiteralPath $stdout -Tail 15
    Write-Output "Preview: $output"
} finally {
    Pop-Location
}

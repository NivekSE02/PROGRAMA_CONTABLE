$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

# ============================================================
# RUTAS
# ============================================================

$javaHome = 'C:\Program Files\Java\jdk-21'
$jpackage = Join-Path $javaHome 'bin\jpackage.exe'
$jlink = Join-Path $javaHome 'bin\jlink.exe'
$java = Join-Path $javaHome 'bin\java.exe'

if (-not (Test-Path $jpackage)) {
    throw "No se encontró jpackage: $jpackage"
}

if (-not (Test-Path $jlink)) {
    throw "No se encontró jlink: $jlink"
}

if (-not (Test-Path $java)) {
    throw "No se encontró java.exe: $java"
}

# ============================================================
# MAVEN
# ============================================================

$mavenCommand = 'C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd'

if (-not (Test-Path $mavenCommand)) {
    throw "No se encontró Maven: $mavenCommand"
}

# ============================================================
# WIX
# ============================================================

$env:Path += ';C:\Program Files (x86)\WiX Toolset v3.14\bin'

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    throw 'No se encontró candle.exe de WiX.'
}

if (-not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
    throw 'No se encontró light.exe de WiX.'
}

# ============================================================
# CARPETAS DE BUILD
# ============================================================

$buildStamp = Get-Date -Format 'yyyyMMdd-HHmmss'

$buildDirectory = Join-Path $projectRoot "target\msi-build-$buildStamp"
$dependenciesDirectory = Join-Path $buildDirectory 'dependencies'
$inputDirectory = Join-Path $buildDirectory 'input'
$javafxModulePath = Join-Path $buildDirectory 'javafx-modules'
$runtimeDirectory = Join-Path $buildDirectory 'runtime'
$outputDirectory = Join-Path $projectRoot "target\installer-$buildStamp"

New-Item -ItemType Directory -Force `
    -Path $dependenciesDirectory,
          $inputDirectory,
          $javafxModulePath,
          $outputDirectory | Out-Null

# ============================================================
# MAVEN BUILD + DEPENDENCIAS
# ============================================================

$mavenArguments = @(
    '-DskipTests',
    'package',
    'dependency:copy-dependencies',
    '-DincludeScope=runtime',
    "-DoutputDirectory=$dependenciesDirectory"
)

Write-Host ''
Write-Host '=== COMPILANDO PROYECTO ===' -ForegroundColor Cyan
Write-Host ''

& $mavenCommand @mavenArguments

if ($LASTEXITCODE -ne 0) {
    throw "Maven terminó con código $LASTEXITCODE."
}

# ============================================================
# ENCONTRAR JAR PRINCIPAL
# ============================================================

$applicationJar = Get-ChildItem `
    -LiteralPath (Join-Path $projectRoot 'target') `
    -Filter 'ContaNoPortable-*.jar' |
    Where-Object {
        $_.Name -notlike '*-sources.jar' -and
        $_.Name -notlike '*-javadoc.jar'
    } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $applicationJar) {
    throw 'No se encontró el JAR de la aplicación.'
}

Write-Host ''
Write-Host "JAR encontrado: $($applicationJar.Name)" -ForegroundColor Green

# ============================================================
# COPIAR DEPENDENCIAS NORMALES A INPUT
# EXCLUIMOS TODOS LOS JAVAFX
# ============================================================

Get-ChildItem `
    -LiteralPath $dependenciesDirectory `
    -Filter '*.jar' |
    Where-Object {
        $_.Name -notlike 'javafx-*.jar'
    } |
    ForEach-Object {
        Copy-Item `
            -LiteralPath $_.FullName `
            -Destination $inputDirectory `
            -Force
    }

Copy-Item `
    -LiteralPath $applicationJar.FullName `
    -Destination $inputDirectory `
    -Force

# ============================================================
# JAVAFX
# SOLO LOS JAR -WIN
# ============================================================

Get-ChildItem `
    -LiteralPath $dependenciesDirectory `
    -Filter 'javafx-*-win.jar' |
    ForEach-Object {
        Copy-Item `
            -LiteralPath $_.FullName `
            -Destination $javafxModulePath `
            -Force
    }

$javafxJars = Get-ChildItem `
    -LiteralPath $javafxModulePath `
    -Filter 'javafx-*-win.jar'

if ($javafxJars.Count -eq 0) {
    throw 'No se encontraron los módulos JavaFX -win.jar.'
}

Write-Host ''
Write-Host '=== MODULOS JAVAFX ===' -ForegroundColor Cyan

$javafxJars | ForEach-Object {
    Write-Host "  $($_.Name)"
}

# ============================================================
# CREAR RUNTIME CON JLINK
# ============================================================

Write-Host ''
Write-Host '=== CREANDO RUNTIME JAVAFX CON JLINK ===' -ForegroundColor Cyan
Write-Host ''

$modulePath = "$($javaHome)\jmods;$javafxModulePath"

$jlinkArguments = @(
    '--module-path', $modulePath,
   '--add-modules', 'javafx.controls,javafx.fxml,java.sql',
    '--output', $runtimeDirectory,
    '--strip-debug',
    '--no-header-files',
    '--no-man-pages',
    '--compress=2'
)

& $jlink @jlinkArguments

if ($LASTEXITCODE -ne 0) {
    throw "jlink terminó con código $LASTEXITCODE."
}

# ============================================================
# VERIFICAR RUNTIME
# ============================================================

Write-Host ''
Write-Host '=== VERIFICANDO RUNTIME ===' -ForegroundColor Cyan
Write-Host ''

$jimage = Join-Path $javaHome 'bin\jimage.exe'
$modulesImage = Join-Path $runtimeDirectory 'lib\modules'

if (-not (Test-Path $modulesImage)) {
    throw "No se encontró el runtime generado por jlink."
}

$javafxCheck = & $jimage list $modulesImage | Select-String 'javafx'

if (-not $javafxCheck) {
    throw 'ERROR: El runtime no contiene módulos JavaFX.'
}

$javafxCheck | ForEach-Object {
    Write-Host $_
}

Write-Host ''
Write-Host 'Runtime JavaFX creado correctamente.' -ForegroundColor Green

# ============================================================
# JPACKAGE
# ============================================================

Write-Host ''
Write-Host '=== GENERANDO MSI ===' -ForegroundColor Cyan
Write-Host ''

$jpackageArguments = @(
    '--type', 'msi',
    '--name', 'ContaNoPortable',
    '--app-version', '1.0.0',
    '--vendor', 'ContaNoPortable',

    '--input', $inputDirectory,

    '--main-jar', $applicationJar.Name,
    '--main-class', 'com.mycompany.programa_contable.Launcher',

    '--runtime-image', $runtimeDirectory,

    '--java-options', '--add-modules=javafx.controls,javafx.fxml',

    '--dest', $outputDirectory,

    '--icon', (Join-Path $projectRoot 'icono.ico'),

    '--win-shortcut',
    '--win-menu',
    '--win-menu-group', 'ContaNoPortable',

    '--win-upgrade-uuid', '50a2d76e-5c30-4f72-94ab-61fa12622a9d',

    '--win-per-user-install'
)

& $jpackage @jpackageArguments

if ($LASTEXITCODE -ne 0) {
    throw "jpackage terminó con código $LASTEXITCODE."
}

Write-Host ''
Write-Host '========================================' -ForegroundColor Green
Write-Host ' MSI GENERADO CORRECTAMENTE' -ForegroundColor Green
Write-Host '========================================' -ForegroundColor Green
Write-Host ''
Write-Host "MSI: $outputDirectory"
Write-Host ''
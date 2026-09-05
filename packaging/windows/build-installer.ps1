$ErrorActionPreference = "Stop"

Write-Host "=== DentalCare - Empaquetado Windows ===" -ForegroundColor Cyan

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven no está disponible en PATH."
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage no está disponible. Instala un JDK 17 o superior y verifica PATH."
}

$icon = Join-Path (Get-Location) "packaging\windows\dentalcare.ico"
if (-not (Test-Path $icon)) {
    throw "No se encontró el icono de Windows: $icon"
}

Write-Host "[1/3] Compilando..."
mvn clean package -DskipTests

$jar = Get-ChildItem "target\dentalcare-*.jar" |
    Where-Object { $_.Name -notmatch "original" } |
    Select-Object -First 1

if (-not $jar) {
    throw "No se encontró el JAR ejecutable en target."
}

$output = Join-Path (Get-Location) "target\installer"
if (Test-Path $output) {
    Remove-Item $output -Recurse -Force
}
New-Item -ItemType Directory -Path $output | Out-Null

Write-Host "[2/3] Generando aplicación Windows..."
jpackage `
    --type app-image `
    --input $jar.DirectoryName `
    --name DentalCare `
    --main-jar $jar.Name `
    --main-class mx.dentalcare.ui.DentalCareJavaFXApplication `
    --dest $output `
    --icon $icon `
    --win-dir-chooser `
    --win-menu `
    --win-shortcut

Write-Host "[3/3] Empaquetado terminado." -ForegroundColor Green
Write-Host "Aplicación: $output\DentalCare"
Write-Host "Para generar un instalador MSI, cambia --type app-image por --type msi."

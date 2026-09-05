$ErrorActionPreference = "Stop"

Write-Host "=== DentalCare - Instalador Windows ===" -ForegroundColor Cyan

function Require-Command($name, $message) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw $message
    }
}

Require-Command "mvn" "Maven no está disponible en PATH. Instala Maven y vuelve a intentarlo."
Require-Command "jpackage" "jpackage no está disponible. Instala un JDK 17 o superior y verifica PATH."

$icon = Join-Path (Get-Location) "packaging\windows\dentalcare.ico"
if (-not (Test-Path $icon)) {
    throw "No se encontró el icono de Windows: $icon"
}

Write-Host "[1/4] Compilando DentalCare..."
mvn clean package -DskipTests

$target = Join-Path (Get-Location) "target"
$input = Join-Path $target "installer-input"
$output = Join-Path $target "installer"

if (Test-Path $input) { Remove-Item $input -Recurse -Force }
if (Test-Path $output) { Remove-Item $output -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $input "lib") -Force | Out-Null
New-Item -ItemType Directory -Path $output -Force | Out-Null

$thinJar = Get-ChildItem $target -Filter "dentalcare-*.jar" |
    Where-Object { $_.Name -notmatch "-boot\.jar$|original" } |
    Select-Object -First 1

if (-not $thinJar) {
    throw "No se encontró el JAR de aplicación para el instalador."
}

Copy-Item $thinJar.FullName $input

$dependencies = Join-Path $input "lib"
if (-not (Test-Path $dependencies) -or -not (Get-ChildItem $dependencies -Filter "*.jar")) {
    throw "No se copiaron las dependencias runtime."
}

Write-Host "[2/4] Preparando aplicación autónoma..."
Write-Host "JAR principal: $($thinJar.Name)"
Write-Host "Dependencias: $((Get-ChildItem $dependencies -Filter '*.jar').Count) JAR(s)"

Write-Host "[3/4] Generando instalador MSI..."
jpackage `
    --type msi `
    --input $input `
    --name DentalCare `
    --main-jar $thinJar.Name `
    --main-class mx.dentalcare.ui.DentalCareJavaFXApplication `
    --dest $output `
    --icon $icon `
    --app-version 1.0.0 `
    --vendor "DentalCare" `
    --description "Sistema de gestión para consultorio dental" `
    --win-dir-chooser `
    --win-menu `
    --win-shortcut `
    --win-menu-group "DentalCare"

$msi = Get-ChildItem $output -Filter "DentalCare-*.msi" | Select-Object -First 1
if (-not $msi) {
    throw "jpackage terminó sin generar el instalador MSI."
}

Write-Host "[4/4] Instalador generado correctamente." -ForegroundColor Green
Write-Host "Archivo: $($msi.FullName)"
Write-Host ""
Write-Host "DentalCare está listo para instalarse en Windows." -ForegroundColor Green

<#
.SYNOPSIS
    Генерирует release-keystore ВНЕ репозитория и пишет keystore.properties локально.

.DESCRIPTION
    БЕЗОПАСНОСТЬ: ни keystore, ни пароли НЕ попадают в git.
      * Сам keystore создаётся в %LOCALAPPDATA%\gtd-widgets\release.keystore (вне репо).
      * keystore.properties создаётся в корне репо, но он в .gitignore.
      * *.keystore / *.jks тоже в .gitignore на случай генерации внутри репо.

    build.gradle.kts подхватывает signingConfig ТОЛЬКО если keystore.properties есть;
    без него release собирается с debug-подписью и сборка не падает.

    Пароль по умолчанию генерируется случайным; можно передать свой через -StorePassword.
    Скрипт идемпотентен: если keystore уже есть, повторно его не пересоздаёт (если не -Force).

.PARAMETER StorePassword
    Пароль хранилища/ключа. По умолчанию — случайный 24-символьный.

.PARAMETER Alias
    Алиас ключа. По умолчанию 'gtdwidgets'.

.PARAMETER Force
    Пересоздать keystore, даже если он уже существует.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\make-keystore.ps1
#>
[CmdletBinding()]
param(
    [string]$StorePassword,
    [string]$Alias = 'gtdwidgets',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

# --- Каталог keystore вне репозитория ---
$keystoreDir = Join-Path $env:LOCALAPPDATA 'gtd-widgets'
$keystorePath = Join-Path $keystoreDir 'release.keystore'
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

# --- Найти keytool: JAVA_HOME -> PATH -> toolchain JDK ---
function Resolve-Keytool {
    if ($env:JAVA_HOME) {
        $kt = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
        if (Test-Path $kt) { return $kt }
    }
    $cmd = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallback = 'D:\gtd-toolchain\jdk-17\bin\keytool.exe'
    if (Test-Path $fallback) { return $fallback }
    throw "keytool не найден. Установите JDK или задайте JAVA_HOME."
}
$keytool = Resolve-Keytool

# --- Пароль ---
if (-not $StorePassword) {
    $bytes = New-Object 'System.Byte[]' 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $StorePassword = [Convert]::ToBase64String($bytes) -replace '[+/=]', 'x'
}

# --- Генерация keystore (если нужно) ---
if ((Test-Path $keystorePath) -and (-not $Force)) {
    Write-Host "Keystore уже существует: $keystorePath (перезапись — с -Force)."
    Write-Host "Обновляю только keystore.properties (пароль должен совпадать с существующим)."
} else {
    if (Test-Path $keystorePath) { Remove-Item $keystorePath -Force }
    & $keytool -genkeypair `
        -keystore $keystorePath `
        -storepass $StorePassword `
        -keypass $StorePassword `
        -alias $Alias `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=GTD Flow Widgets, OU=Dev, O=gtdflow, C=RU"
    if ($LASTEXITCODE -ne 0) { throw "keytool завершился с кодом $LASTEXITCODE" }
    Write-Host "Keystore создан: $keystorePath"
}

# --- keystore.properties в корне репо (gitignored). Путь — с прямыми слэшами:
#     java.util.Properties трактует '\' как escape, поэтому обратные слэши нельзя. ---
$repoRoot = Split-Path -Parent $PSScriptRoot
$propsPath = Join-Path $repoRoot 'keystore.properties'
$storeFileForward = $keystorePath -replace '\\', '/'
$content = @(
    "# АВТОГЕНЕРАЦИЯ scripts/make-keystore.ps1 — НЕ КОММИТИТЬ (в .gitignore).",
    "storeFile=$storeFileForward",
    "storePassword=$StorePassword",
    "keyAlias=$Alias",
    "keyPassword=$StorePassword"
) -join "`n"
Set-Content -Path $propsPath -Value $content -Encoding UTF8 -NoNewline
Write-Host "keystore.properties записан: $propsPath"
Write-Host "Готово. Локальные release-сборки теперь подписываются этим ключом."

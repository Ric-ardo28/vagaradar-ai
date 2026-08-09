$ErrorActionPreference = 'Stop'

$projectDirectory = Split-Path -Parent $PSScriptRoot
$dockerDesktop = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'

if (-not (Get-Process -Name 'Docker Desktop' -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
}

$deadline = (Get-Date).AddMinutes(2)
do {
    Start-Sleep -Seconds 5
    docker info *> $null
} until ($LASTEXITCODE -eq 0 -or (Get-Date) -ge $deadline)

if ($LASTEXITCODE -ne 0) {
    throw 'O Docker Desktop não ficou disponível em até dois minutos.'
}

Set-Location $projectDirectory
docker compose --env-file .env.local up -d

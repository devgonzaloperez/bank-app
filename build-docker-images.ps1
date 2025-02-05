#Get the directory where the script is located.
$ROOT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition

#Navigate to the root directory.
Set-Location $ROOT_DIR

#Loop through each subdirectory.
Get-ChildItem -Directory | ForEach-Object {
    #Skip the .vscode and docker-compose directories.
    if ($_.Name -eq '.vscode' -or $_.Name -eq 'docker-compose') {
        Write-Host "Skipping $($_.Name)"
        return
    }

    $dir = $_.FullName
    Write-Host "Building Docker image for $($_.Name)"
    Set-Location $dir
    & mvn compile jib:dockerBuild
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build Docker image for $($_.Name)"
        exit 1
    }
    Set-Location $ROOT_DIR
}

Write-Host "Docker images built successfully for all microservices."

#Change the execution policy if its necessary with Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass.
#Execute with ./build-docker-images.ps1.

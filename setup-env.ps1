# setup-env.ps1: Configures Java 21 and Maven for the current PowerShell session.

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:MAVEN_HOME = "C:\Program Files\apache-maven-3.9.13"

# Add Java and Maven to the PATH for this session.
$javaBin = Join-Path $env:JAVA_HOME "bin"
$mavenBin = Join-Path $env:MAVEN_HOME "bin"
$env:Path = "$javaBin;$mavenBin;$env:Path"

# Load environment variables from .env if it exists.
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()

        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
    Write-Host "Loaded variables from .env"
}

$javaVersion = java -version 2>&1 | Select-Object -First 1
$mavenVersion = mvn -v 2>&1 | Select-Object -First 1

Write-Host "----------------------------------------------------"
Write-Host "Environment configured for AI Donor Matcher Backend"
Write-Host "Java Version: $javaVersion"
Write-Host "Maven Version: $mavenVersion"
Write-Host "----------------------------------------------------"

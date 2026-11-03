param([switch]$CompileOnly, [switch]$SmokeTest)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Import-LocalDbConfig {
    $file = Join-Path $PSScriptRoot ".env"
    if (Test-Path $file) {
        foreach ($line in [System.IO.File]::ReadAllLines($file)) {
            $entry = $line.Trim()
            if ($entry.Length -eq 0 -or $entry.StartsWith("#")) { continue }
            if ($entry -notmatch '^(DB_URL|DB_USER|DB_PASSWORD)\s*=\s*(.*)$') {
                throw "Invalid .env entry. Use DB_URL=..., DB_USER=..., or DB_PASSWORD=... (do not add PowerShell syntax)."
            }

            $name = $Matches[1]
            $value = $Matches[2].Trim()
            if ($value.Length -ge 2 -and
                (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                 ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            # Explicitly set terminal variables take precedence over the local .env file.
            if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }

    $password = [Environment]::GetEnvironmentVariable("DB_PASSWORD", "Process")
    if ([string]::IsNullOrWhiteSpace($password) -or
        $password -eq "REPLACE_WITH_YOUR_MYSQL_PASSWORD" -or
        $password -eq "change_this_password" -or
        $password -eq "YOUR_PASSWORD_FROM_SCHEMA_SQL") {
        throw "Database password is missing. Copy .env.example to .env, set DB_PASSWORD to the MySQL equipment_app password, then run .\run.ps1. See README.md."
    }
}

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "Java Development Kit 17+ is required. Install the JDK, reopen VS Code, and check: javac -version"
}

$version = "26.10.0"
$jar = Join-Path $PSScriptRoot "lib/mysql-connector-j-$version.jar"
if (-not (Test-Path $jar)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $jar) | Out-Null
    Write-Host "Downloading the MySQL JDBC driver from Maven Central (first run only)..."
    try {
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/$version/mysql-connector-j-$version.jar" -OutFile $jar -ErrorAction Stop
    } catch {
        throw "Could not download MySQL Connector/J. Connect to the internet and retry, or use Maven. Details: $($_.Exception.Message)"
    }
}

$sources = @(Get-ChildItem "src/main/java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
New-Item -ItemType Directory -Force -Path "build/classes" | Out-Null
& javac -encoding UTF-8 --release 17 -d "build/classes" $sources
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }
if ($CompileOnly) { Write-Host "Java compilation completed."; exit 0 }

if ($SmokeTest) {
    & javac -encoding UTF-8 --release 17 -cp "build/classes" -d "build/classes" "src/test/java/edu/cs26/equipment/ModelSmokeTest.java"
    if ($LASTEXITCODE -ne 0) { throw "Smoke test compilation failed." }
    & java -cp "build/classes;$jar" edu.cs26.equipment.ModelSmokeTest
    if ($LASTEXITCODE -ne 0) { throw "Smoke test failed." }
    exit 0
}

Import-LocalDbConfig

& java -cp "build/classes;$jar" edu.cs26.equipment.App
if ($LASTEXITCODE -ne 0) { throw "Application failed to start." }

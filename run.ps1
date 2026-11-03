param([switch]$CompileOnly, [switch]$SmokeTest)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$version = "26.10.0"
$jar = Join-Path $PSScriptRoot "lib/mysql-connector-j-$version.jar"
if (-not (Test-Path $jar)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $jar) | Out-Null
    Write-Host "Downloading the MySQL JDBC driver from Maven Central..."
    Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/$version/mysql-connector-j-$version.jar" -OutFile $jar
}
$sources = @(Get-ChildItem "src/main/java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
New-Item -ItemType Directory -Force -Path "build/classes" | Out-Null
& javac -encoding UTF-8 -d "build/classes" $sources
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed." }
if ($CompileOnly) { Write-Host "Java compilation completed."; exit 0 }
if ($SmokeTest) {
    & javac -encoding UTF-8 -cp "build/classes" -d "build/classes" "src/test/java/edu/cs26/equipment/ModelSmokeTest.java"
    if ($LASTEXITCODE -ne 0) { throw "Smoke test compilation failed." }
    & java -cp "build/classes;$jar" edu.cs26.equipment.ModelSmokeTest
    if ($LASTEXITCODE -ne 0) { throw "Smoke test failed." }
    exit 0
}
& java -cp "build/classes;$jar" edu.cs26.equipment.App
if ($LASTEXITCODE -ne 0) { throw "Application failed to start." }

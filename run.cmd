@echo off
setlocal
cd /d "%~dp0"
rem Build separately so Maven cannot consume the terminal's first input.
call mvn -q compile dependency:copy-dependencies -DincludeScope=runtime < nul
if errorlevel 1 exit /b %errorlevel%
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -cp "target\classes;target\dependency\*" app.Main --tui %*
) else (
    java -cp "target\classes;target\dependency\*" app.Main --tui %*
)
endlocal

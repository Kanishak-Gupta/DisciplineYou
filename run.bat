@echo off
echo ============================================
echo   DisciplineYou - Self Analysis App
echo ============================================
echo.

set JAVA_HOME=%~dp0tools\jdk-21.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=%~dp0tools\apache-maven-3.9.6\bin\mvn.cmd

echo Starting DisciplineYou...
call "%MVN%" -f "%~dp0pom.xml" compile javafx:run -q

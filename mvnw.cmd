@REM Maven wrapper script for Windows
@REM This allows running Maven with custom memory settings

@echo off
set MAVEN_OPTS=-Xmx512m
IF EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  "%MAVEN_HOME%\bin\mvn.cmd" %*
) ELSE (
  mvn %*
) 
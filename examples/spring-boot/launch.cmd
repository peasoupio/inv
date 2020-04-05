@echo off

if not exist "app1" (
    call git clone https://github.com/spring-guides/gs-spring-boot.git app1
)

if not exist "app2" (
    call git clone https://github.com/spring-guides/gs-spring-boot.git app2
)

if "%INV_HOME%"=="" (
    echo INV_HOME is not defined. Please define it before using this script
    exit /b 1
)

REM Since INV_HOME is defined, pattern points at the top level in the file hierarchy
call %INV_HOME%\inv.bat example/spring-boot/*.groovy




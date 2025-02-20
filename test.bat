@echo off
setlocal

:: Simulate create user
if /I "%1"=="createUser" (
    echo Creating user: %2
    echo %2 >> users.txt
    echo User created successfully
    exit /b 0
)

:: Simulate delete user
if /I "%1"=="deleteUser" (
    echo Deleting user: %2
    findstr /v /c:"%2" users.txt > temp.txt
    move /y temp.txt users.txt >nul
    echo User deleted successfully
    exit /b 0
)

:: Simulate list users
if /I "%1"=="getUsers" (
    type users.txt
    exit /b 0
)

:: Unknown operation
echo Unknown operation: %1
exit /b 1

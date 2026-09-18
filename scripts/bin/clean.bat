@echo off
echo.
echo [��Ϣ] ��������target����·����
echo.

%~d0
cd %~dp0

cd ../../backend
call mvn clean

pause
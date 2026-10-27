@echo off
rem Starts Expense Tracker as a normal window (no console). Run "gradlew.bat installDist" first.
set "JW=javaw"
if defined JAVA_HOME set "JW=%JAVA_HOME%\bin\javaw.exe"
start "" "%JW%" -Dfile.encoding=UTF-8 -cp "%~dp0build\install\expense-tracker\lib\*" com.example.expensetracker.MainKt

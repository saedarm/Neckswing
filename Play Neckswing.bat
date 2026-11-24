@echo off
setlocal
cd /d "%~dp0"
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if "%JAVA%"=="java" if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
  set "JAVA=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
  set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
if not exist "lwjgl3\build\libs\Neckswing.jar" (
  echo Building Neckswing the first time, this takes a minute...
  call gradlew.bat lwjgl3:dist
  if errorlevel 1 goto fail
)
"%JAVA%" -jar "lwjgl3\build\libs\Neckswing.jar" %*
if errorlevel 1 goto fail
goto :eof
:fail
echo.
echo Something went wrong. Need Java 17 or newer.
pause

@echo off
setlocal
echo Building Voice Magic for Minecraft 1.21.1 / NeoForge...
where java >nul 2>nul || (echo ERROR: Java is not installed or not on PATH.& exit /b 1)
where gradle >nul 2>nul || (echo ERROR: Gradle is not installed or not on PATH. Install Gradle 8.8+ and try again.& exit /b 1)
gradle build
if errorlevel 1 (echo Build failed. Review the Gradle errors above.& exit /b 1)
echo Build complete: build\libs\voicemagic-1.0.0.jar
pause

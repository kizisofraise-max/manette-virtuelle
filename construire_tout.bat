@echo off
title Construction de Manette Virtuelle
echo ==============================================
echo   Construction automatique de Manette Virtuelle
echo ==============================================
echo.

if not exist "%~dp0serveur_manette.py" goto manque

echo [1/4] Copie des fichiers dans C:\Manette ...
if not exist "C:\Manette" mkdir "C:\Manette"
for %%F in (serveur_manette.py installateur.iss icone.ico assistant_grand.bmp assistant_petit.bmp) do copy /Y "%~dp0%%F" "C:\Manette\" >nul
if exist "C:\Manette\ViGEmBus_Setup.exe" goto pilote_ok
for %%D in ("%~dp0." "%USERPROFILE%\OneDrive\Bureau\Manette" "%USERPROFILE%\Desktop\Manette" "%USERPROFILE%\Bureau\Manette") do if exist "%%~D\ViGEmBus_Setup.exe" copy /Y "%%~D\ViGEmBus_Setup.exe" "C:\Manette\" >nul
:pilote_ok
cd /d "C:\Manette"
if errorlevel 1 goto erreur

echo.
echo [2/4] Installation des outils Python ...
python -m pip install --upgrade pip
python -m pip install vgamepad websockets segno pyinstaller
if errorlevel 1 goto erreur

echo.
echo [3/4] Construction du programme (plusieurs minutes) ...
if exist "C:\Manette\build" rmdir /s /q "C:\Manette\build"
if exist "C:\Manette\dist" rmdir /s /q "C:\Manette\dist"
python -m PyInstaller --onefile --noconsole --clean --name ManetteVirtuelle --icon icone.ico --add-data "icone.ico;." --collect-all vgamepad serveur_manette.py
if errorlevel 1 goto erreur
echo OK : C:\Manette\dist\ManetteVirtuelle.exe est pret.

echo.
echo [4/4] Fabrication de l'installateur ...
if not exist "C:\Manette\ViGEmBus_Setup.exe" goto sans_pilote
set "ISCC="
for %%P in ("%LOCALAPPDATA%\Programs\Inno Setup 7\ISCC.exe" "%ProgramFiles(x86)%\Inno Setup 7\ISCC.exe" "%ProgramFiles%\Inno Setup 7\ISCC.exe" "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" "%ProgramFiles%\Inno Setup 6\ISCC.exe") do if exist %%P set "ISCC=%%~P"
if "%ISCC%"=="" goto sans_inno
"%ISCC%" "C:\Manette\installateur.iss"
if errorlevel 1 goto erreur
echo.
echo ==============================================
echo   TERMINE : C:\Manette\Sortie\ManetteVirtuelle_Installateur.exe
echo ==============================================
start "" "C:\Manette\Sortie"
pause
exit /b 0

:sans_pilote
echo.
echo Le programme est construit, mais ViGEmBus_Setup.exe est introuvable.
echo Copie-le dans C:\Manette puis relance ce fichier.
pause
exit /b 1

:sans_inno
echo.
echo Le programme est construit, mais Inno Setup est introuvable.
echo Ouvre C:\Manette\installateur.iss avec Inno Setup et appuie sur Ctrl+F9.
start "" "C:\Manette"
pause
exit /b 1

:manque
echo.
echo ERREUR : serveur_manette.py est introuvable a cote de ce fichier.
echo Extrais bien TOUT le contenu du fichier zip dans un dossier, puis relance.
pause
exit /b 1

:erreur
echo.
echo La construction a echoue. Fais une capture d'ecran de cette fenetre.
pause
exit /b 1

@echo off
REM Construit ManetteVirtuelle.exe (dossier dist\)
python -m pip install --upgrade pip
python -m pip install vgamepad websockets segno pyinstaller
if errorlevel 1 goto erreur

python -m PyInstaller --onefile --noconsole --clean --name ManetteVirtuelle --icon icone.ico --add-data "icone.ico;." --collect-all vgamepad serveur_manette.py
if errorlevel 1 goto erreur

echo.
echo OK : dist\ManetteVirtuelle.exe est pret.
echo Pour l'installateur : place ViGEmBus_Setup.exe a cote de installateur.iss,
echo puis compile installateur.iss avec Inno Setup.
pause
exit /b 0

:erreur
echo.
echo La construction a echoue.
pause
exit /b 1

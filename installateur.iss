; Script Inno Setup : installe Manette Virtuelle + le pilote ViGEmBus si absent.
; Fichiers necessaires dans le meme dossier :
;   dist\ManetteVirtuelle.exe   (produit par construire_tout.bat)
;   ViGEmBus_Setup.exe          (installeur officiel de ViGEmBus)
;   icone.ico, assistant_grand.bmp, assistant_petit.bmp  (apparence)

[Setup]
AppName=Manette Virtuelle
AppVersion=1.1
AppPublisher=Manette Virtuelle
DefaultDirName={autopf}\Manette Virtuelle
DefaultGroupName=Manette Virtuelle
OutputBaseFilename=ManetteVirtuelle_Installateur
OutputDir=Sortie
Compression=lzma2
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\ManetteVirtuelle.exe
WizardStyle=modern
SetupIconFile=icone.ico
WizardImageFile=assistant_grand.bmp
WizardSmallImageFile=assistant_petit.bmp

#if FileExists(CompilerPath + "Languages\French.isl")
[Languages]
Name: "french"; MessagesFile: "compiler:Languages\French.isl"
#endif

[Files]
Source: "dist\ManetteVirtuelle.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "ViGEmBus_Setup.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall; Check: PiloteAbsent

[Icons]
Name: "{group}\Manette Virtuelle"; Filename: "{app}\ManetteVirtuelle.exe"
Name: "{autodesktop}\Manette Virtuelle"; Filename: "{app}\ManetteVirtuelle.exe"

[Run]
; Pilote ViGEmBus (son propre assistant s'affiche), seulement s'il n'est pas deja installe
Filename: "{tmp}\ViGEmBus_Setup.exe"; StatusMsg: "Installation du pilote ViGEmBus..."; Flags: waituntilterminated; Check: PiloteAbsent
; Regle pare-feu : reseaux prives uniquement
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall add rule name=""Manette Virtuelle"" dir=in action=allow program=""{app}\ManetteVirtuelle.exe"" enable=yes profile=private"; Flags: runhidden
Filename: "{app}\ManetteVirtuelle.exe"; Description: "Lancer Manette Virtuelle"; Flags: postinstall nowait skipifsilent

[UninstallRun]
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall delete rule name=""Manette Virtuelle"""; Flags: runhidden; RunOnceId: "SupprRegleParefeu"

[Code]
function PiloteAbsent: Boolean;
begin
  Result := not RegKeyExists(HKLM, 'SYSTEM\CurrentControlSet\Services\ViGEmBus');
end;

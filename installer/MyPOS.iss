; Inno Setup script for MyPOS. Built by build-installer.bat (run it instead of compiling this by hand).
#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif

[Setup]
AppId={{B7F3A2E1-5C4D-4E8A-9F21-6D0C3A7B1E55}
AppName=MyPOS
AppVersion={#AppVersion}
DefaultDirName={autopf}\MyPOS
DisableProgramGroupPage=yes
DisableDirPage=yes
OutputDir=..\build\installer
OutputBaseFilename=MyPOS-Setup-{#AppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\MyPOS.exe
CloseApplications=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"
Name: "startup"; Description: "Start MyPOS automatically when Windows starts"; Flags: unchecked

[Files]
Source: "..\build\out\MyPOS\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion

[Icons]
Name: "{autoprograms}\MyPOS"; Filename: "{app}\MyPOS.exe"
Name: "{autodesktop}\MyPOS"; Filename: "{app}\MyPOS.exe"; Tasks: desktopicon
Name: "{autostartup}\MyPOS"; Filename: "{app}\MyPOS.exe"; Tasks: startup

[Run]
Filename: "{app}\MyPOS.exe"; Description: "Start MyPOS now"; Flags: nowait postinstall skipifsilent

; The customer's data lives in %APPDATA%\MyPOS. It is deliberately NOT listed here,
; so uninstalling or updating the program never deletes their sales data.

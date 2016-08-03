set TOPDIR=..
set SRCDIR=%TOPDIR%\src
set RUNDIR=%TOPDIR%\run
set SCRIPTDIR=%TOPDIR%\scripts

mkdir  %RUNDIR%\server 
mkdir  %RUNDIR%\server\helper
copy %TOPDIR%\server.log4j.properties %RUNDIR%\server
copy server.jar %RUNDIR%\server 
copy %SCRIPTDIR%\winhosts.txt %RUNDIR%\server 
copy %SCRIPTDIR%\jsl.exe %RUNDIR%\server 
copy %SCRIPTDIR%\jsl.ini %RUNDIR%\server 
copy OsNameArch.jar %RUNDIR%\server 

copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Server_2008_R2-amd64.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Server_2008_R2-x86.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Server_2008-amd64.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Server_2008-x86.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_7-amd64.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_7-x86.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Vista-amd64.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_Vista-x86.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_XP-amd64.exe
copy PMNative.exe %RUNDIR%\server\helper\PMWindows_XP-x86.exe

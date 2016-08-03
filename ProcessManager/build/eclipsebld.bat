set TOPDIR=..
set SRCDIR=%TOPDIR%\src

cl %SRCDIR%\pmserver\PMNative.c /c /Zi /nologo /W3 /WX- /Od /Oy- /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_UNICODE" /D "UNICODE" /EHsc /RTC1 /GS /fp:precise /Zc:wchar_t /Zc:forScope /Fa /Gd /analyze- /errorReport:none 

link PMNative.obj /OUT:"PMNative.exe" /INCREMENTAL:no /NOLOGO "kernel32.lib" "user32.lib" "gdi32.lib" "winspool.lib" "comdlg32.lib" "advapi32.lib" "shell32.lib" "ole32.lib" "oleaut32.lib" "uuid.lib" "odbc32.lib" "odbccp32.lib" /MANIFEST /DEBUG /SUBSYSTEM:CONSOLE /DYNAMICBASE:NO /NXCOMPAT /MACHINE:X86 /ERRORREPORT:NONE 

mkdir pmapi pmclient pmutils pmserver
copy ..\bin\pmapi\*.class pmapi
copy ..\bin\pmclient\*.class pmclient
copy ..\bin\pmutils\*.class pmutils
copy ..\bin\pmserver\*.class pmserver

call ant -file bld-api.xml
call ant -file bld-client.xml
call ant -file bld-server.xml
call ant -file bld-osname.xml

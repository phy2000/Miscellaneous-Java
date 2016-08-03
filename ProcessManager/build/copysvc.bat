set PATH=c:\UnxUtils\usr\local\wbin;%PATH%

set DESTDISK=C:
set DESTDIR=%DESTDISK%\ProcMgr\ServiceCopy
rmdir /s /q %DESTDIR%
mkdir %DESTDIR%\helper

copy ..\scripts\sendmail.* %DESTDIR%
copy ..\scripts\startmsg.txt %DESTDIR%
copy ..\scripts\restartmsg.txt %DESTDIR%
copy ..\scripts\runserver.bat %DESTDIR%
copy ..\run\server\jsl.* %DESTDIR%
copy ..\run\server\server.jar %DESTDIR%
copy ..\run\server\server.log4j.properties %DESTDIR%
copy ..\run\server\helper\*.exe %DESTDIR%\helper
copy OsNameArch.jar %DESTDIR%

%DESTDISK%
cd %DESTDIR%\..
zip -r ServiceCopy.zip ServiceCopy

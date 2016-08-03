set TOPDIR=..
set SRCDIR=%TOPDIR%\src
set RUNDIR=%TOPDIR%\run

mkdir %RUNDIR%\client
copy client.jar %RUNDIR%\client 

mkdir %RUNDIR%\api
copy PMAPI.jar %RUNDIR%\api
copy ..\lib\*.jar %RUNDIR%\api
copy ..\scripts\README-api.txt %RUNDIR%\api
copy ..\src\PMUser.java %RUNDIR%\api


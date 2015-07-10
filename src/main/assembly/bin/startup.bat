@echo off
set USERDIR=%cd%/../
set CLASSPATH=%CLASSPATH%;%USERDIR%/classes/;%USERDIR%/lib/*;

java -server -Xms512m -Xmx512m -classpath %CLASSPATH% -Duser.dir=%USERDIR% org.usc.check.in.AppMain &
pause
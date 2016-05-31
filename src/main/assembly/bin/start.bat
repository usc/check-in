@echo off & setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_51
set path=C:\Program Files\Java\jdk1.8.0_51\bin;%path%

if ""%1"" == ""debug"" goto debug
if ""%1"" == ""jmx"" goto jmx

java -classpath ..\conf;..\lib\* org.usc.check.in.AppMain
goto end

:debug
java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -classpath ..\conf;%LIB_JARS% org.usc.check.in.AppMain
goto end

:jmx
java -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -classpath ..\conf;%LIB_JARS% org.usc.check.in.AppMain

:end
pause

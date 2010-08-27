SET HOME=%~dp0

REM =============================================================================
REM F360 v2.5
REM =============================================================================
cd "C:\Program Files\Fortify Software\Fortify 360 v2.5.0 Demonstration Suite\bin"
call fortifyDemo.cmd start 360
cd %HOME%
sleep 2
call mvn clean
call mvn package -Dfortify.version=2.5
copy /y target\fortifyclient-2.5.jar ..\src\main\resources
cd "C:\Program Files\Fortify Software\Fortify 360 v2.5.0 Demonstration Suite\bin"
call fortifyDemo.cmd stop 360
cd %HOME%
sleep 10

REM =============================================================================
REM F360 v2.6
REM =============================================================================
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.0 Demonstration Suite\bin"
call fortifyDemo.cmd start 360
cd %HOME%
sleep 2
call mvn clean
call mvn package -Dfortify.version=2.6
copy /y target\fortifyclient-2.6.jar ..\src\main\resources
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.0 Demonstration Suite\bin"
call fortifyDemo.cmd stop 360
cd %HOME%
sleep 10

REM =============================================================================
REM F360 v2.6.1
REM =============================================================================
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.1 Demonstration Suite\bin"
call fortifyDemo.cmd start 360
cd %HOME%
sleep 2
call mvn clean
call mvn package -Dfortify.version=2.6.1
copy /y target\fortifyclient-2.6.1.jar ..\src\main\resources
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.1 Demonstration Suite\bin"
call fortifyDemo.cmd stop 360
cd %HOME%
sleep 10

REM =============================================================================
REM F360 v2.6.5
REM =============================================================================
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.5 Demonstration Suite\bin"
call fortifyDemo.cmd start 360
cd %HOME%
sleep 2
call mvn clean
call mvn package -Dfortify.version=2.6.5
copy /y target\fortifyclient-2.6.5.jar ..\src\main\resources
cd "C:\Program Files\Fortify Software\Fortify 360 v2.6.5 Demonstration Suite\bin"
call fortifyDemo.cmd stop 360
cd %HOME%
sleep 10
@echo off

set ssconfpath=SuiteSparse\SuiteSparse_config
set cspath=SuiteSparse\CXSparse
set amdpath=SuiteSparse\AMD
set btfpath=SuiteSparse\BTF
set colamdpath=SuiteSparse\COLAMD
set klupath=SuiteSparse\KLU

set sepath=.

set lib=-I %ssconfpath%^
 -I %cspath%\include^
 -I %amdpath%\include^
 -I %btfpath%\include^
 -I %colamdpath%\include^
 -I %klupath%\include^
 -I %sepath%

echo lib: %lib%

@echo on

@echo --- Compile config ---
gcc -c -std=c99 %lib% %ssconfpath%\SuiteSparse_config.c
 
@echo --- Compile CS ---
gcc -c -std=c99 %lib% %cspath%\source\*.c

@echo --- Compile AMD ---
gcc -c -std=c99 %lib% %amdpath%\source\*.c

@echo --- Compile BTF ---
gcc -c -std=c99 %lib% %btfpath%\source\*.c

@echo --- Compile COLAMD ---
gcc -c -std=c99 %lib% %colamdpath%\source\*.c
  
@echo --- Compile klu solve ---
gcc -c -std=c99 %lib% %klupath%\source\*.c

@echo --- Compile SE ---
g++ -c -std=c++11 %lib% %sepath%\*.cpp

@REM echo --- Compile demo ---
REM g++ -c -std=c++11 %lib% estimator.cpp

@del cs_convert.o

@echo --- Link ---
g++ *.o -o buildtest.exe

@echo --- Cleanup ---
@del *.o *.s

@REM se_bcse_kfe_demo

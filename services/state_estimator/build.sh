#!/bin/sh

ssconfpath="SuiteSparse/SuiteSparse_config"
cspath="SuiteSparse/CXSparse"
amdpath="SuiteSparse/AMD"
btfpath="SuiteSparse/BTF"
colamdpath="SuiteSparse/COLAMD"
klupath="SuiteSparse/KLU"
sepath="."

lib="-I $ssconfpath -I $cspath/Include -I $amdpath/Include -I $btfpath/Include -I $colamdpath/Include -I $klupath/Include -I $sepath"
echo lib: $lib

echo --- Compile config ---
gcc -c -std=c99 $lib $ssconfpath/SuiteSparse_config.c

echo --- Complie CS ---
gcc -c -std=c99 $lib $cspath/Source/*.c

echo --- Compile AMD ---
gcc -c -std=c99 $lib $amdpath/Source/*.c

echo --- Compile BTF ---
gcc -c -std=c99 $lib $btfpath/Source/*.c

echo --- Compile COLAMD ---
gcc -c -std=c99 $lib $colamdpath/Source/*.c

echo --- Compile KLU ---
gcc -c -std=c99 $lib $klupath/Source/*.c

echo --- Compile SE ---
g++ -c -std=c++11 $lib $sepath/*.cpp

#echo --- Compile demo ---

rm cs_convert.o

echo --- Link ---
g++ *.o -o buildtest.exe

echo --- Cleanup ---
rm *.o


# demo.exe

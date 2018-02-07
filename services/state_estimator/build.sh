#!/bin/sh

ssconfpath="SuiteSparse/SuiteSparse_config"
cspath="SuiteSparse/CXSparse"
amdpath="SuiteSparse/AMD"
btfpath="SuiteSparse/BTF"
colamdpath="SuiteSparse/COLAMD"
klupath="SuiteSparse/KLU"
sepath="."

amqpath="/usr/local/include/activemq-cpp-3.9.4"
aprpath="/usr/local/apr/include/apr-1"

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

#echo --- Compile ActiveMQ ---
#g++ -c -std=c++98 $lib -I$amqpath -I$aprpath $sepath/my_activeMQ.hpp

echo --- Compile SE ---
g++ -Wno-deprecated-declarations -c -std=c++11 $lib -I$amqpath -I$aprpath $sepath/estimator.cpp

#echo --- Compile demo ---

rm cs_convert.o

echo --- Link ---
g++ *.o -l activemq-cpp -l stdc++ -o amqtest.out

echo --- Cleanup ---
rm *.o


# demo.exe

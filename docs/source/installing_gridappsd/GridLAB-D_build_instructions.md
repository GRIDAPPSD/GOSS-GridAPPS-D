# GridLAB-D

## Overview

GridLAB-D is a steady-state Distribution System simulation tool. It solves full three phase unbalanced network power flows and provides highly detailed enduse load models. It is part of GridAPPS-D's Simulation Engine. It serves for providing the real world distribution system environment for third party  GridAPPS-D applications to monitor and control in real time.

## Source Code
GridLAB-D is maintained by Pacific Northwest National Laboratories in GitHub. The repository is located at https://github.com/gridlab-d/gridlab-d. GridAPPS-D uses the 4.0 release which is in release candidate currently and located on branch release/RC4.0.

## GridLAB-D Documentation
GridLAB-D's Documentation is located at http://gridlab-d.shoutwiki.com/wiki/Main_Page

## Building and Installing the Source
### Linux
#### Prerequisites
The following packages are needed in order to build GridLAB-D. 
```bash
:~$ sudo apt-get install \
gcc \
g++ \
automake \
libtool \
git
```

For GridAPPS-D GridLAB-D will need to be compiled with the FNCS shared Library so FNCS will need to be installed. For instructions on building and installing FNCS, please go [here](). For the purposes of this document the location of where you installed FNCS will be known as $FNCS_INSTALL.

#### Building GridLAB-D
For the purposes of this instruction set, the location to where you download the repository will be known as $GLD_INSTALL.
```bash
#download the release/RC4.0 branch repository
:$GLD_INSTALL$ git clone https://github.com/gridlab-d/gridlab-d.git -b release/RC4.0 --single-branch
#build and install xerces located in the third_party folder of the repository
:$GLD_INSTALL$ cd gridlab-d/third_party
:$GLD_INSTALL/gridlab-d/third_party$ tar -xzf xerces-c-3.1.1.tar.gz
:$GLD_INSTALL/gridlab-d/third_party$ cd xerces-c-3.1.1
:$GLD_INSTALL/gridlab-d/third_party/xerces-c-3.1.1$ ./configure
:$GLD_INSTALL/gridlab-d/third_party/xerces-c-3.1.1$ sudo make
:$GLD_INSTALL/gridlab-d/third_party/xerces-c-3.1.1$ sudo make install
#build and install GridLAB-D with FNCS
:$GLD_INSTALL/gridlab-d/third_party/xerces-c-3.1.1$ cd ../../
:$GLD_INSTALL/gridlab-d$ autoreconf -if
:$GLD_INSTALL/gridlab-d$ ./configure --prefix=$GLD_INSTALL/install --with-fncs=$FNCS_INSTALL --enable-silent-rules 'CFLAGS=-g -O0 -w' 'CXXFLAGS=-g -O0 -w' 'LDFLAGS=-g -O0 -w'
#before performing make. Make sure the envirionment variable $LD_LIBRARY_PATH contains the path $FNCS_INSTALL/lib if it doesn't then it will need to be added to $LD_LIBRARY_PATH
:$GLD_INSTALL/gridlab-d$ make
:$GLD_INSTALL/gridlab-d$ install
```

#### Environment Setup
In order for GridAPPS-D to be able to run GridLAB-D The following environment variables need to be setup:
* $PATH must contain $GLD_INSTALL/install/bin and $FNCS_INSTALL/bin
* $GLPATH must contain $GLD_INSTALL/install/lib/gridlabd and $GLD_INSTALL/install/share/gridlabd
* $CXXFLAGS must contain $GLD_INSTALL/install/share/gridlabd
* $LD_LIBRARY_PATH must contain $FNCS_INSTALL/lib

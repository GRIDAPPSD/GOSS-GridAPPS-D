# FNCS

## Overview
FNCS is the co-simulation engine used by GridAPP-D's simulation manager class to facilitating real-time synchonization and message passing between the GridLAB-D simulation and the GOSS message bus.

## Source Code
FNCS is maintained by PNNL. The repository is located at https://github.com/FNCS/fncs. GridAPPS-D is using the latest release of FNCS which is v2.3.2.

## FNCS Documentation
The documentation for FNCS is located at https://github.com/FNCS/fncs/wiki.

## Building and Installing the Source
### Linux
#### Prerequisites
FNCS requires both the ZeroMQ and CZMQ libraries. For the purposes of the tutorial FNCS and it's prerequisites will be installed a custom location refered to by $FNCS_INSTALL. All source code is downloaded to the $HOME directory.

```bash
# download and install ZeroMQ
:~$ wget http://download.zeromq.org/zeromq-3.2.4.tar.gz
# if you do not have wget, use
# curl -O http://download.zeromq.org/zeromq-3.2.4.tar.gz

# unpack zeromq, change to its directory
:~$ tar -xzf zeromq-3.2.4.tar.gz
:~$ cd zeromq-3.2.4

# configure, make, and make install 
:~/zeromq-3.2.4$ ./configure --prefix=$FNCS_INSTALL
:~/zeromq-3.2.4$ make
:~/zeromq-3.2.4$ make install

# download and install CZMQ
:~/zeromq-3.2.4$ cd $HOME

:~$ wget http://download.zeromq.org/czmq-3.0.0-rc1.tar.gz
# if you do not have wget, use
# curl -O http://download.zeromq.org/czmq-3.0.0-rc1.tar.gz

# unpack czmq, change to its directory
:~$ tar -xzf czmq-3.0.0-rc1.tar.gz
:~$ cd czmq-3.0.0

# configure, make, and make install 
:~/czmq-3.0.0$ ./configure --prefix=$FNCS_INSTALL --with-libzmq=$FNCS_INSTALL
:~/czmq-3.0.0$ make
:~/czmq-3.0.0$ make install
```

#### Building FNCS
In this tutorial FNCS source code will be downloaded using git to the $HOME directory. The code will be installed at t/FNCShe loacation $FNCS_INSTALL.
```bash
# download FNCS
:~$ git clone https://github.com/FNCS/fncs.git

# change to FNCS directory
:~$ cd fncs

# configure, make, and make install 
:~/fncs$ ./configure --prefix=$FNCS_INSTALL --with-zmq=$FNCS_INSTALL
:~/fncs$ make
:~/fncs$ make install
```

#### Environment Setup
In order for GridAPPS-D to be able to run FNCS and for GridLAB-D to be built with FNCS The following environment variables need to be setup:
* $PATH must contain $FNCS_INSTALL/bin
* $LD_LIBRARY_PATH must contain $FNCS_INSTALL/lib
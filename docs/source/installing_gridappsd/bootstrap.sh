#prereq should have a user called gridappsd that everthing is expected to run under

#bootstrap - launch this script as root


#----------------
# prereq setup
#----------------
apt update -y 

apt install -y vim git mysql-server automake default-jdk g++ gcc python python-pip libtool apache2 gradle pip install stomp.py

pip install --upgrade pip
pip install stomp.py


su - gridappsd

mkdir gridappsd_project

mkdir gridappsd_project/sources

mkdir gridappsd_project/builds

export GRIDAPPSD_INSTALL=/home/gridappsd/gridappsd_project

#----------------
# fncs setup
#----------------
export FNCS_INSTALL=$GRIDAPPSD_INSTALL/builds/test

cd /tmp
wget http://download.zeromq.org/zeromq-3.2.4.tar.gz
tar -xzf zeromq-3.2.4.tar.gz
cd zeromq-3.2.4
./configure --prefix=$FNCS_INSTALL 
make
make install

cd /tmp
wget http://download.zeromq.org/czmq-3.0.0-rc1.tar.gz
tar -xzf czmq-3.0.0-rc1.tar.gz
cd czmq-3.0.0
./configure –prefix=$FNCS_INSTALL –with-libzmq=$FNCS_INSTALL
make
make install

cd $GRIDAPPSD_INSTALL/sources
git clone https://github.com/FNCS/fncs.git
cd fncs
./configure –prefix=$FNCS_INSTALL –with-zmq=$FNCS_INSTALL
make
make install

export PATH=$PATH:$FNCS_INSTALL/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$FNCS_INSTALL/lib

#----------------
# gridlab-d setup
#----------------
cd $GRIDAPPSD_INSTALL/sources
git clone https://github.com/gridlab-d/gridlab-d.git -b release/RC4.0 --single-branch
cd gridlab-d/third_party
tar -xzf xerces-c-3.1.1.tar.gz
cd xerces-c-3.1.1 
./configure
sudo make
sudo make install
cd ../../

autoreconf -if
./configure --prefix=$GLD_INSTALL/install --with-fncs=$FNCS_INSTALL --enable-silent-rules 'CFLAGS=-g -O0 -w' 'CXXFLAGS=-g -O0 -w' 'LDFLAGS=-g -O0 -w'
make
install

#----------------
# goss gridapps-d setup
#----------------
cd $GRIDAPPSD_INSTALL/sources

git clone https://github.com/GRIDAPPSD/GOSS-GridAPPS-D.git
cd GOSS-GridAPPS-D
./build-goss-test.sh

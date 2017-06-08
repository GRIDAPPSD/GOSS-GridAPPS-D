
You will need to clone the GOSS-GridAPPS-D and viz repositories and build each


git clone https://github.com/GRIDAPPSD/GOSS-GridAPPS-D.git

cd GOSS-GridAPPS-D

./build-goss-test.sh

mkdir -p $GRIDAPPSD_INSTALL/builds/test/log


git clone https://github.com/GRIDAPPSD/viz.git

cd viz 

npm install


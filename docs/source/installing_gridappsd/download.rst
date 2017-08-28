
You will need to clone the GOSS-GridAPPS-D and viz repositories and build each

- GOSS-GridAPPS-D
    - git clone https://github.com/GRIDAPPSD/GOSS-GridAPPS-D.git
    - cd GOSS-GridAPPS-D
    - ./build-goss-test.sh
    - mkdir -p $GRIDAPPSD_INSTALL/builds/log
- Vizualization
    - git clone https://github.com/GRIDAPPSD/viz.git
    - cd viz 
    - npm install
    - webpack
- Blazegraph
	- wget https://downloads.sourceforge.net/project/bigdata/bigdata/2.1.1/blazegraph.jar -O $GRIDAPPSD_INSTALL/builds/lib/blazegraph.jar


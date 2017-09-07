export GRIDAPPSD_PROJECT=/home/gridappsd/gridappsd_project
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GRIDAPPSD_PROJECT/builds/lib:/usr/local/lib
export FNCS_LOG_LEVEL="DEBUG4"

cd $GRIDAPPSD_PROJECT/sources/viz/
node server.js >> $GRIDAPPSD_PROJECT/builds/log/nodeserver.log 2>&1 &
NJSPID=`echo $!`

cd $GRIDAPPSD_PROJECT/builds/

java -Dbigdata.propertyFile=$GRIDAPPSD_PROJECT/builds/conf/rwstore.properties -jar blazegraph.jar >> $GRIDAPPSD_PROJECT/builds/log/blazegraph.log 2>&1 &
BGPID=`echo $!`


java -jar lib/run.bnd.jar >> $GRIDAPPSD_PROJECT/builds/log/goss-gridappsd.log 2>&1


echo "Stopping blazegraph, process $BGPID"
kill $BGPID

echo "Stopping node server, process $NJSPID"
kill $NJSPID

exit
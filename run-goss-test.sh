export GRIDAPPSD_PROJECT=/home/gridappsd/gridappsd_project
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$GRIDAPPSD_PROJECT/builds/lib:/usr/local/lib
export FNCS_LOG_LEVEL="DEBUG4"

cd $GRIDAPPSD_PROJECT/builds/lib/
java -Dfelix.cm.home=$GRIDPPAD_PROJECT/builds/test/conf/ -Dfelix.system.properties=$GRIDAPPSD_PROJECT/builds/conf/config.properties -jar run.bnd.jar >> $GRIDAPPSD_PROJECT/builds/log/goss-gridappsd.log 2>&1

exit(0)
cd pnnl.goss.gridappsd/
gradle export
#copy built jar to dist directory
cp generated/distributions/executable/run.bnd.jar ~/gridappsd_project/builds/test/lib/

cp scripts/fncs_goss_bridge.py ~/gridappsd_project/builds/test/scripts/
cp scripts/zipload_schedule.player ~/gridappsd_project/builds/test/scripts/

cp ~/gridappsd_project/sources/fncs/python/fncs.py ~/gridappsd_project/builds/test/scripts/
cp -R ~/gridappsd_project/sources/GOSS-GridAPPS-D/applications ~/gridappsd_project/builds/test/
#copy conf to dist directory
#cp -R conf ~/gridappsd_project/builds/test/lib/goss/

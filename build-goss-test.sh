cd gov.pnnl.goss.gridappsd/
gradle export
#copy built jar to dist directory
cp generated/distributions/executable/run.bnd.jar ~/gridappsd_project/builds/lib/

mkdir -p ~/gridappsd_project/builds/scripts/
#cp applications/python/fncs_goss_bridge.py ~/gridappsd_project/builds/scripts/
#cp applications/etc/zipload_schedule.player ~/gridappsd_project/builds/scripts/

#cp ~/gridappsd_project/sources/fncs/python/fncs.py ~/gridappsd_project/builds/scripts/
cp -R ~/gridappsd_project/sources/GOSS-GridAPPS-D/applications ~/gridappsd_project/builds/
cp -R ~/gridappsd_project/sources/GOSS-GridAPPS-D/services ~/gridappsd_project/builds/
#copy conf to dist directory
#cp -R conf ~/gridappsd_project/builds/

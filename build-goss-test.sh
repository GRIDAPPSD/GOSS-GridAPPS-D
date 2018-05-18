cd gov.pnnl.goss.gridappsd/
gradle clean
gradle export
#copy built jar to dist directory
cp generated/distributions/executable/run.bnd.jar ~/gridappsd_project/builds/lib/

mkdir -p ~/gridappsd_project/builds/scripts/

cp -R ~/gridappsd_project/sources/GOSS-GridAPPS-D/applications ~/gridappsd_project/builds/
cp -R ~/gridappsd_project/sources/GOSS-GridAPPS-D/services ~/gridappsd_project/builds/


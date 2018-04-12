cd gov.pnnl.goss.gridappsd/
gradle clean
gradle export
#copy built jar to docker
docker cp generated/distributions/executable/run.bnd.jar gridappsddocker_gridappsd_1:/gridappsd/lib/run.bnd.jar

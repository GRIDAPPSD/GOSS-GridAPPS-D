./gradlew clean
rm -rf gov.pnnl.goss/gridappsd/generated
./gradlew export
#cd gov.pnnl.goss.gridappsd/

#copy built jar to docker
echo "copying run.bnd.jar into container"
docker cp gov.pnnl.goss.gridappsd/generated/distributions/executable/run.bnd.jar gridappsddocker_gridappsd_1:/gridappsd/lib/run.bnd.jar

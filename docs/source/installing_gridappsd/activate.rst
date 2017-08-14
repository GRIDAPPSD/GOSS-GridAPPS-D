You will need to populate the mysql database with the ieee8500 model

wget https://github.com/GRIDAPPSD/Bootstrap/raw/master/gridappsd_mysql_dump.sql

mysql -u root -p < gridappsd_mysql_dump.sql




To populate Blazegraph with the ieee8500 model
    - Download https://github.com/GRIDAPPSD/Powergrid-Models/blob/master/CIM/ieee8500.xml
    - java -Dbigdata.propertyFile=$GRIDAPPSD_INSTALL/builds/lib/conf/rwstore.properties -jar $GRIDAPPSD_INSTALL/builds/lib/blazegraph.jar >> $GRIDAPPSD_INSTALL/builds/log/blazegraph.log 2>&1 &
    - Go to http://localhost:9999 
    - Click on the Update tab
    - Choose the ieee8500 model file and change the format to RDF/XML 
    - Click Update


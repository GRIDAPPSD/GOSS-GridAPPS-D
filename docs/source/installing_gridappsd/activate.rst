You will need to populate the mysql database with the ieee8500 model

wget https://github.com/GRIDAPPSD/Bootstrap/raw/master/gridappsd_mysql_dump.sql
mysql -u root -p < gridappsd_mysql_dump.sql

If the bootstrap doesn't work, or you wish to install manually you will need the following prerequisites.

apt upgrade -y  (as root user)

GridAPPS-D Dependencies -  Use apt install for the following dependencies 

*apt install -y vim git mysql-server automake default-jdk g++ gcc python python-pip libtool apache2 gradle nodejs-legacy npm curl*


-	vim
-	Git
-	Mysql-server    (I set the root pw as gridappsd1234)
-	Automake
-	Default-jdk
-	G++
-	Gcc
-	Python  (v 2.x)
-	Python-pip
-	Libtool
-	Apache2
-	Gradle
-	nodejs-legacy
-	npm
-	curl


 
Then apply the following pip installs

*pip install --upgrade pip*

*pip install stomp.py*
*pip install pyyaml*

- pip install --upgrade pip
- pip install stomp.py
- pip install pyyaml

As well as the following npm packages

- npm install -g express
- npm install -g ejs
- npm install -g typescript
- npm install -g typings
- npm install -g webpack

 The following structure should be set up to enable the run scripts to execute correctly.
 
 -	Griddapps-project
    -	builds/
    -	sources/

 

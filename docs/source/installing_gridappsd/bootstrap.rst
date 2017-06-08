It is recommended to start with a linux platform such as Ubuntu and a 'gridappsd' user created.  The bootstrap scripts should be run as root.

::
	
    apt install -y git   (you may need to run apt update first)
    git clone https://github.com/GRIDAPPSD/Bootstrap.git
    cd Bootstrap
    chmod a+x *.sh
    ./bootstrap.sh


1. Download or clone the repository from github 
       a. Install github desktop https://desktop.github.com/ or sourcetree https://www.atlassian.com/software/sourcetree/overview and Clone the GOSS Powergrid repository (https://github.com/GRIDAPPSD/GOSS-GridAPPS-D)
       b. Or download the source (https://github.com/GRIDAPPSD/GOSS-GridAPPS-D/archive/master.zip)
#. Install java 1.8 SDK and set JAVA_HOME variable
#. Install Eclipse http://www.eclipse.org/downloads/packages/release/Mars/1 (Mars 4.5.1 or earlier, 4.5.2 appears to have bugs related to bundle processing) TODO what about neon?
#. Open eclipse with workspace set to powergrid download location, eg. C:\Users\username\Documents\GOSS-GridAPPS-D
#. Install BNDTools plugin: Help->Install New Software->Work with: http://dl.bintray.com/bndtools/bndtools/3.0.0 and Install Bndtools 3.0.0 or earlier
#. Import projects into workspace 
       a. File->Import General->Existing Projects into workspace 
       b. Select root directory, powergrid download location 
       c. Select cnf, pnnl.goss.powergrid, pnnl.goss.gridappsd
#. If errors are detected, Right click on the gridappsd project and select release, then release all bundles
#. If you would like to you a local version of GOSS-Core (Optional) 
       a. Update cnf/ext/repositories.bnd
       b. Select source view and add the following as the first line
       c. aQute.bnd.deployer.repository.LocalIndexedRepo;name=GOSS Local Release;local=/GOSS-Core2/cnf/releaserepo;pretty=true,
       d. verify by switching to bndtools and verify that there are packages under GOSS Local Relase
#. Open pnnl.goss.gridappsd/bnd.bnd, Rebuild project, you should not have errors
#. Open pnnl.goss.gridappsd/run.bnd.bndrun and click Run OSGI

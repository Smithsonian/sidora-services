# SIservices - Microservices Execution Platform

## Installation of ServiceMix
### See the ServiceMix [Quickstart](http://servicemix.apache.org/docs/5.0.x/quickstart/index.html) guide [install section](http://servicemix.apache.org/docs/5.0.x/quickstart/installation.html) for help 
### See the SIdora ServiceMix [Installation](https://confluence.si.edu/display/SIDKB/Install+and+Configure+ServiceMix) and [Configuration](https://confluence.si.edu/display/SIDKB/Configure+ServiceMix+for+SIdora+Services) confluence pages
> Tested with ServiceMix Versions: 6.1.0

### Steps:
1. Download and extract ServiceMix (http://servicemix.apache.org/)
 - SMX_HOME = Directory of ServiceMix after downloaded and extracted
2. Download GitHub files
  - GH_DIR = Directory of contents of GitHub on local file system
3. Folders to copy (and over write when required)
  - Copy the `Input` folder from `GH_DIR/Routes/Camera Trap` to `SMX_HOME` folder
4. Generate the Sidora service artifacts by running the maven commands
  - Go to the GH_DIR and run mvn clean package.  (Note: The automated unit testing on /Components/FedoraRepo requires Fedora service running as of today.  You may want to skip the unit test for FedoraRepo module)
5. Copy SIdora beans to th ServiceMix deploy directory (and over write when required)
  - Copy the `CameraTrap-(version).jar` from `GH_DIR/Beans/CameraTrap/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `Excel-(version).jar` from `GH_DIR/Beans/Excel/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `VelocityToolsHandler-(version).jar` from `GH_DIR/Beans/VelocityToolsHandler/target/` to the `SMX_HOME/deploy/` folder
6. Copy SIdora Camel Components to the ServiceMix deploy directory
  - Copy the `Extractor-(version).jar` from `GH_DIR/Components/Extractor/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `FedoraRepo-(version).jar` from `GH_DIR/Components/FedoraRepo/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `FcrepoRest-(version).jar` from `GH_DIR/Components/FcrepoRest/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `Reader-(version).jar` from `GH_DIR/Components/Reader/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `Thumbnailator-(version).jar` from `GH_DIR/Components/Thumbnailator/target/` to the `SMX_HOME/deploy/` folder
7. Copy SIdora Tabular Services to the ServiceMix deploy directory
  - Copy the `excel2tabular-translator-(version).jar` from `GH_DIR/Tabular/excel2tabular-translator/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `tabular-metadata-generator-(version).jar` from `GH_DIR/Tabular/tabular-metadata-generator/target/` to the `SMX_HOME/deploy/` folder
  - Copy the `tabular-metadata-cxf-services-SMX-blueprint-(version).jar` from `GH_DIR/Tabular/tabular-metadata-cxf-blueprint/target/` to the `SMX_HOME/deploy/` folder
8. Copy SIdora Camel Routes to the ServiceMix deploy directory
  - Copy the `camera-trap-route.xml` from `GH_DIR/Routes/Camera Trap/Route/` to the `SMX_HOME/deploy/` folder
  - Copy the `wcs-route.xml` from `GH_DIR/Routes/WCS/Route/` to the `SMX_HOME/deploy/` folder
  - Copy the `derivatives-route.xml` from `GH_DIR/Routes/Derivatives/Route/` to the `SMX_HOME/deploy/` folder
9. Edit SMX_HOME/etc/system.properties and add following properties for Fedora and CT route settings
  - si.fedora.host = URL of the Fedora repository
  - si.fedora.user = User that have rights to ingest data
  - si.fedora.password = Password for the user defined above
  - si.ct.owner = CT owner username
  - si.ct.namespace = CT namespace
  - si.ct.root = PID of CT root object
10. Create SMX_HOME/etc/edu.si.sidora.karaf.cfg properties file and add following properties for the SCBI and WCS CT routes.  More details on properties can be found on the SIdora ServiceMix Confluence page.  
  - si.ct.wcs.dryrun = false
  - si.ct.scbi.dryrun = false
  - si.ct.thread.wait.time = 6000
11. Add following dependencies to the ServiceMix by running the commands from the SMX client console
  - smx@fedora> feature:install camel-exec # Use feature vs features for SMX 6.x
  - smx@fedora> feature:install camel-groovy
  - smx@fedora> feature:install camel-velocity
  - smx@fedora> feature:install camel-saxon
  - smx@fedora> feature:install camel-schematron
  - smx@fedora> feature:install camel-csv
  - smx@fedora> bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.poi/3.11_1
  - smx@fedora> bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.velocity-tools/2.0_1
  - smx@fedora> bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.spring-beans/3.2.14.RELEASE_1
  - smx@fedora> bundle:install mvn:mvn:commons-codec/commons-codec/1.10
  - smx@fedora> bundle:install mvn:commons-codec/commons-codec/1.10
  - smx@fedora> bundle:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-httpclient/3.1_7
  - smx@fedora> bundle:install mvn:com.google.guava/guava/18.0
12. Start ServiceMix
  - Linux: SMX_HOME/bin/servicemix 
  - Windows: SMX_HOME/bin/servicemix.bat
- To stop ServiceMix enter `logout` in the ServiceMix command prompt

### Successful Installation
- Folder called `Process` will be created in the SMX_HOME directory
- ServiceMix command `route-list` should have 3 Contexts (CTIngestCamelContext, DerivativesCamelContext, WCSIngestCamelContext) with routes that have the status _'Started'_

### Trouble Shutting Down
- The commands `log:display-exception` or the alias `lde` will display the last exception that occurred within ServiceMix.
- If no error and no routes are listed, enter the command `la | grep camera-trap-route.xml` if it returns something like `[ 196][Active ][GracePeriod][  ][ 80] camera-trap-route.xml (0.0.0)`
    - Then if `la | grep camel-velocity` returns nothing. It means Camel-Velocity was not started automatically
    - Enter the command `features:install camel-velocity` to install and start the Camel-Velocity feature
    - The camera-trap-route.xml should go from 'GracePeriod' to 'Created'

## Camera Trap Execution
To process Camera Trap data copy the data archive (e.g. p1d246.tar.gz) into the `SMX_HOME/Process` folder. That is it! To monitor the execution process at the ServiceMix command prompt enter `log:tail` and watch the log output.

#### Helpful ServiceMix Commands
- `log:display` -- To display the log file
- `log:tail` -- To watch the log file (ctrl-c to stop watching)
- `route-list` -- To display the current active Camel Routes
- `logout` -- To shutdown ServiceMix

## Building 
- Install Apache Maven version 3.X (http://maven.apache.org/)
- Open a command line terminal
- Navigate to the root directory of source code file
- Run the command `mvn install`
    - Note: Some Integration Test look for Fedora Repository Host at localhost with the default username and password
- Or run the command `mvn install -DskipTests` to build without running Unit and Integration Tests    
- Newly built jars will need to be moved from their individual `target` folders to the `SM_HOME/deploy/` folder

> Note: SIservices is built with Maven 3

### Version
0.4.4

Copyright 2015 Smithsonian Institution.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.You may obtain a copy of
the License at: http://www.apache.org/licenses/

This software and accompanying documentation is supplied without
warranty of any kind. The copyright holder and the Smithsonian Institution:
(1) expressly disclaim any warranties, express or implied, including but not
limited to any implied warranties of merchantability, fitness for a
particular purpose, title or non-infringement; (2) do not assume any legal
liability or responsibility for the accuracy, completeness, or usefulness of
the software; (3) do not represent that use of the software would not
infringe privately owned rights; (4) do not warrant that the software
is error-free or will be maintained, supported, updated or enhanced;
(5) will not be liable for any indirect, incidental, consequential special
or punitive damages of any kind or nature, including but not limited to lost
profits or loss of data, on any basis arising from contract, tort or
otherwise, even if any of the parties has been warned of the possibility of
such loss or damage.

This distribution includes several third-party libraries, each with their own
license terms. For a complete copy of all copyright and license terms, including
those of third-party libraries, please see the product release notes.

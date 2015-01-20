# SI - Service Execution Environment (SI-See)
## Installation of ServiceMix
See the ServiceMix [Quickstart](http://servicemix.apache.org/docs/5.0.x/quickstart/index.html) guide [install section](http://servicemix.apache.org/docs/5.0.x/quickstart/installation.html) for help 
>Note: ServiceMix 5.3.0 prints out an error about an "Unknown signal: HUP" but does not seem to cause an acutal project

### Steps:
1. Download and extract ServiceMix (http://servicemix.apache.org/)
 - SM_HOME = Directory of ServiceMix after downloaded and extracted
2. Download GitHub files
  - GH_DIR = Directory of contents of GitHub on local file system
3. Folders to copy (and over write when required)
  1. Copy the `Input` folder from `GH_DIR/Routes/Camera Trap` to `SM_HOME` folder
4. Files to copy (and over write when required)
  1. Copy the 4 `jar` files in the `GH_DIR/Binaries/deploy/` to the `SM_HOME/deploy/` folder
  2. Copy the `camera-trap-route.xml` from `GH_DIR/Routes/Camera Trap/Route/` to the `SM_HOME/deploy/` folder
  3. Copy the files from the `GH_DIR/Configurations/etc/` to the `SM_HOME/etc/`
5. Edit SM_HOME/etc/system.properties for Fedora instance
  1. si.fedora.host = URL of the Fedora repository
  2. si.fedora.user = User that have rights to ingest data
  3. si.fedora.password = Password for the user defined above
6. Start ServiceMix
  - Linux: SM_HOME/bin/servicemix 
  - Windows: SM_HOME/bin/servicemix.bat
- To stop ServiceMix enter `logout` in the ServiceMix command prompt

### Successful Installation
- Folder called `Process` will be created in the SM_HOME directory
- ServiceMix command `route-list` should have 9 _CameraTrap*_ routes that have the status _'Started'_

### Trouble Shutting Issues
- The commands `log:display-exception` or the alias `lde` will display the last exception that occurred within ServiceMix.
- If not error and not routes listed, enter the command `la | grep camera-trap-route.xml` if it returns something like `[ 196][Active ][GracePeriod][  ][ 80] camera-trap-route.xml (0.0.0)`
    - Then if `la | grep camel-velocity` returns nothing. It means Camel-Velocity was not started automatically
    - Enter the command `features:install camel-velocity` to install and start the Camel-Velocity feature
    - The camera-trap-route.xml should go from 'GracePeriod' to 'Created'

## Camera Trap Execution
To process Camera Trap data copy the data archive (e.g. p1d246.tar.gz) into the `SM_HOME/Process` folder. That is it! To monitor the execution process at the ServiceMix command prompt enter `log:tail` and watch the log output.

#### Helpful ServiceMix Commands
- `log:display` -- To display the log file
- `log:tail` -- To watch the log file (ctrl-C to stop watching)
- `route-list` -- To display the current active Camel Routes
- `logout` -- To shutdown ServiceMix

> Tested with ServiceMix Versions: 5.0.1, 5.3.0

## Building 
- Install Apache Maven version 3.X (http://maven.apache.org/)
- Open a command line terminal
- Navigate to the root directory of source code file
- Run the command `mvn install`
    - Note: Some Integration Test look for Fedora Repository Host at localhost with the default username and password
- Or run the command `mvn install -DskipTests` to build without running Unit and Integration Tests    
- Newly built jars will need to be moved from there `target` folders to the `SM_HOME/deploy/` folder

> Note: SI-See is built with Maven 3 (although Maven 2 should work [untested])

### Version
1.0.0
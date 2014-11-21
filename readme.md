# SI - Service Execution Environment (SI-See)

## Installation of ServiceMix

### Steps:
1. Download and extract ServiceMix
 - SM_HOME = Directory of ServiceMix after downloaded and extracted (generally apache-servicemix-5.3.0)
2. Download GitHub files
  - GH_DIR = Directory of contents of GitHub on local file system
3. Folders to copy/move
  1. GH_DIR/Routes/Camera Trap/Input/ --> SM_HOME/
4. Files to copy/move (and over write when required)
  1. GH_DIR/Binaries/deploy/* --> SM_HOME/deploy/
  2. GH_DIR/Routes/Camera Trap/Route/camera-trap-route.xml --> SM_HOME/deploy/
  3. GH_DIR/Configurations/etc/* to SM_HOME/etc/
5. Edit SM_HOME/etc/system.properties for Fedora instance
  1. si.fedora.host = URL of the Fedora repository
  2. si.fedora.user = User that have rights to ingest data
  3. si.fedora.password = Password for the user defined above
6. Start ServiceMix
  - Linux: SM_HOME/bin/servicemix 
  - Windows: SM_HOME/bin/servicemix.bat
- To stop ServiceMix enter `logout` in the ServiceMix command prompt

### Successfull Installation
- Folder called _'Process'_ will be created in the SM_HOME directory
- ServiceMix command `route-list` should have 9 _CameraTrap*_ routes that have the status _'Started'_

## Camera Trap Execution
To process Camera Trap data copy the data archive (e.g. p1d246.tar.gz) into the `SM_HOME/Process` folder. That is it! To monitor the execution process at the ServiceMix command prompt enter _log:tail_ and watch the log output.

#### Helpful ServiceMix Commands
- `log:display` -- To display the log file
- `log:tail` -- To watch the log file (ctrl-C to stop watching)
- `route-list` -- To display the current active Camel Routes
- `logout` -- To shutdown ServiceMix

##### Notes: 
Tested with ServiceMix Version 5.3.0

### Version
1.0.0
# Wildlife Insights

### Table of contents
* [Prerequisites](#prerequisites)
* [Configuration](#configuration)
    * [Install Python and Modules](#install-python-and-modules)
* [Face Blur Python Script](#face-blur-python-script)
    * [Test Face Blur Script](#test-face-blur-script)
    * [Face Blur For CT Ingest Routes and Derivatives Routes](#face-blur-for-ct-ingest-routes-and-derivatives-routes)
* [Legacy Data Update Script](#legacy-data-update-script)
    * [Properties File](#properties-file)
    * [Update Script](#update-script)
        * [Usage](#usage)
        * [Running An Update](#running-an-update)
        * [Generate A List Of Deployments](#generate-a-list-of-deployments)
        * [Update Deployments From File](#update-deployments-from-file)

## Prerequisites
* Oracle Java 8 (see [Java 8 Installation and Configuration](https://confluence.si.edu/display/SIDKB/Java+8+Installation+and+Configuration)).
* Apache Maven version 3.X (http://maven.apache.org/)
* Git (https://git-scm.com/)
* Python
* FITS and FITS web service (see [Install File Information Tool Set (FITS v1.0.4) and FITS Web Service (FITSservlet v1.1.3)](https://confluence.si.edu/pages/viewpage.action?pageId=3735601))
* Exiftool (provided by FITS)

## Configuration

### Install Python and Modules

```
sudo yum install python36 opencv-python python36-requests python36-pip python36-devel

# run as fedora user
pip3 install opencv-python lxml --user
```


## Face Blur Python Script


#### Test Face Blur Script

```
cat testFaceBlur.jpg | python FaceBlurrer.py 99 ./haarcascades/haarcascade_frontalface_alt.xml > output.jpg
```


## Face Blur For CT Ingest Routes and Derivatives Routes:

The type of classifier used for face detection is set via property file `<smx-home>/etc/edu.si.emammal.cfg` and the property `si.ct.wi.faceBlur.classifier = {{karaf.home}}/FaceBlurrer/haarcascades/haarcascade_frontalface_alt.xml`

There are several different haarcascade files under `<smx-home>/FaceBlurrer/haarcascades/`

There is also a property for the GaussianBlur value `si.ct.wi.faceBlur.blur_value = 99`



## Legacy Data Update Script:

### Deploy the Update Script:

The update script can be run from a clone of sidora-services git repo or by copying `<git-home>/sidora-services/Routes/Wildlife-Insights` directory. Python and python packages will need to be installed see [Install Python and Modules](#install-python-and-modules)

> NOTE: If using dry-run, datastream changes will be saved locally so adequate disk space is needed. 
 
```
cp <git-home>/sidora-services/Routes/Wildlife-Insights <some-install-path>

mkdir -p <some-install-path>/Wildlife-Insights/output 
```


### Properties File:

To configure various settings edit the `update.properties` file.
   

### Update Script

> **:warning: update script must have the properties file and FaceBlurrer directory on its path** 

> **:warning: running with `-dr` or `--dry-run` param will save updated datastreams to local file system under `output` directory instead of updating the fedora datastreams**


#### Usage:
```
python3 UpdateLegacyCT.py -h
usage: UpdateLegacyCT.py [-h] [-i INFILE] [-o OUTFILE]
                         [-ds {FGDC,OBJ,FITS,SIDORA} [{FGDC,OBJ,FITS,SIDORA} ...]]
                         [-v] [-dr [DRY_RUN]] [-V [VALIDATE]]
                         [-dA [DELETEALL]] [-G [GROUP_RESOURCES]]

Update Legacy CameraTrap Data for WildLife Insights

optional arguments:
  -h, --help            show this help message and exit
  -i INFILE, --infile INFILE
                        file name containing list of deployment pids
  -o OUTFILE, --outfile OUTFILE
                        output file name for list of deployment pids (default
                        is deploymentList.csv)
  -ds {FGDC,OBJ,FITS,SIDORA} [{FGDC,OBJ,FITS,SIDORA} ...], --datastreams {FGDC,OBJ,FITS,SIDORA} [{FGDC,OBJ,FITS,SIDORA} ...]
                        datastreams to update i.e. FGDC, FITS, OBJ, SIDORA
  -v, --verbose         increase output verbosity
  -dr [DRY_RUN], --dry-run [DRY_RUN]
                        Store datastream changes to local file system instead
                        of updating Fedora datastreams (default dir ./output)
  -V [VALIDATE], --validate [VALIDATE]
                        Enable Validation
  -dA [DELETEALL], --deleteAll [DELETEALL]
                        Remove all existing files from output dir.
  -G [GROUP_RESOURCES], --group_resources [GROUP_RESOURCES]
                        Group dry-run datastream output to individual
                        directories
```

#### Running An Update:

> **:warning: update script must have the properties file and FaceBlurrer directory on its path** 

> :warning: if no infile is specified a list of deployments will be automatically generated and saved to deploymentList.csv
> Datastreams must be provided 

```
python3 UpdateLegacyCT.py -ds FGDC FITS OBJ SIDORA
```

#### Generate A List Of Deployments:

```
python3 UpdateLegacyCT.py

# or specify the outfile name
python3 UpdateLegacyCT.py --dry-run -o deploymentPids.txt
```


#### Update Deployments From file: 

```
python3 UpdateLegacyCT.py -i <deployment-pid-file> -ds <datastreams-2-update>

ex. python3 UpdateLegacyCT.py -v -i pidList.txt -ds FGDC OBJ FITS SIDORA
```



# TODO:

- [X] does blur change image metadata?
    - opencv does not preserve image metadata.
- [X] update legacy data
    - [X] walk ct hierarchy over fuseki ri 
        - [X] use file or create file with list of pids to update
    - [X] script for blurring legacy images
        - [X] use file or create file with list of pids to update  
    - [X] script to update FITS for all images
        - [X] use file or create file with list of pids to update
- [X] exec compiled python vs python source performance? 
- [ ] document FITS changes 
    - [ ] how to identify FITS output matches FGDC make/model 
    - [X] FITS config changes 
- [ ] when blurring image remove version and set non versionable on successful blur


 
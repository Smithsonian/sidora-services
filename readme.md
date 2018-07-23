# SI Services - Microservices Execution Platform

### Table of contents
* [Prerequisites](#prerequisites)
* [Configuration of Fedora Repository and MySQL for Sidora Services](#configuration-of-fedora-repository-and-mysql-for-sidora-services)
    * [Fedora Repository Configuration](#fedora-repository-configuration)
    * [MySQL Configuration for Batch and Service Requests](#mysql-configuration-for-batch-and-service-requests)
* [Cloning and Building Sidora Services](#cloning-and-building-sidora-services)
* [Configure and Deploy Sidora Services](#configure-and-deploy-sidora-services)

## Prerequisites
* Oracle Java 8 (see [Java 8 Installation and Configuration](https://confluence.si.edu/display/SIDKB/Java+8+Installation+and+Configuration)).
* Apache Maven version 3.X (http://maven.apache.org/)
* Git (https://git-scm.com/)
* ServiceMix 6.1.2 (see [Installation of ServiceMix](https://confluence.si.edu/display/SIDKB/Install+and+Configure+ServiceMix))
* MySQL (see [MySQL Installation and Configuration for Fedora](https://confluence.si.edu/display/SIDKB/MySQL+Installation+and+Configuration+for+Fedora))
* Fedora Repository (see [Install and Configure the Fedora Repository](https://confluence.si.edu/display/SIDKB/Install+and+Configure+the+Fedora+Repository))
* FITS and FITS web service (see [Install File Information Tool Set (FITS v1.0.4) and FITS Web Service (FITSservlet v1.1.3)](https://confluence.si.edu/pages/viewpage.action?pageId=3735601))
* SWFTools (see [Install the SWFTools](https://confluence.si.edu/display/SIDKB/Install+the+SWFTools))
* FFMPEG (see [FFMPEG Install](https://confluence.si.edu/display/SIDKB/FFMPEG+Install))


## Configuration of Fedora Repository and MySQL for Sidora Services

### Fedora Repository Configuration:

* Add a new Fedora user used by camel processes:
    ```bash
    # vi <fedora-install>/server/config/fedora-users.xml

    <?xml version='1.0' ?>Add
      <users>
        ...
        <user name="<fedora-camel-user>" password="<fedora-camel-user-password">
          <attribute name="fedoraRole">
            <value>administrator</value>
          </attribute>
        </user>
        ...
      </users>

    ```

* Add a new ActiveMQ Queue `edanIds.apim.update` for EDAN/IDS camel processes:
    ```bash
    # vi <fedora-install>/server/config/spring/activemq.xml

    ...
    <amq:destinationInterceptors>
      <amq:virtualDestinationInterceptor>
        <amq:virtualDestinations>
          <amq:compositeQueue name="fedora.apim.update">
            <amq:forwardTo>
              <amq:queue physicalName="sidora.apim.update"/>
              <amq:queue physicalName="solr.apim.update"/>
              <amq:queue physicalName="edanIds.apim.update"/>
            </amq:forwardTo>
          </amq:compositeQueue>
        </amq:virtualDestinations>
      </amq:virtualDestinationInterceptor>
    </amq:destinationInterceptors>
    ...
    ```

### MySQL Configuration for Batch and Service Requests:

The example below is for a test server where the MySQL database is co-located with Fedora.
    - Create a MySQL database for Sidora Camel Batch & Service Requests with the required user and privileges.
    - When using MySQL, it may be required to create the following tables. It need only be done once.
    - Verify the version of MySQL you are using and create the tables for your version of MySQL.

```bash
# mysql -u root -p

mysql> CREATE DATABASE IF NOT EXISTS sidora;
mysql> GRANT ALL ON sidora.* TO <camel-user>@localhost IDENTIFIED BY '<camel-secret>';
```

<p>
<details>
<summary>MySQL 5.7 (Click to expand)</summary>

```bash
############################################################################
# MySQL 5.7
############################################################################

# sql that creates the table for the Sidora Camel Service Requests

mysql>  DROP TABLE IF EXISTS sidora.camelRequests;

mysql>  CREATE TABLE IF NOT EXISTS sidora.camelRequests (
correlationId CHAR(36) NOT NULL,
exchangeId CHAR(255) NOT NULL,
messageId CHAR(255) NOT NULL,
camelId CHAR(255) NOT NULL,
headers blob NOT NULL,
body blob NOT NULL,
request_consumed BOOLEAN NOT NULL DEFAULT false,
request_complete BOOLEAN NOT NULL DEFAULT false,
error_processing TEXT,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (correlationId));

# sql that creates two table for the batch process

mysql> DROP TABLE IF EXISTS sidora.camelBatchRequests;

mysql> CREATE TABLE IF NOT EXISTS sidora.camelBatchRequests (
correlationId CHAR(36) NOT NULL,
parentId TEXT NOT NULL,
resourceFileList TEXT NOT NULL,
ds_metadata TEXT NOT NULL,
ds_sidora TEXT NOT NULL,
association TEXT NOT NULL,
resourceOwner TEXT NOT NULL,
codebookPID TEXT DEFAULT NULL,
resourceCount INT(10) NULL,
processCount INT(10) NOT NULL DEFAULT 0,
request_consumed BOOLEAN NOT NULL DEFAULT false,
request_complete BOOLEAN NOT NULL DEFAULT false,
created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (correlationId));

mysql> DROP TABLE IF EXISTS sidora.camelBatchResources;

mysql> CREATE TABLE IF NOT EXISTS sidora.camelBatchResources (
correlationId CHAR(36) NOT NULL,
resourceFile TEXT NOT NULL,
parentId TEXT NOT NULL,
pid TEXT DEFAULT NULL,
contentModel TEXT DEFAULT NULL,
resourceOwner TEXT NOT NULL,
titleLabel TEXT DEFAULT NULL,
resource_created BOOLEAN NOT NULL DEFAULT false,
ds_relsExt_created BOOLEAN NOT NULL DEFAULT false,
ds_metadata_created BOOLEAN NOT NULL DEFAULT false,
ds_sidora_created BOOLEAN NOT NULL DEFAULT false,
ds_dc_created BOOLEAN NOT NULL DEFAULT false,
ds_obj_created BOOLEAN NOT NULL DEFAULT false,
ds_tn_created BOOLEAN NOT NULL DEFAULT false,
codebook_relationship_created BOOLEAN NOT NULL DEFAULT false,
parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false,
resource_consumed BOOLEAN NOT NULL DEFAULT false,
resource_complete BOOLEAN NOT NULL DEFAULT 0,
created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);
```
</details>
</p>

<p>
<details>
<summary>MySQL 5.1 (Click to expand)</summary>

```bash
############################################################################
# MySQL 5.1 has issues with two columns with "DEFAULT CURRENT_TIMESTAMP"
############################################################################

# sql that creates the table for the Sidora Camel Service Requests

mysql> DROP TABLE IF EXISTS sidora.camelRequests;

mysql> CREATE TABLE IF NOT EXISTS sidora.camelRequests (
correlationId CHAR(36) NOT NULL,
exchangeId CHAR(255) NOT NULL,
messageId CHAR(255) NOT NULL,
camelId CHAR(255) NOT NULL,
headers blob NOT NULL,
body blob NOT NULL,
request_consumed BOOLEAN NOT NULL DEFAULT false,
request_complete BOOLEAN NOT NULL DEFAULT false,
error_processing TEXT,
created TIMESTAMP DEFAULT '0000-00-00 00:00:00',
updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (correlationId));


# sql that creates two table for the batch process

mysql> DROP TABLE IF EXISTS sidora.camelBatchRequests;

mysql> CREATE TABLE IF NOT EXISTS sidora.camelBatchRequests (
correlationId CHAR(36) NOT NULL,
parentId TEXT NOT NULL,
resourceFileList TEXT NOT NULL,
ds_metadata TEXT NOT NULL,
ds_sidora TEXT NOT NULL,
association TEXT NOT NULL,
resourceOwner TEXT NOT NULL,
codebookPID TEXT DEFAULT NULL,
resourceCount INT(10) NULL,
processCount INT(10) NOT NULL DEFAULT 0,
request_consumed BOOLEAN NOT NULL DEFAULT false,
request_complete BOOLEAN NOT NULL DEFAULT false,
created TIMESTAMP DEFAULT '0000-00-00 00:00:00',
updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (correlationId));

mysql> DROP TABLE IF EXISTS sidora.camelBatchResources;

mysql> CREATE TABLE IF NOT EXISTS sidora.camelBatchResources (
correlationId CHAR(36) NOT NULL,
resourceFile TEXT NOT NULL,
parentId TEXT NOT NULL,
pid TEXT DEFAULT NULL,
contentModel TEXT DEFAULT NULL,
resourceOwner TEXT NOT NULL,
titleLabel TEXT DEFAULT NULL,
resource_created BOOLEAN NOT NULL DEFAULT false,
ds_relsExt_created BOOLEAN NOT NULL DEFAULT false,
ds_metadata_created BOOLEAN NOT NULL DEFAULT false,
ds_sidora_created BOOLEAN NOT NULL DEFAULT false,
ds_dc_created BOOLEAN NOT NULL DEFAULT false,
ds_obj_created BOOLEAN NOT NULL DEFAULT false,
ds_tn_created BOOLEAN NOT NULL DEFAULT false,
codebook_relationship_created BOOLEAN NOT NULL DEFAULT false,
parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false,
resource_consumed BOOLEAN NOT NULL DEFAULT false,
resource_complete BOOLEAN NOT NULL DEFAULT false,
created_date TIMESTAMP DEFAULT '0000-00-00 00:00:00',
updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);
```
</details>
</p>


```bash
mysql> FLUSH PRIVILEGES;

mysql> EXIT;
```

<p>
<details>
<summary>Commands for Checking the MySQL user, database, and tables (Click to expand)</summary>

```bash
# mysql -u root -p

mysql> SELECT User FROM mysql.user;
mysql> SHOW DATABASES;
mysql> USE sidora;
mysql> SELECT DATABASE();
mysql> SHOW TABLES;
mysql> DESCRIBE camelBatchRequests;
mysql> DESCRIBE camelBatchResources;
```
##### Example Output:
```bash
mysql> SELECT User FROM mysql.user;
+-------------------+
| User              |
+-------------------+
| camelBatchUser    |
| fedora            |
| mysql.sys         |
| root              |
+-------------------+
4 rows in set (0.05 sec)

mysql> SHOW DATABASES;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| sidora             |
| fedora3            |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
6 rows in set (0.00 sec)

mysql> USE sidora;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed

mysql> SELECT DATABASE();
+------------+
| DATABASE() |
+------------+
| sidora     |
+------------+
1 row in set (0.00 sec)

mysql> SHOW TABLES;
+----------------------+
| Tables_in_sidora |
+----------------------+
| camelBatchRequests   |
| camelBatchResources  |
+----------------------+
2 row in set (0.00 sec)

mysql> DESCRIBE camelBatchRequests;
+------------------+------------+------+-----+-------------------+-----------------------------+
| Field            | Type       | Null | Key | Default           | Extra                       |
+------------------+------------+------+-----+-------------------+-----------------------------+
| correlationId    | char(36)   | NO   | PRI | NULL              |                             |
| parentId         | text       | NO   |     | NULL              |                             |
| resourceFileList | text       | NO   |     | NULL              |                             |
| ds_metadata      | text       | NO   |     | NULL              |                             |
| ds_sidora        | text       | NO   |     | NULL              |                             |
| association      | text       | NO   |     | NULL              |                             |
| resourceOwner    | text       | NO   |     | NULL              |                             |
| codebookPID      | text       | YES  |     | NULL              |                             |
| resourceCount    | int(10)    | YES  |     | NULL              |                             |
| processCount     | int(10)    | NO   |     | 0                 |                             |
| request_consumed | tinyint(1) | NO   |     | 0                 |                             |
| request_complete | tinyint(1) | NO   |     | 0                 |                             |
| created          | timestamp  | NO   |     | CURRENT_TIMESTAMP |                             |
| updated          | datetime   | YES  |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
+------------------+------------+------+-----+-------------------+-----------------------------+
14 rows in set (0.03 sec)

mysql> DESCRIBE camelBatchResources;
+--------------------------------------------+------------+------+-----+-------------------+-----------------------------+
| Field                                      | Type       | Null | Key | Default           | Extra                       |
+--------------------------------------------+------------+------+-----+-------------------+-----------------------------+
| correlationId                              | char(36)   | NO   |     | NULL              |                             |
| resourceFile                               | text       | NO   |     | NULL              |                             |
| parentId                                   | text       | NO   |     | NULL              |                             |
| pid                                        | text       | YES  |     | NULL              |                             |
| contentModel                               | text       | YES  |     | NULL              |                             |
| resourceOwner                              | text       | NO   |     | NULL              |                             |
| titleLabel                                 | text       | YES  |     | NULL              |                             |
| resource_created                           | tinyint(1) | NO   |     | 0                 |                             |
| ds_relsExt_created                         | tinyint(1) | NO   |     | 0                 |                             |
| ds_metadata_created                        | tinyint(1) | NO   |     | 0                 |                             |
| ds_sidora_created                          | tinyint(1) | NO   |     | 0                 |                             |
| ds_dc_created                              | tinyint(1) | NO   |     | 0                 |                             |
| ds_obj_created                             | tinyint(1) | NO   |     | 0                 |                             |
| ds_tn_created                              | tinyint(1) | NO   |     | 0                 |                             |
| codebook_relationship_created              | tinyint(1) | NO   |     | 0                 |                             |
| parent_child_resource_relationship_created | tinyint(1) | NO   |     | 0                 |                             |
| resource_consumed                          | tinyint(1) | NO   |     | 0                 |                             |
| resource_complete                          | tinyint(1) | NO   |     | 0                 |                             |
| created_date                               | timestamp  | NO   |     | CURRENT_TIMESTAMP |                             |
| updated_date                               | datetime   | YES  |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
+--------------------------------------------+------------+------+-----+-------------------+-----------------------------+
20 rows in set (0.00 sec)
```
</details>
</p>



## Cloning and Building Sidora Services

> __Note:__
The build process allows for a maven profile with a property `smx.home.path` to point to an existing ServiceMix installation where existing properties are used for generating new configuration and property files.
If the `smx.home.path` property is not set then configuration and property files will be generated from properties in `./sidora-service/test.properties`

>#### All files needed for deploying the Sidora Services will be located in `<fedora-home>/git/sidora-service/KAR/target` where `<fedora-home>` is `/home/fedora`

```bash
mkdir <fedora-home-dir>/git
cd <fedora-home>/git

git clone https://github.com/Smithsonian/sidora-services.git

cd <fedora-home>/git/sidora-services

#To quickly build and generate artifacts, KAR, configuration, and property files skipping all tests and intigration tests
mvn clean install -DskipTests=true -Dcargo.maven.skip=true -Dsmx.home.path=<smx-home-path>

#For a full build running all tests and intigration tests
# All tests will use properties from ./sidora-service/test.properties
# Config files needed for deployment will use properties from the smx.home.path and will be generated to <fedora-home>/git/sidora-service/KAR/target
mvn clean install -Dsmx.home.path=<smx-home-path>
```


## Configure and Deploy Sidora Services

### Prerequisites:
* [Cloning and Building Sidora Services](#cloning-and-building-sidora-services)

> INFO:
As of Sidora 0.4.5, we have switched to ServiceMix 6.1.2 with Java 8. This is the only tested configuration for this version

> __*** New Installs ***__
To prevent errors when deploying to ServiceMix its a good idea to verify the property files in the `<local-git-repo>/sidora-service/KAR/target` first before copying the configuration and properties to ServiceMix and before deploying the KAR.

>The current Karaf provisioning used for deploying and during the build process will include all the configuration, and resource files that are needed and will create and/or populate a configuration in ServiceMix when the KAR is deployed. If any of the configuration, or resource files are already present at the desired location they are kept and the deployment of that configuration file is skipped, as an already existing file might contain customization.

> __*** Updating Only ***__
Property, configuration, and resource files (steps 1 - 3) are only needed for new installs and can be skipped.

#### __*** All files needed for deploying the Sidora Services will be located in `<git-local>/sidora-service/KAR/target` ***__

### Step-by-step guide:

1. Edit `<SMX_HOME>/etc/system.properties`:
    1. Make a copy of the original `system.properties` file
        ```bash
        cd <SMX_HOME>/etc/
        cp system.properties system.properties.orig
        ```

    2. Validate and add the following properties to `<SMX_HOME>/etc/system.properties`. The properties below are also generated during the build and can be found at `<git-local>/sidora-service/KAR/target/system.properties`

        >__DO NOT COPY the generated `<git-local>/sidora-service/KAR/target/system.properties` file to `<smx-home>/etc` as the `<smx-home>/etc/system.properties` file contains additional properties not in the generated `<git-local>/sidora-service/KAR/target/system.properties`__

        ```bash
        #
        # The properties defined in this file will be made available through system
        # properties at the very beginning of the Karaf's boot process.
        #
        si.fedora.host=http://localhost:8080/fedora
        si.fedora.user=<fedora-camel-user>
        si.fedora.password=<secret>
        # For Camera Trap routes
        si.ct.owner=forresterT
        si.ct.namespace=ct
        si.ct.root=si:121909

        # A fuseki endpoint with datastore location included path (i.e. http://localhost:9080/fuseki/fedora3)
        # required
        si.fuseki.endpoint=http://localhost:9080/fuseki/fedora3

        si.fits.host=http://localhost:8080/fits-1.1.3

        workbench.server = workbench.sidora.si.edu
        workbench.host = https://${workbench.server}
        ```

2. Add the following additional properties files to `<SMX_HOME>/etc` making sure all properties are set correctly before copying to `<smx-home>/etc`

    1. Copy the following configuration files from the git repository
        ```bash
        cp <local-git-repo>/sidora-services/KAR/target/etc/edu.si.sidora.karaf.cfg <SMX_HOME>/etc/
        cp <local-git-repo>/sidora-services/KAR/target/etc/edu.si.sidora.batch.cfg <SMX_HOME>/etc/
        cp <local-git-repo>/sidora-services/KAR/target/etc/edu.si.sidora.mci.cfg <SMX_HOME>/etc/
        cp <local-git-repo>/sidora-services/KAR/target/etc/edu.si.sidora.emammal.cfg <SMX_HOME>/etc/
        ```

4. (OPTIONAL) Update `org.ops4j.pax.logging.cfg` to format the ServiceMix log entries and/or enable MDC logging.  You may find additional information about Karaf MDC logging in [Camel and Servicemix / Karaf MDC logging](https://confluence.si.edu/pages/viewpage.action?pageId=7373291).  Our instances utilize MDC logging to differentiate Camel Context IDs from individual Camera Trap ingest pipelines in separate Camel Contexts.

    <p>
    <details>
    <summary>Sample org.ops4j.pax.logging.cfg (Click to expand)</summary>

    ```bash
    ################################################################################
    #
    #    Licensed to the Apache Software Foundation (ASF) under one or more
    #    contributor license agreements.  See the NOTICE file distributed with
    #    this work for additional information regarding copyright ownership.
    #    The ASF licenses this file to You under the Apache License, Version 2.0
    #    (the "License"); you may not use this file except in compliance with
    #    the License.  You may obtain a copy of the License at
    #
    #       http://www.apache.org/licenses/LICENSE-2.0
    #
    #    Unless required by applicable law or agreed to in writing, software
    #    distributed under the License is distributed on an "AS IS" BASIS,
    #    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    #    See the License for the specific language governing permissions and
    #    limitations under the License.
    #
    ###############################################################################
    # Root logger
    log4j.rootLogger=INFO, out, osgi:VmLogAppender
    log4j.throwableRenderer=org.apache.log4j.OsgiThrowableRendere
    # To avoid flooding the log when using DEBUG level on an ssh connection and doing log:tail
    log4j.logger.org.apache.sshd.server.channel.ChannelSession = INF
    # CONSOLE appender not used by default
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | ContextId: %X{camel.contextId} | RouteId: %X{camel.routeId} | %m%
    # File appender
    log4j.appender.out=org.apache.log4j.RollingFileAppender
    log4j.appender.out.layout=org.apache.log4j.PatternLayout
    log4j.appender.out.layout.ConversionPattern=%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | ContextId: %X{camel.contextId} | RouteId: %X{camel.routeId} | %m%n
    log4j.appender.out.file=${karaf.data}/log/servicemix.log
    log4j.appender.out.append=true
    log4j.appender.out.maxFileSize=1MB
    log4j.appender.out.maxBackupIndex=1
    # Sift appender
    log4j.appender.camel-mdc=org.apache.log4j.sift.MDCSiftingAppender
    log4j.appender.camel-mdc.key=camel.contextId
    log4j.appender.camel-mdc.default=servicemix
    log4j.appender.camel-mdc.appender=org.apache.log4j.RollingFileAppender
    log4j.appender.camel-mdc.appender.layout=org.apache.log4j.PatternLayout
    log4j.appender.camel-mdc.appender.layout.ConversionPattern=%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | ContextId: %X{camel.contextId} | RouteId: %X{camel.routeId} | %m%n
    log4j.appender.camel-mdc.appender.file=${karaf.data}/log/$\\{camel.contextId\\}.log
    log4j.appender.camel-mdc.appender.append=true
    log4j.appender.camel-mdc.appender.maxFileSize=1MB
    log4j.appender.camel-mdc.appender.maxBackupIndex=1
    # SI services individual log config
    log4j.logger.edu.si.derivatives = INFO
    log4j.logger.edu.si.ctingest = INFO
    log4j.logger.edu.si.uctingest = INFO
    log4j.logger.edu.si.wcsingest = INFO
    log4j.logger.edu.si.services = INFO
    ```

    </details>
    </p>

4. Deploy the Karaf Archive.  This method is preferred in a stable environment such as test and production since the dependencies installation will be automated.
    1.  Copy the `sidora-deployment-<version>.kar` artifact to the `<SMX_HOME>/deploy` directory.  The karaf archive (kar) contains a file called `feature.xml` to describe the necessary bundles and features to deploy the artifact and also includes the necessary dependencies within the packaging as JARs.  `<GIT_HOME>` refers to where you have the sidora-services git repository checked out and have run the maven build from.
        ```bash
        cp <GIT_HOME>/sidora-services/KAR/target/sidora-deployment-1.0.kar <SMX_HOME>/deploy/
        ```

        Here is an example `feature.xml` that describes the required SIdora Servies dependencies and artifacts.

        <p>
        <details>
        <summary>feature.xml  (Click to expand)</summary>

        ```bash
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <features xmlns="http://karaf.apache.org/xmlns/features/v1.2.1" name="edu.si.sidora.smx.bundles">
            <repository>mvn:io.hawt/hawtio-karaf/1.5.2/xml/features</repository>
            <feature name="sidora-dependencies" version="1.0" start-level="50">
                <bundle dependency="true">mvn:commons-codec/commons-codec/1.10</bundle>
                <bundle dependency="true">mvn:org.apache.commons/commons-collections4/4.1</bundle>
                <bundle dependency="true">mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.poi/3.15_1</bundle>
                <bundle dependency="true">mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.velocity-tools/2.0_1</bundle>
                <bundle dependency="true">mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.spring-beans/3.2.14.RELEASE_1</bundle>
                <bundle dependency="true">mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-httpclient/3.1_7</bundle>
                <bundle dependency="true">mvn:com.google.guava/guava/18.0</bundle>
                <bundle dependency="true">mvn:commons-dbcp/commons-dbcp/1.4</bundle>
                <bundle dependency="true">wrap:mvn:mysql/mysql-connector-java/5.1.6</bundle>
                <bundle dependency="true">mvn:org.apache.tika/tika-core/1.13</bundle>
            </feature>
            <feature name="edan-ids-dependencies" version="1.0" start-level="50">
                <bundle dependency="true">mvn:org.apache.httpcomponents/httpcore-osgi/4.4.4</bundle>
                <bundle dependency="true">mvn:org.apache.httpcomponents/httpclient-osgi/4.5.3</bundle>
            </feature>
            <feature name="sidora-camel-dependencies" version="2.16.3" start-level="50">
                <feature version="2.16.3">camel-exec</feature>
                <feature version="2.16.3">camel-groovy</feature>
                <feature version="2.16.3">camel-velocity</feature>
                <feature version="2.16.3">camel-saxon</feature>
                <feature version="2.16.3">camel-schematron</feature>
                <feature version="2.16.3">camel-csv</feature>
                <feature version="2.16.3">camel-aws</feature>
                <feature version="2.16.3">camel-http</feature>
                <feature version="2.16.3">camel-jdbc</feature>
                <feature version="2.16.3">camel-sql</feature>
                <feature version="2.16.3">camel-gson</feature>
                <feature version="2.16.3">camel-http4</feature>
                <feature>hawtio-core</feature>
                <feature>webconsole</feature>
                <feature>activemq-web-console</feature>
            </feature>
            <feature name="sidora-deployment" version="1.0" description="SI :: Services :: Karaf Archive">
                <details>The SIdora Services Karaf Archive deployment artifact</details>
                <bundle start-level="80">mvn:edu.si.services.beans/CameraTrap/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.beans/Excel/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.beans/VelocityToolsHandler/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.camel/Extractor/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.camel/FcrepoRest/1.0.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.camel/FedoraRepo/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.camel/Reader/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.camel/Thumbnailator/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.sidora/excel2tabular-translator/0.0.1-SNAPSHOT</bundle>
                <bundle start-level="80">mvn:edu.si.sidora/tabular-metadata-cxf-services-SMX-blueprint/0.0.1-SNAPSHOT</bundle>
                <bundle start-level="80">mvn:edu.si.sidora/tabular-metadata-generator/0.0.1-SNAPSHOT</bundle>
                <bundle start-level="80">mvn:edu.si.services/sidora-batch/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services/sidora-mci/1.0</bundle>
                <bundle start-level="80">mvn:edu.si.services.beans/edansidora/1.0</bundle>
            </feature>
        </features>
        ```
        </details>
        </p>

5. Copy the sidora-services camel routes to the ServiceMix deploy directory.
    ```bash
    cp <GIT_HOME>/sidora-services/KAR/target/sidora-dbatch.xml <SMX_HOME>/deploy/
    cp <GIT_HOME>/sidora-services/KAR/target/unified-camera-trap-route.xml <SMX_HOME>/deploy/
    cp <GIT_HOME>/sidora-services/KAR/target/derivatives-route.xml <SMX_HOME>/deploy/
    ```

Copyright 2015-2016 Smithsonian Institution.

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
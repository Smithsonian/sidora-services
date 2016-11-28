# Sidora-REST Service Setup and Configuration:
### A new Fedora user must be created for Derivatives to be created during Batch Process:
add camelBatchProcess user to `fedora-users.xml`
```bash
# vi <fedora-install>/server/config.fedora-users.xml

<?xml version='1.0' ?>
  <users>
    ...
    <user name="<fedora-batch-user>" password="<fedora-batch-user-password">
      <attribute name="fedoraRole">
        <value>administrator</value>
      </attribute>
    </user>
    ...
  </users>

```

## MySQL setup and configuration:
The example below is for a test server where the MySQL database is co-located with Fedora.
- Create a MySQL database for the Camel Batch Service with the required user and privileges.
- When using MySQL, it may be required to create the following tables. It need only be done once.
- Verify the version of MySQL you are using and create the tables for your version of MySQL.

```bash
# mysql -u root -p

mysql> CREATE DATABASE IF NOT EXISTS camelBatch;
mysql> GRANT ALL ON camelBatch.* TO <camel-batch-user>@localhost IDENTIFIED BY '<camel-batch-secret>';

############################################################################
## sql that creates two table for the batch process
# MySQL 5.7
############################################################################

#sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, resourceOwner TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, request_consumed BOOLEAN NOT NULL DEFAULT false, request_complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

#sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT DEFAULT NULL, resourceOwner TEXT NOT NULL, titleLabel TEXT DEFAULT NULL, resource_created BOOLEAN NOT NULL DEFAULT false, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, ds_tn_created BOOLEAN NOT NULL DEFAULT false, codebook_relationship_created BOOLEAN NOT NULL DEFAULT false, parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false, resource_consumed BOOLEAN NOT NULL DEFAULT false, resource_complete BOOLEAN NOT NULL DEFAULT 0, created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);


############################################################################
## sql that creates two table for the batch process
# MySQL 5.1 has issues with two columns with "DEFAULT CURRENT_TIMESTAMP"
############################################################################

sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, resourceOwner TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, request_consumed BOOLEAN NOT NULL DEFAULT false, request_complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT DEFAULT NULL, resourceOwner TEXT NOT NULL, titleLabel TEXT DEFAULT NULL, resource_created BOOLEAN NOT NULL DEFAULT false, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, ds_tn_created BOOLEAN NOT NULL DEFAULT false, codebook_relationship_created BOOLEAN NOT NULL DEFAULT false, parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false, resource_consumed BOOLEAN NOT NULL DEFAULT false, resource_complete BOOLEAN NOT NULL DEFAULT false, created_date TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);

mysql> FLUSH PRIVILEGES;

mysql> EXIT;
```

### Commands for Checking the MySQL user, database, and tables
```bash
# mysql -u root -p

mysql> SELECT User FROM mysql.user;
mysql> SHOW DATABASES;
mysql> USE camelBatch;
mysql> SELECT DATABASE();
mysql> SHOW TABLES;
mysql> DESCRIBE camelBatchRequests;
mysql> DESCRIBE camelBatchResources;
```
#### Example Output:
```bash
mysql> SELECT User FROM mysql.user;
+-------------------+
| User              |
+-------------------+
| camelBatchProcess |
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
| camelBatch         |
| fedora3            |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
6 rows in set (0.00 sec)

mysql> USE camelBatch;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed

mysql> SELECT DATABASE();
+------------+
| DATABASE() |
+------------+
| camelBatch |
+------------+
1 row in set (0.00 sec)

mysql> SHOW TABLES;
+----------------------+
| Tables_in_camelBatch |
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
| codebook_relationship_created              | tinyint(1) | NO   |     | 0                 |                             |
| parent_child_resource_relationship_created | tinyint(1) | NO   |     | 0                 |                             |
| resource_consumed                          | tinyint(1) | NO   |     | 0                 |                             |
| resource_complete                          | tinyint(1) | NO   |     | 0                 |                             |
| created_date                               | timestamp  | NO   |     | CURRENT_TIMESTAMP |                             |
| updated_date                               | datetime   | YES  |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
+--------------------------------------------+------------+------+-----+-------------------+-----------------------------+
19 rows in set (0.00 sec)
```

## ServiceMix setup and configuration:
### Copy batch.process.sql.properties from `Sidora-REST/src/test/resources/sql` to `<smx-install>/sql/`
```bash
# mkdir <smx-install>/sql
# cp sidora-services/Sidora-REST/src/test/resources/sql/batch.process.sql.properties <smx-install>/sql/
```

### Create camel batch karaf config `<smx-install>/etc/edu.si.sidora.batch.cfg`
```sql
################################################################################
#
#   Copyright 2015-2016 Smithsonian Institution.
#
#   Licensed under the Apache License, Version 2.0 (the "License"); you may not
#   use this file except in compliance with the License.You may obtain a copy of
#   the License at: http://www.apache.org/licenses/
#
#   This software and accompanying documentation is supplied without
#   warranty of any kind. The copyright holder and the Smithsonian Institution:
#   (1) expressly disclaim any warranties, express or implied, including but not
#   limited to any implied warranties of merchantability, fitness for a
#   particular purpose, title or non-infringement; (2) do not assume any legal
#   liability or responsibility for the accuracy, completeness, or usefulness of
#   the software; (3) do not represent that use of the software would not
#   infringe privately owned rights; (4) do not warrant that the software
#   is error-free or will be maintained, supported, updated or enhanced;
#   (5) will not be liable for any indirect, incidental, consequential special
#   or punitive damages of any kind or nature, including but not limited to lost
#   profits or loss of data, on any basis arising from contract, tort or
#   otherwise, even if any of the parties has been warned of the possibility of
#   such loss or damage.
#
#   his distribution includes several third-party libraries, each with their own
#   license terms. For a complete copy of all copyright and license terms, including
#   those of third-party libraries, please see the product release notes.
#
################################################################################

# The properties defined in this file will be made available through karaf config properties
# and you can update through the karaf admin console to change the value
#
#
# For example, you may edit an existing property configured in this file via the SMX client commands and propagate the changes
#
# config:edit edu.si.sidora.batch
# config:property-set <property-to-be-set> <property-value>
# config:update

########################################################
#
# Sidora Batch Processing properties
#
# Default properties used to identify the Batch route logs
# edu.si.batch=edu.si.batch
#
########################################################

edu.si.batch=edu.si.batch

# The fedora host and password
si.fedora.host=http://localhost:8080/fedora
si.fedora.password=<fedora-password>

# The fedora user must be different from the Camera Trap routes otherwise derivatives will not be made
si.fedora.batch.user=<fedora-batch-user>

# The REST endpoint for the Batch Process
sidora.rest.service.address=/sidora/rest

# MySQL database setup
mysql.host=localhost
mysql.port=3306
mysql.database=camelBatch
mysql.username=<camelMySQL-user>
mysql.password=<camelMySQL-password>
```

# Deploy the Sidora-REST service to ServiceMix
- Shutdown ServiceMIX
- Compile sidora-services
- Deploy sidora-deployment.kar
- Start ServiceMix and check logs for errors

```batch
# cd <sidora-services>
# mvn clean install -DskipTests=true
# cp <sidora-services>/KAR/target/sidora-development <smx-install>/deploy/
```

# Testing Batch Resource REST Service
## Batch Resource Testing Curl:
```bash
# curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/imageFiles.xml" \
-F "ds_metadata=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/metadata.xml" \
-F "ds_sidora=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/sidora.xml" \
-F "association=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/association.xml" \
-F "resourceOwner=ramlani" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403
```

## FGDC / codebook Batch Testing Curl (query param ie. 'http://...?codebookPID=si:123456'):
```bash
# curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/codebook/files_ramlani5821e4a34a2aa.xml" \
-F "ds_metadata=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/codebook/batch_ramlani5821e4a34a2aa.xml" \
-F "ds_sidora=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/acodebook/sidora_ramlani5821e4a34a2aa.xml" \
-F "association=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/association.xml" \
-F "resourceOwner=ramlani" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403?codebookPID=si:123456
```

## Testing Batch Status Curl:
```bash
# curl -v -X GET http://localhost:8181/cxf/sidora/rest/batch/process/requestStatus/[parentId]/[correlationId]
```
# Sidora-Batch Service Setup and Configuration:
### A new Fedora user must be created for Derivatives to be created during Batch Process:
add camelBatchProcess user to `fedora-users.xml`
```bash
# vi <fedora-install>/server/config/fedora-users.xml

<?xml version='1.0' ?>Add
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

mysql> CREATE DATABASE IF NOT EXISTS sidora;
mysql> GRANT ALL ON sidora.* TO <camel-batch-user>@localhost IDENTIFIED BY '<camel-batch-secret>';

############################################################################
## sql that creates two table for the batch process
# MySQL 5.7
############################################################################

sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS sidora.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, resourceOwner TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, request_consumed BOOLEAN NOT NULL DEFAULT false, request_complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS sidora.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT DEFAULT NULL, resourceOwner TEXT NOT NULL, titleLabel TEXT DEFAULT NULL, resource_created BOOLEAN NOT NULL DEFAULT false, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, ds_tn_created BOOLEAN NOT NULL DEFAULT false, codebook_relationship_created BOOLEAN NOT NULL DEFAULT false, parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false, resource_consumed BOOLEAN NOT NULL DEFAULT false, resource_complete BOOLEAN NOT NULL DEFAULT 0, created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);


############################################################################
## sql that creates two table for the batch process
# MySQL 5.1 has issues with two columns with "DEFAULT CURRENT_TIMESTAMP"
############################################################################

sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS sidora.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, resourceOwner TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, request_consumed BOOLEAN NOT NULL DEFAULT false, request_complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS sidora.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT DEFAULT NULL, resourceOwner TEXT NOT NULL, titleLabel TEXT DEFAULT NULL, resource_created BOOLEAN NOT NULL DEFAULT false, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, ds_tn_created BOOLEAN NOT NULL DEFAULT false, codebook_relationship_created BOOLEAN NOT NULL DEFAULT false, parent_child_resource_relationship_created BOOLEAN NOT NULL DEFAULT false, resource_consumed BOOLEAN NOT NULL DEFAULT false, resource_complete BOOLEAN NOT NULL DEFAULT false, created_date TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);

mysql> FLUSH PRIVILEGES;

mysql> EXIT;
```

### Commands for Checking the MySQL user, database, and tables
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
#### Example Output:
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

## ServiceMix setup and configuration:
### Copy batch.process.sql.properties from `Routes/Sidora-Batch/Karaf-config/sql/` to `<smx-install>/sql/`
```bash
# mkdir <smx-install>/sql
# cp sidora-services/Routes/Sidora-Batch/Karaf-config/sql/application-sql.properties <smx-install>/sql/
```

### Create camel batch karaf config file `<smx-install>/etc/edu.si.sidora.batch.cfg` (Make sure to the properties are correct)
[edu.si.sidora.batch.cfg](../Routes/Sidora-Batch/Karaf-config/etc/edu.si.sidora.batch.cfg)

# Deploy the Sidora-Batch service to ServiceMix
- Shutdown ServiceMIX
- Clone sidora-services git repo
- Create edu.si.sidora.batch.cgf from step above (Make sure to the properties are correct)
- Create batch.process.sql.properties from step above
- Compile sidora-services from git clone
- Deploy sidora-deployment.kar
- Deploy sidora-batch.xml camel route
- Copy batch xslt and templates to SMX
- Start ServiceMix and check logs for errors
- Set sidora-batch.xml bundle to start-level 85

```batch
# cd <sidora-services> (git clone of sidora-services, may need to swith to correct branch)
# mvn clean install -DskipTests=true
# cp <sidora-services>/KAR/target/sidora-development <smx-install>/deploy/
# cp <sidora-services>/Routes/Sidora-Batch/Karaf-config/deploy/sidora-batch.xml <smx-install>/deploy/
# cp <sidora-services>/Routes/Sidora-Batch/Karaf-config/Input/templates/BatchResourceTemplate.vsl <smx-install>/Input/
# cp <sidora-services>/Routes/Sidora-Batch/Karaf-config/Input/xslt/BatchProcess_ManifestResource.xsl <smx-install>/Input/
```

# Testing Batch Resource REST Service
(see sidora-services/Sidora-Batch/test/resources/test-data/batch-test-files for example xml files used for curl requests)
## Batch Resource Testing Curl:
```bash
# curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://.../resourceFileList.xml" \
-F "ds_metadata=http://.../ds_metadata.xml" \
-F "ds_sidora=http://.../ds_sidora.xml" \
-F "association=http://.../association.xml" \
-F "resourceOwner=resourceOwner" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/[parentId]
```

## FGDC / codebook Batch Testing Curl (query param ie. 'http://...?codebookPID=si:123456'):
```bash
# curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://.../resourceFileList.xml" \
-F "ds_metadata=http://.../ds_metadata.xml" \
-F "ds_sidora=http://.../ds_sidora.xml" \
-F "association=http://.../association.xml" \
-F "resourceOwner=resourceOwner" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/[parentId]?codebookPID=[codebookPID]
```

## Testing Batch Status Curl:
```bash
# curl -v -X GET http://localhost:8181/cxf/sidora/rest/batch/process/requestStatus/[parentId]/[correlationId]
```
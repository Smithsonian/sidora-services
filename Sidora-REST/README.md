# Sidora REST Service Setup and Configuration:
## ServiceMix setup and configuration:

## MySQL setup and configuration:
The example below is for a test server where the MySQL database is co-located with Fedora.
- Create a MySQL database for Fedora with the required user and privileges.
- When using MySQL, it may be required to create the following table. It need only be done once.

mysql -u root -p

CREATE DATABASE IF NOT EXISTS camelBatch;
GRANT ALL ON camelBatch.* TO camelBatchUser@localhost IDENTIFIED BY '<camelBatchProcess-secret>';

############################################################################
## sql that creates two table for the batch process
# MySQL 5.7
############################################################################

# sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, resourceXML TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, contentModel TEXT NOT NULL, resourceOwner TEXT NOT NULL, titleField TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

#sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT NOT NULL, resourceOwner TEXT NOT NULL, titleField TEXT DEFAULT NULL, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, complete BOOLEAN NOT NULL DEFAULT 0, created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);


############################################################################
## sql that creates two table for the batch process
# MySQL 5.1 has issues with two columns with "DEFAULT CURRENT_TIMESTAMP"
############################################################################

sql.createBatchProcessRequestTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchRequests (correlationId CHAR(36) NOT NULL, parentId TEXT NOT NULL, resourceFileList TEXT NOT NULL, resourceXML TEXT NOT NULL, ds_metadata TEXT NOT NULL, ds_sidora TEXT NOT NULL, association TEXT NOT NULL, contentModel TEXT NOT NULL, resourceOwner TEXT NOT NULL, titleField TEXT NOT NULL, codebookPID TEXT DEFAULT NULL, resourceCount INT(10) NULL, processCount INT(10) NOT NULL DEFAULT 0, complete BOOLEAN NOT NULL DEFAULT false, created TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, PRIMARY KEY (correlationId));

sql.createBatchResourceFilesTable=CREATE TABLE IF NOT EXISTS camelBatch.camelBatchResources (correlationId CHAR(36) NOT NULL, resourceFile TEXT NOT NULL, parentId TEXT NOT NULL, pid TEXT DEFAULT NULL, contentModel TEXT NOT NULL, resourceOwner TEXT NOT NULL, titleField TEXT DEFAULT NULL, resource_created BOOLEAN NOT NULL DEFAULT false, ds_relsExt_created BOOLEAN NOT NULL DEFAULT false, ds_metadata_created BOOLEAN NOT NULL DEFAULT false, ds_sidora_created BOOLEAN NOT NULL DEFAULT false, ds_dc_created BOOLEAN NOT NULL DEFAULT false, ds_obj_created BOOLEAN NOT NULL DEFAULT false, complete BOOLEAN NOT NULL DEFAULT false, created_date TIMESTAMP DEFAULT '0000-00-00 00:00:00', updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);

FLUSH PRIVILEGES;

EXIT;

### Commands for Checking the user, database, and table
mysql -u root -p

SELECT User FROM mysql.user;
SHOW DATABASES;
USE camelBatch;
SELECT DATABASE();
SHOW TABLES;
DESCRIBE camelBatchRequests;
DESCRIBE camelBatchResources;

#### Example Output:
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
+------------------+------------+------+-----+---------------------+-----------------------------+
| Field            | Type       | Null | Key | Default             | Extra                       |
+------------------+------------+------+-----+---------------------+-----------------------------+
| correlationId    | char(36)   | NO   | PRI | NULL                |                             |
| parentId         | text       | NO   |     | NULL                |                             |
| resourceFileList | text       | NO   |     | NULL                |                             |
| resourceXML      | text       | NO   |     | NULL                |                             |
| ds_metadata      | text       | NO   |     | NULL                |                             |
| ds_sidora        | text       | NO   |     | NULL                |                             |
| contentModel     | text       | NO   |     | NULL                |                             |
| resourceOwner    | text       | NO   |     | NULL                |                             |
| titleField       | text       | NO   |     | NULL                |                             |
| codebookPID      | text       | YES  |     | NULL                |                             |
| resourceCount    | int(10)    | YES  |     | NULL                |                             |
| processCount     | int(10)    | NO   |     | 0                   |                             |
| complete         | tinyint(1) | NO   |     | 0                   |                             |
| created          | timestamp  | NO   |     | 0000-00-00 00:00:00 |                             |
| updated          | timestamp  | NO   |     | CURRENT_TIMESTAMP   | on update CURRENT_TIMESTAMP |
+------------------+------------+------+-----+---------------------+-----------------------------+
15 rows in set (0.00 sec)

mysql> DESCRIBE camelBatchResources;
+---------------------+------------+------+-----+---------------------+-----------------------------+
| Field               | Type       | Null | Key | Default             | Extra                       |
+---------------------+------------+------+-----+---------------------+-----------------------------+
| correlationId       | char(36)   | NO   |     | NULL                |                             |
| resourceFile        | text       | NO   |     | NULL                |                             |
| parentId            | text       | NO   |     | NULL                |                             |
| pid                 | text       | YES  |     | NULL                |                             |
| contentModel        | text       | NO   |     | NULL                |                             |
| resourceOwner       | text       | NO   |     | NULL                |                             |
| titleField          | text       | YES  |     | NULL                |                             |
| resource_created    | tinyint(1) | NO   |     | 0                   |                             |
| ds_relsExt_created  | tinyint(1) | NO   |     | 0                   |                             |
| ds_metadata_created | tinyint(1) | NO   |     | 0                   |                             |
| ds_sidora_created   | tinyint(1) | NO   |     | 0                   |                             |
| ds_dc_created       | tinyint(1) | NO   |     | 0                   |                             |
| ds_obj_created      | tinyint(1) | NO   |     | 0                   |                             |
| complete            | tinyint(1) | NO   |     | 0                   |                             |
| created_date        | timestamp  | NO   |     | 0000-00-00 00:00:00 |                             |
| updated_date        | timestamp  | NO   |     | CURRENT_TIMESTAMP   | on update CURRENT_TIMESTAMP |
+---------------------+------------+------+-----+---------------------+-----------------------------+
16 rows in set (0.01 sec)


### A new user must be created for the Batch Routes:



## Testing adddatastream for OBJ when file name has spaces
curl -v -i -u "fedoraAdmin:f3d0r@" -H "Content-Type=text/xml" -X POST "http://sidora0c.myquotient.net:8080/fedora/objects/si:391928/datastreams/OBJ?controlGroup=M&dsLabel=batch_test_image&dsLocation=http://sidora0c.myquotient.net/~ramlani/sidora/sidora0.4/sites/default/files/jbirkhimer582216cea5432_batch%20test%20image.jpg&mimeType=text/jpg&versionable=false"


### Testing Status:
curl -v -X GET http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/[parentId]/[correlationId]

curl -v -X GET http://localhost:8181/cxf/sidora/rest/batch/process/requestStatus/[parentId]/[correlationId]

curl -v -X GET http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:391809/0a56ad1f-c78f-435f-9e02-083bdb92fd14


## Testing FGDC / codebook test

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/codebook/files_ramlani5821e4a34a2aa.xml" \
-F "ds_metadata=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/codebook/batch_ramlani5821e4a34a2aa.xml" \
-F "ds_sidora=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/acodebook/sidora_ramlani5821e4a34a2aa.xml" \
-F "association=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/association.xml" \
-F "resourceOwner=ramlani" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403?codebookPID=si:123456


## Testing

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/image/imageFiles.xml" \
-F "ds_metadata=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/metadata.xml" \
-F "ds_sidora=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/sidora.xml" \
-F "association=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/association.xml" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/imageFiles.xml" \
-F "ds_metadata=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/metadata.xml" \
-F "ds_sidora=http://localhost/~jbirkhimer/Sidora-Batch-Test-Files/image/sidora.xml" \
-F "association=http://sidora0c.myquotient.net/~jbirkhimer/Sidora-Batch-Test-Files/audio/association.xml" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403


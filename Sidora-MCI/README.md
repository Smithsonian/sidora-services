### A new Fedora user must be created for Derivatives to be created during camel process:
add camelProcess user to `fedora-users.xml`
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

## MySQL setup and configuration:
The example below is for a test server where the MySQL database is co-located with Fedora.
- Create a MySQL database for Sidora Camel Service Requests with the required user and privileges.
- When using MySQL, it may be required to create the following tables. It need only be done once.
- Verify the version of MySQL you are using and create the tables for your version of MySQL.

```bash
# mysql -u root -p

mysql> CREATE DATABASE IF NOT EXISTS sidora;
mysql> GRANT ALL ON sidora.* TO <camel-user>@localhost IDENTIFIED BY '<camel-secret>';

############################################################################
## sql that creates the table for the Sidora Camel Service Requests
# MySQL 5.7
############################################################################

DROP TABLE IF EXISTS sidora.camelRequests;

CREATE TABLE IF NOT EXISTS sidora.camelRequests (
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


############################################################################
## sql that creates the table for the Sidora Camel Service Requests
# MySQL 5.1 has issues with two columns with "DEFAULT CURRENT_TIMESTAMP"
############################################################################
DROP TABLE IF EXISTS sidora.camelRequests;

CREATE TABLE IF NOT EXISTS sidora.camelRequests (
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
mysql> DESCRIBE camelRequests;
mysql> DESCRIBE camelResources;
```

## ServiceMix setup and configuration:
### Copy mci.sql.properties from `sidora-services/Sidora-MCI/Karaf-Config/sql/` to `<smx-install>/sql/`
```bash
# mkdir <smx-install>/sql
# cp sidora-services/Sidora-MCI/Karaf-Config/sql/application-sql.properties <smx-install>/sql/
```

### Create MCI karaf config `<smx-install>/etc/edu.si.sidora.mci.cfg`
[edu.si.sidora.mci.cfg](config/application-test.properties)

# Deploy the Sidora-MCI service to ServiceMix
- Shutdown ServiceMIX
- Clone sidora-services git repo
- Create `edu.si.sidora.mci.cgf` from step above (Make sure to the properties are correct)
- Copy MCI resource files
```bash
################################
# Resources (sql, xslt, schema's, etc.)
#
# NOTE: make sure to set the
# properties after installing them
################################

# install -v -g fedora -o fedora -m 644 <git-repo>/sidora-services/Sidora-MCI/Karaf-Config/Input/schemas/* /opt/sidora/smx/Input/schemas/

# install -v -g fedora -o fedora -m 644 <git-repo>/sidora-services/Sidora-MCI/Karaf-Config/Input/templates/* /opt/sidora/smx/Input/templates/

# install -v -g fedora -o fedora -m 644 <git-repo>/sidora-services/Sidora-MCI/Karaf-Config/Input/xslt/* /opt/sidora/smx/Input/xslt/

# install -v -g fedora -o fedora -m 644 <git-repo>/sidora-services/Sidora-MCI/Karaf-Config/sql/application-sql.properties /opt/sidora/smx/sql/
```
- Compile sidora-services from git clone
```bash
# mvn clean install -DskipTests=true
```
- Deploy sidora-deployment.kar
```bash
# cp <git-repo>/sidora-services>/Kar/target/sidora-deployment.kar <smx-home>/deply/
```
- Start ServiceMix and check logs for errors

# Testing MCI REST Service
## addProject() Testing Curl:
```bash

curl -v -H "Content-Type:application/xml" \
-d "@<path-to-project>/sidora-services/Sidora-MCI/src/test/resources/sample-data/MCI_Inbox/42_0.1.xml" \
-X POST "http://localhost:8181/cxf/sidora/rest/mci/addProject"

```

curl -v -H "Content-Type:application/xml" \
-d "@<path-to-project>/sidora-services/Sidora-MCI/src/test/resources/sample-data/MCI_Inbox/42_0.1.xml" \
-X POST "http://localhost:8181/cxf/sidora/rest/mci/addProject"
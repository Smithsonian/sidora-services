# Sidora Solr Service

Sidora Solr Service provides event driven create, update, and delete operation to Solr Indexes via Fedora Messaging or REST Endpoints



Sidora Solr REST API:

Base URL:

`http://localhost:{port}/sidora/solr`

URL Syntax

`/{solrIndex}/{pid}?solrOperation={add|delete|update}`

HTTP Method:
GET

HTTP Response:
200

Parameters:

`solrIndex = the index we are operating on`

`pid = the pid of the fedora object to index`

`solrOperation = add, delete, or update`

# Building:

**Make Sure You have a copy of `test.properties` with the correct properties in the project root (i.e. `sidora-services/test.properties`)**

> (**Optional**) Create `test.properties` from existing smx cfg properties
`cat /opt/sidora/smx/etc/system.properties /opt/sidora/smx/etc/edu.si.sidora.solr.cfg > test.properties` 

From `sidora-solr-spring-boot` module build the jar file

`mvn clean install`

This will create a runnable `jar` and a `config` dir in `target` with properties generated dynamically based on root `test.properties` values

# Running:

*Copy the `jar` and `config` dir to the location the program will run from and double check the properties in `config/application.properties` and `config/etc/edu.si.sidora.solr.cfg`*


`java -jar -Dkaraf.home=$PWD sidora-solr-spring-boot-1.0.jar`


Run as a background process:

`nohup java -jar -Dkaraf.home=$PWD sidora-solr-spring-boot-1.0.jar </dev/null >/dev/null 2>&1 &`

# Shutdown:

`curl -v -u user:pass -X POST localhost:{port}/shutdown`


# Spring Boot Actuator

*${server.port} is set in `config/application.properteis`*

`http://localhost:{port}/actuator`
`http://localhost:{port}/docs`

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Messaging with JMS](https://spring.io/guides/gs/messaging-jms/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Using Apache Camel with Spring Boot](https://camel.apache.org/spring-boot)
* [Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql/)


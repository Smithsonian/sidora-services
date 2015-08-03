#Tabular Metadata CXF Services:

##REQUIREMENTS:

- git
- maven
- ServiceMix

##Install projects to local maven repository:

####excel2tabular:

`git clone https://github.com/ocymum/excel2tabular.git`

`cd excel2tabular`

`mvn clean install`

####tabular-metadata:

`git clone https://github.com/ocymum/tabular-metadata.git`

`cd tabular-metadata`

`mvn clean install`

####tabular-metadata-cxf-services:

`git clone https://github.com/jbirkhimer/tabular-metadata-cxf-services.git`

`cd to tabular-metadata-cxf-services`

`mvn clean install`


##There are several options for deploying the Tabular Data Service:

1. Deploy to ServiceMix

    a. using blueprint implementation
    
    b. using spring implementation
    
    c. using camel implementation [not currently available]

2. Deploy to Tomcat or Jetty

##Deploying to ServiceMix:

The tabular data service makes use of the ServiceMix features facility.
A feature is a named, versioned collection of OSGi bundles that work
together to provide the tabular data functionality. The details of what
makes up the tabular data feature is contained in a features definition
file `features.xml`. The ServiceMix console includes a features subshell
that provides commands to enable, add and remove features, and to point
to feature repositories. When you add a feature, ServiceMix uses the
details provided in the features definition file `features.xml` to load
and activate all of the required bundles that are not already present.

ServiceMix includes a number of features that make the running of
the tabular data service quick and easy. Each feature enables you to use
a single command to install the example bundle and any bundles that the
example depends on.

###Add the Tabular Data feature repository to ServiceMix:

`karaf@root> features:addurl mvn:com.jbirkhimer.sidora/tabular-metadata-cxf-services/1.0-SNAPSHOT/xml/features`

###Check that the feature has been added to ServiceMix:

`karaf@root> features:list | grep tabular-metadata`

###Start either the `tabular-metadata-blueprint` or `tabular-metadata-spring` CXF service:

`karaf@root> features:install tabular-metadata-blueprint`

###Check that the tabular data service was created and is active:

`karaf@root> list | grep Tabular`


##Testing the Tabular Data Service:

There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8181/cxf/codebook/?url=[URL-of-my-tabular-data-file]`

The URL can be a `file:///[path-to-csv,-xls,-or-xlsx-file]` URL for convenience.

Changing /cxf servlet alias:
---------------------------
By default CXF Servlet is assigned a '/cxf' alias. You can change it in a couple of ways

a. Add org.apache.cxf.osgi.cfg to the /etc directory and set the
   `org.apache.cxf.servlet.context` property, for example:
   
   `org.apache.cxf.servlet.context=/custom`

b. Use shell config commands, for example:

     config:edit org.apache.cxf.osgi   
     config:propset org.apache.cxf.servlet.context /super
     config:update


##Deploying to Tomcat:

cd /tabular-metadata-cxf-services/tabular-metadata-cxf-services-webapp/target

copy `codebook.war` to `$TOMCAT_HOME/webapps`

##Testing the Tabular Data Service WebApp:

There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8080/codebook/?url=[URL-of-my-tabular-data-file]`

The URL can be a `file:///[path-to-csv,-xls,-or-xlsx-file]` URL for convenience.
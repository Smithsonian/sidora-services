#Tabular Metadata CXF Services:

##REQUIREMENTS:

- git
- maven
- ServiceMix

##Install projects to local maven repository:

####excel2tabular:

`git clone https://github.com/ocymum/excel2tabular.git`

`cd excel2tabular`

`mvn clean install -DskipTests=true`

####tabular-metadata:

`git clone https://github.com/ocymum/tabular-metadata.git`

`cd tabular-metadata`

`mvn clean install -DskipTests=true`

####tabular-metadata-cxf-services:

`git clone https://github.com/jbirkhimer/tabular-metadata-cxf-services.git`

`cd tabular-metadata-cxf-services`

`mvn clean install -DskipTests=true`


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

###Add your local Maven repository to `org.ops4j.pax.url.mvn.cfg`:

Edit the `org.ops4j.pax.url.mvn.cfg` file located in `[SERVICEMIX_HOME]/etc/`

Find the `Path to local Maven repository...` section and add the following line 

`org.ops4j.pax.url.mvn.localRepository=${user.home}/.m2/repository`

**NOTE:**
`${user.home}/.m2/repository` should be the absolute path to your local Maven
repository for example `/home/user_home/.m2/repository`

**NOTE:**
The user which Servicemix is running under must also have permissions set to access
the repository defined by the `org.ops4j.pax.url.mvn.localRepository` variable. 

###Add the Tabular Data feature repository to ServiceMix:

`karaf@root> features:addurl mvn:edu.si.sidora/tabular-metadata-cxf-services/1.0-SNAPSHOT/xml/features`

###Check that the feature has been added to ServiceMix:

`karaf@root> features:list | grep tabular-metadata`

###Start either the `tabular-metadata-blueprint` or `tabular-metadata-spring` CXF service:

`karaf@root> features:install tabular-metadata-blueprint`

###Check that the tabular data service was created and is active:

`karaf@root> list | grep Tabular`

###Permission problems when using local maven repo as users other than fedora
After doing the above `mvn clean install -DskipTests=true` commands for each project

Then trying to do the following from SMX to install the tabular data feature from maven repo

`karaf@root> feature:repo-add mvn:edu.si.sidora/tabular-metadata-cxf-services/1.0-SNAPSHOT/xml/features`

you get permissions issues if you build the project in your local maven repo (not fedora user) and
Servicemix is running under another user (ie. fedora)

A workaround to still be able to create the Tabular Data Servicemix feature you will need to change to the fedora user

and build the projects to the fedora users maven repo (ie. /home/fedora/.m2/repository)

change to the fedora user

`sudo su - fedora` 

you are now the fedora user and all the commands for building the projects from above are the same except for maven 

which is:

mvn => `/usr/local/apache-maven/apache-maven-3.3.3/bin/mvn` 

MAKE SURE TO LOGOUT OF FEDORA USER WHEN DONE BUILDING

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

   `config:edit org.apache.cxf.osgi`
   `config:propset org.apache.cxf.servlet.context /super`
   `config:update`


##Deploying to Tomcat:

`cd /tabular-metadata-cxf-services/tabular-metadata-cxf-services-webapp/target`

copy `codebook.war` to `$TOMCAT_HOME/webapps`

##Testing the Tabular Data Service WebApp:

There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8080/codebook/?url=[URL-of-my-tabular-data-file]`

The URL can be a `file:///[path-to-csv,-xls,-or-xlsx-file]` URL for convenience.
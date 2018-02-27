#Deploying FITSservlet to Fedora Tomcat
### Installation Steps
1. [Deploy FITSservlet](#deploy-fitsservlet)
2. [Edit catalina.properties](#edit-catalinaproperties)
3. [Stop Fedora](#stop-fedora)
4. [Start Fedora](#start-fedora)
5. [Test FITS Web Service](#test-fits-web-service)


## Deploy FITSservlet
```bash
# wget http://projects.iq.harvard.edu/files/fits/files/fits-1.1.3.war <FEDORA_CATALINA_HOME>webapps/
```

## Edit catalina.properties
Add Entries to `<FEDORA_CATALINA_HOME>/conf/catalina.properties` (see example [catalina.properties](src/test/resources/catalina.properties))

* Add the `<fits.home>` environment variable.
    ```bash
    fits.home=/opt/sidora/fits
    ```

* Add all `<fits.home>/lib/` JAR files to `<FEDORA_CATALINA_HOME>/webapps/fits-1.1.3/WEB-INF/lib/`
    ```bash
    cp <fits.home>/lib/*.jar <FEDORA_CATALINA_HOME>/webapps/fits-1.1.3/WEB-INF/lib/
    ```

    >***Note:** Do NOT add any JAR files that are contained in any of the FITS lib/ subdirectories to this classpath entry. They are added programmatically at runtime by the application. (Additional Information: https://github.com/harvard-lts/FITSservlet)*

## Stop Fedora
```bash
# service fedora stop
```

## Start Fedora
```bash
# service fedora start
```

## Test FITS Web Service
Obtaining the version information for FITS being used to examine the input files returned in plain text format.

Using Curl:
```bash
# curl --get http://localhost:8080/fits-1.1.3/version


1.0.4
```

Using Browser:
```bash
http://localhost:8080/fits-1.1.3/version
```
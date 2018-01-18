# Edan Test Utility

This test utility provides an http endpoint for receiving Edan GET requests from the Cameratrap Routes.
The body of the Edan GET request is printed in the logs.
The Edan test utility can be run separately as standalone or as a bundle deployed in Servicemix.


## Note:

### If running from ServiceMix:

Make sure the `si.ct.uscbi.server` property is set in the `<smx-home>/etc/edu.si.sidora.emammal.cfg` property file

Ex. `si.ct.uscbi.server = /edan`

*** AFTER YOU ARE FINISHED TESTING BE SURE TO CHANGE THIS PROPERTY TO THE ACTUAL EDAN HTTP PATH !!! ***

### If Running as standalone:

 `http://localhost:1101/edan` is the default endpoint. To change it you need to edit the `edu.si.sidora.emammal.cfg` properties file in the `EdanTestService/Karaf-config/etc` directory and then start the Edan test service.


## To build this project use

```
mvn clean install
```


## To run this project standalone use the following Maven goal

    mvn camel:run


## To deploy in ServiceMix

From the ServiceMix console:

```
smx@root>bundle:install mvn:edu.si.services/edan-test-util/1.0
```

Or by placing the `edan-test-util-1.0.jar` into `<smx-home>/deploy`

## Making Sure the Edan Test endpoint is working

From cmd line:

```
#If edan-test-util was started via camel:run
curl -v -X GET "http://localhost:1101/edan/addEdan"

#If edan-test-util was deployed to SMX
curl -v -X GET "http://localhost:1101/edan/addEdan"
```

You should see something similar in the log output.

```
Jul 06, 2017 1:51:31 PM org.apache.cxf.interceptor.LoggingInInterceptor
INFO: Inbound Message
----------------------------
ID: 1
Address: http://localhost:1101/edan/addEdan
Http-Method: GET
Content-Type:
Headers: {Accept=[*/*], Content-Type=[null], Host=[localhost:1101], User-Agent=[curl/7.29.0]}
--------------------------------------
2017-07-06 13:51:31,899 [qtp854009666-31] INFO  test                           - Starting Edan Service Request for: addEdan ...
2017-07-06 13:51:31,906 [qtp854009666-31] INFO  test                           - ===================================================
2017-07-06 13:51:31,906 [qtp854009666-31] INFO  test                           - null
2017-07-06 13:51:31,906 [qtp854009666-31] INFO  test                           - ===================================================
2017-07-06 13:51:31,907 [qtp854009666-31] INFO  test                           - Finished Edan Service Request for: addEdan ...
Jul 06, 2017 1:51:31 PM org.apache.cxf.interceptor.LoggingOutInterceptor
INFO: Outbound Message
---------------------------
ID: 1
Response-Code: 200
Content-Type: */*
Headers: {Accept=[*/*], User-Agent=[curl/7.29.0], Host=[localhost:1101], Date=[Thu, 06 Jul 2017 17:51:31 GMT], Content-Length=[0]}
--------------------------------------
```
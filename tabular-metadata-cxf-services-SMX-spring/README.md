#Servicemix CXF Spring Service for tabular data:

##There is only one way to use it: send a GET (with browser or command-line tool) to:

`http://localhost:8181/cxf/spring/codebook/?url=file:///C:/Users/jbirkhimer/Desktop/Intellij%20Workspace/tabular-metadata/tabular-metadata-generator/src/test/resources/itest-data/Thompson-WMA-10B-researcher_observation.csv`

##Required Bundle to Install:

`osgi:install mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.commons-httpclient/3.1_7`
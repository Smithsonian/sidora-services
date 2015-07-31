Tabular Metadata CXF Services:

REQUIREMENTS:

git
maven
servicemix

Install projects to local maven repository:

git clone https://github.com/ocymum/excel2tabular.git
cd excel2tabular
mvn clean install

git clone https://github.com/ocymum/tabular-metadata.git
cd tabular-metadata
mvn clean install

cd to tabular-metadata-cxf-services
mvn clean install


Deploying to Servicemix:

features:addurl mvn:com.jbirkhimer.sidora/tabular-metadata-cxf-services/1.0-SNAPSHOT/xml/features

features:list | grep tabular-metadata

features:install tabular-metadata-blueprint
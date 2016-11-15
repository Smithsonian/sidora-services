### A new user muct be created for the Batch Routes:


### Testing:

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=/opt/sidora/smx/Sidora-Batch-Test-Files/image-resources.zip;type=text/plain" \
-F "contentModel=si:generalImageCModel" \
-F "ds_MODS=/opt/sidora/smx/Sidora-Batch-Test-Files/metadatWithTitle.xml;type=text/plain" \
-F "titleField=test-title" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=/opt/sidora/smx/Sidora-Batch-Test-Files/image-resources.zip;type=text/plain" \
-F "contentModel=si:generalImageCModel" \
-F "ds_MODS=/opt/sidora/smx/Sidora-Batch-Test-Files/metadatWithTitle.xml;type=text/plain" \
-F "titleField=test-title" -F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403


curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=/opt/sidora/smx/Sidora-Batch-Test-Files/image-resources.zip;type=text/plain" \
-F "contentModel=si:generalImageCModel" \
-F "ds_MODS=/opt/sidora/smx/Sidora-Batch-Test-Files/metadataWithTitle.xml;type=text/plain" \
-F "titleField=test-title" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403



########################

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=/opt/sidora/smx/Sidora-Batch-Test-Files/image-resources.zip;type=text/plain" \
-F "contentModel=si:generalImageCModel" \
-F "ds_MODS=/opt/sidora/smx/Sidora-Batch-Test-Files/metadataWithTitle.xml;type=text/plain" \
-F "titleField=test-title" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403

########################

curl -v -H "Content-Type:multipart/form-data" \
-F "resourceFileList=/opt/sidora/smx/Sidora-Batch-Test-Files/image-resources.zip;type=text/plain" \
-F "contentModel=si:generalImageCModel" \
-F "ds_MODS=/opt/sidora/smx/Sidora-Batch-Test-Files/metadataWithTitle.xml;type=text/plain" \
-F "ds_Sidora=/opt/sidora/smx/Sidora-Batch-Test-Files/sidora-datastream.xml;type=text/plain" \
-F "titleField=test-title" \
-F "resourceOwner=ramlani;type=text/plain" \
-X POST http://localhost:8181/cxf/sidora/rest/batch/process/addResourceObjects/si:390403


### Common Data Streams
Each of the resource types above include the following common data streams:
Rels-Ext - This data stream is an RDF file that includes the unique identifier for the object, the content model used to create the object and any relationships with other objects.
DC -  The Dublin Core data stream is used in all objects in the SIdora system.
POLICY - The policy data stream defines the specific ownership policies for an object.

### General Image
Images can be uploaded as a resource and associated with any SIdora concept in the hierarchy.  The supported image formats include .jpg, .jp2, .png, .gif and .tif.  Once an image is uploaded, the following data streams are created in addition to the data streams described in the Common Data Streams section above:
MODS - The MODS data stream describes the metadata associated with a general image resource.
OBJ - The OBJ data stream represents the original image object as it was ingested.
FITS - The FITS (File Information Tool Set) data stream is the result of extracting the technical metadata from the image object.
IMAGE - The IMAGE data stream represents a .jpg derivative used for viewing.
TN - The TN data stream represents the thumbnail derivative of the original image.


association file notes
the following is bassically the xpath location for title field on the datastream being sent.
["title_field"]=>
 
    array(3) {
 
      [0]=>
 
      string(4) "mods"
 
      [1]=>
 
      string(9) "titleInfo"
 
      [2]=>
 
      string(5) "title"
 
    }
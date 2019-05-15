import logging
import os
import pprint
import sys

import requests
from lxml.etree import fromstring, tostring, parse, XSLT
from requests.auth import HTTPBasicAuth

ns = {"fsmgmt": "http://www.fedora.info/definitions/1/0/management/",
      "fedora": "info:fedora/fedora-system:def/relations-external#",
      "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "ri": "http://www.w3.org/2005/sparql-results#",
      "fits": "http://hul.harvard.edu/ois/xml/ns/fits/fits_output"}

def main():
    fedora_user = sys.argv[1]
    fedora_pass = sys.argv[2]
    HOST = sys.argv[3]

    # http endpoints
    FEDORA_URL = "http://" + HOST + ":8080/fedora"
    FUSEKI_URL = "http://" + HOST + ":9080/fuseki/fedora3"
    FITS_URL = "http://" + HOST + ":8080/fits-1.1.3/examine"

    # fuseki query params
    query = "SELECT ?ctPID FROM <info:edu.si.fedora#ri> WHERE {?ctPID <info:fedora/fedora-system:def/model#hasModel> <info:fedora/si:cameraTrapCModel>.}"

    # defining a params dict for the parameters to be sent
    FUSEKI_PARAMS = {'query': query}

    # sending get request and saving the response as response object
    fuseki_request = requests.get(url=FUSEKI_URL, params=FUSEKI_PARAMS)

    # extracting data in json format
    fuseki_data = fuseki_request.json()

    logging.debug(fuseki_data)

    ctPIDList = fuseki_data["results"]["bindings"]

    ctPIDCount = len(ctPIDList)
    logging.info("ctPID count: %s", str(ctPIDCount))

    # loop over the deployment pids
    for i, pid in enumerate(ctPIDList, start=1):
        deploymentPID = pid["ctPID"]["value"].split("info:fedora/")[1]
        logging.info("(" + str(i) + "/" + str(ctPIDCount) + ") deployment: %s", deploymentPID)

        FEDORA_PARAMS = {'format': 'xml'}
        URL = FEDORA_URL + "/objects/" + deploymentPID + "/datastreams"
        r = requests.get(url=URL, params=FEDORA_PARAMS)
        logging.info("deployment datastreams: %s", r.url)
        deploymentPID_datastreams = fromstring(r.content)
        logging.debug("deployment objectDatastreams:\n%s", tostring(deploymentPID_datastreams, pretty_print=True).decode())

        hasFGDC = deploymentPID_datastreams.xpath("boolean(.//*[@dsid='FGDC'])", namespaces=ns)
        logging.debug("deployment has FGDC: %s", hasFGDC)

        if hasFGDC:
            dsID = "FGDC"
            r = requests.get(url=FEDORA_URL + "/objects/" + deploymentPID + "/datastreams/" + dsID + "/content")
            logging.info("deployment FGDC: %s", r.url)
            deploymentFGDC = fromstring(r.content)
            logging.debug(tostring(deploymentFGDC, pretty_print=True).decode())

            hasMakeModel = deploymentFGDC.xpath("//attrlabl/text() = 'Camera Make'")
            logging.debug("FGDC has Camera Make and Model Already: %s", hasMakeModel)

            if not hasMakeModel:
                dsID = "RELS-EXT"
                r = requests.get(url=FEDORA_URL + "/objects/" + deploymentPID + "/datastreams/" + dsID + "/content", auth=HTTPBasicAuth(fedora_user, fedora_pass))
                logging.info("deployment RELS-EXT: %s", r.url)
                deploymentRelsExt = fromstring(r.content)
                logging.debug(tostring(deploymentRelsExt, pretty_print=True).decode())

                deploymentImgResourcePid = deploymentRelsExt.xpath("substring-after(.//fedora:hasResource[1]/@rdf:resource, 'info:fedora/')", namespaces=ns)
                # deploymentImgResourcePid = deploymentRelsExt.xpath(".//fedora:hasResource[1]/@rdf:resource", namespaces=ns)[0].split("info:fedora/")[1]
                logging.info("resource pid: %s", deploymentImgResourcePid)

                if deploymentImgResourcePid not in (None, ''):
                    dsID = "OBJ"
                    r = requests.get(url=FEDORA_URL + "/objects/" + deploymentImgResourcePid + "/datastreams/" + dsID, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))
                    logging.info(r.url)
                    objDatastreamProfile = fromstring(r.content)
                    logging.debug(tostring(objDatastreamProfile, pretty_print=True).decode())

                    objMIME = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsMIME/text()", namespaces=ns)[0]
                    objLabel = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsLabel/text()", namespaces=ns)[0]
                    logging.info("OBJ mimeType: %s label: %s", objMIME, objLabel)

                    if "image" in objMIME:
                        logging.info("downloading OBJ...")
                        dsID = "OBJ"
                        r = requests.get(url=FEDORA_URL + "/objects/" + deploymentImgResourcePid + "/datastreams/" + dsID + "/content", stream=True)
                        logging.info("resource OBJ datastreamProfile: %s", r.url)

                        fileName = "output/" + deploymentImgResourcePid + "_" + objLabel + ".jpg"
                        with open(fileName, "wb") as f:
                            for chunk in r.iter_content(chunk_size=1024):
                                # writing one chunk at a time to pdf file
                                if chunk:
                                    f.write(chunk)
                        f.close()

                        if os.path.exists(fileName):
                            logging.info("getting FITS...")
                            f = open(os.path.abspath(fileName), "rb")
                            FITS_PARAMS = {'datafile': f}
                            r = requests.post(url=FITS_URL, files=FITS_PARAMS)
                            logging.info(r.url + "?file=" + os.path.abspath(fileName))
                            deploymentImgResourceFITS = fromstring(r.content)
                            f.close()
                            os.remove(fileName)
                            logging.debug(tostring(deploymentImgResourceFITS, pretty_print=True).decode())

                            cameraMake = deploymentImgResourceFITS.xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraManufacturer/text()", namespaces=ns)
                            cameraModel = deploymentImgResourceFITS.xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraModelName/text()", namespaces=ns)

                            if len(cameraMake) > 0:
                                cameraMake = cameraMake[0]
                            else:
                                cameraMake = ""
                            if len(cameraModel) > 0:
                                cameraModel = cameraModel[0]
                            else:
                                cameraModel = ""

                            logging.info("Camera Make: %s Model: %s", cameraMake, cameraModel)

                            xslt = parse("updateManifestDeployment.xsl")
                            transform = XSLT(xslt)
                            newFGDC = transform(deploymentFGDC, cameraMake=XSLT.strparam(cameraMake), cameraModel=XSLT.strparam(cameraModel))
                            logging.debug(tostring(newFGDC, pretty_print=True).decode())

                            if debug:
                                f = open("output/" + deploymentPID + "_FGDC_output.xml", "w")
                                f.write(tostring(newFGDC, pretty_print=True).decode())
                                f.close()
                            else:
                                logging.info("Updating %s FGDC with Camera Make: %s, Model: %s", deploymentPID, cameraMake, cameraModel)
                                dsID = "FGDC"
                                FEDORA_PARAMS = {'mimeType': "text/xml"}
                                r = requests.put(FEDORA_URL + "/objects/" + deploymentPID + "/datastreams/" + dsID, data=tostring(newFGDC, pretty_print=True).decode(), params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))
                                logging.debug(r.url)
                                fgdcObjectDatastreams = fromstring(r.content)
                                logging.info("FGDC objectDatastreams:\n%s", tostring(fgdcObjectDatastreams, pretty_print=True).decode())

                    else:
                        logging.error("Problem resource: mimeType= %s pid= %s %s", objMIME, deploymentImgResourcePid, r.url)
                else:
                    logging.error("Problem deployment has no resources: %s", r.url)
            else:
                logging.error("FGDC has Camera Make and Model Already: %s", hasMakeModel)
        else:
            logging.error("Problem deployment has no FGDC datastream: %s", r.url)

        logging.info("====================================================")


if __name__ == "__main__":
    if len(sys.argv) > 4 and "debug" in sys.argv[4]:
        level = logging.DEBUG
        debug = True
    else:
        level = logging.INFO
        debug = False

    logging.basicConfig(stream=sys.stdout, level=level, format='[%(levelname)s] %(message)s')

    main()

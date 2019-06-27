import argparse
import concurrent
import configparser
import csv
import json
import logging
import os
import re
import shutil
import subprocess
import sys
import time
from concurrent.futures import ALL_COMPLETED
from string import Template

import cv2
import requests
from lxml.etree import fromstring, tostring, parse, XSLT
from requests.adapters import HTTPAdapter
from requests.auth import HTTPBasicAuth
# from requests.packages.urllib3.util.retry import Retry
from urllib3 import Retry

from FaceBlurrer import FaceBlurrer

ns = {"fsmgmt": "http://www.fedora.info/definitions/1/0/management/",
      "fedora": "info:fedora/fedora-system:def/relations-external#",
      "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "ri": "http://www.w3.org/2005/sparql-results#",
      "fits": "http://hul.harvard.edu/ois/xml/ns/fits/fits_output",
      "fs": "info:fedora/fedora-system:def/model#"}

global FEDORA_URL
global FUSEKI_URL
global FITS_URL

def getDeploymentsFromFuseki():
    # fuseki query param
    # query = "SELECT ?ctPID FROM <info:edu.si.fedora#ri> WHERE {?ctPID <info:fedora/fedora-system:def/model#hasModel> <info:fedora/si:cameraTrapCModel>.}"

    # (count(?ctPID) as ?count)

    query = "SELECT ?child; FROM <info:edu.si.fedora#ri> WHERE { <info:fedora/" + ct_root + "> <info:fedora/fedora-system:def/relations-external#hasConcept>+ ?child . ?child <info:fedora/fedora-system:def/view#disseminates> ?diss . FILTER regex(str(?diss), 'MANIFEST')}"

    # defining a params dict for the parameters to be sent
    FUSEKI_PARAMS = {'query': query}

    # sending get request and saving the response as response object
    fuseki_request = r.get(url=FUSEKI_URL, params=FUSEKI_PARAMS)

    # extracting data in json format
    fuseki_data = fuseki_request.json()

    log.debug(fuseki_data)

    return fuseki_data["results"]["bindings"]


# https://www.peterbe.com/plog/best-practice-with-retries-with-requests
def doGet(pid, ds, content, deploymentPid):
    FEDORA_PARAMS = {'format': 'xml'}
    if ds is None:
        URL = FEDORA_URL + "/objects/" + pid + "/datastreams"
    elif not content:
        URL = FEDORA_URL + "/objects/" + pid + "/datastreams/" + ds
    else:
        URL = FEDORA_URL + "/objects/" + pid + "/datastreams/" + ds + "/content"

    req = r.get(url=URL, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))
    # log.debug("pid: %s, url: %s", pid, str(r.url))
    content_type = req.headers.get("content-type")
    content_disposition = req.headers.get('content-disposition')
    log.debug("content_type: %s, content-disposition: %s", content_type, content_disposition)

    if 'image/jpeg' in content_type.lower():
        if not content_disposition:
            return None
        fname = re.findall('filename="(.+?)(?=[."])', content_disposition)
        if len(fname) == 0:
            return None

        if args.group_resources:
            fileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + pid.replace(":", "_") + "/" + pid + "_" + fname[0] + ".jpg"
        else:
            fileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + pid.replace(":", "_") + "_" + fname[0] + ".jpg"

        log.debug("filename: %s", fileName)

        if not os.path.exists(os.path.dirname(fileName)):
            os.makedirs(os.path.dirname(fileName))

        with open(fileName, "wb") as f:
            for chunk in req.iter_content(chunk_size=1024):
                # writing one chunk at a time to jpg file
                if chunk:
                    f.write(chunk)
        f.close()
        return fileName
    if 'application/json' in content_type.lower():
        # extracting data in json format
        fuseki_data = req.json()
        log.debug(fuseki_data)
        return fuseki_data["results"]["bindings"]
    else:
        response = fromstring(req.content)
        log.debug("pid: %s, response:\n%s", pid, tostring(response, pretty_print=True).decode())
        return response


def doPost(pid, data, ds, params):
    if params:
        FEDORA_PARAMS = params
    else:
        FEDORA_PARAMS = {'format': 'xml'}

    if ds is None:
        URL = FEDORA_URL + "/objects/" + pid + "/datastreams"
    else:
        URL = FEDORA_URL + "/objects/" + pid + "/datastreams/" + ds

    req = r.post(url=URL, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass), data=data)
    log.info("pid: %s, url: %s", pid, req.url + "<--------------------")
    content_type = req.headers.get("content-type")
    content_disposition = req.headers.get('content-disposition')
    response = fromstring(req.content)
    log.debug("post to url: %s, response: %s", req.url, tostring(response, pretty_print=True).decode())
    return response


def doPut(pid, data, ds, params):
    FEDORA_PARAMS = params

    URL = FEDORA_URL + "/objects/" + pid + "/datastreams/" + ds

    req = r.put(url=URL, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))
    log.info("pid: %s, url: %s", pid, req.url + "<--------------------")
    response = fromstring(req.content)
    log.debug("put to url: %s, response: %s", req.url, tostring(response, pretty_print=True).decode())
    return response


def getFITS(fileName):
    if os.path.exists(str(fileName)):
        # log.debug("deployment: %s, getting %s FITS...", pid, pid)
        f = open(os.path.abspath(fileName), "rb")
        FITS_PARAMS = {'datafile': f}

        req = r.post(url=FITS_URL, files=FITS_PARAMS)
        log.debug(req.url + "?file=" + os.path.abspath(fileName))
        fits = fromstring(req.content)
        log.debug("FITS for %s: %s", os.path.abspath(fileName), tostring(fits, pretty_print=True).decode())

        cameraMake = fits.xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraManufacturer/text()", namespaces=ns)
        cameraModel = fits.xpath("/fits:fits/fits:metadata/fits:image/fits:digitalCameraModelName/text()", namespaces=ns)

        if len(cameraMake) > 0:
            cameraMake = cameraMake[0]
        else:
            cameraMake = "None"
        if len(cameraModel) > 0:
            cameraModel = cameraModel[0]
        else:
            cameraModel = "None"

        log.debug("Camera Make: %s Model: %s for file: %s", cameraMake, cameraModel, fileName)
        # log.debug("fits: %s", tostring(fits, pretty_print=True).decode())
        f.close()
        return cameraMake, cameraModel, fits
    else:
        log.error("OBJ temp file %s is missing", fileName)
        return None, None, None


def getOBJ(pid, deploymentPid):

    objLabel = None
    objVersion = None
    fileName = None

    if pid not in (None, ''):
        objDatastreamProfile = doGet(pid, "OBJ", False, None)
        # log.debug(tostring(objDatastreamProfile, pretty_print=True).decode())

        objMIME = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsMIME/text()", namespaces=ns)[0]
        objLabel = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsLabel/text()", namespaces=ns)[0]
        objVersion = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsVersionID/text()", namespaces=ns)[0]
        log.debug("OBJ mimeType: %s label: %s", objMIME, objLabel)

        if "image" in objMIME:
            log.debug("downloading %s OBJ...", pid)
            # Save OBJ to tmp file
            fileName = doGet(pid, "OBJ", True, deploymentPid)
        elif "csv" in objMIME:
            log.debug("Found Observation!!! Skipping Downloading Resource %s", pid)
        else:
            log.error("Problem resource: mimeType= %s resource pid = %s", objMIME, pid)
            problemList[pid] = "resource is not an image. mimeType=" + objMIME
    else:
        log.error("no resources %s", pid)
        problemList[pid] = "resources pid is null"

    return fileName, objLabel, objVersion


def updateFGDC(deploymentPid, cameraMake, cameraModel):
    deploymentFGDC = doGet(deploymentPid, "FGDC", True, None)
    # log.debug(tostring(deploymentFGDC, pretty_print=True).decode())
    hasMakeModel = deploymentFGDC.xpath("//attrlabl/text() = 'Camera Make' and //attrlabl/text() = 'Camera Model'")

    if not hasMakeModel:
        xslt = parse("updateManifestDeployment.xsl")
        transform = XSLT(xslt)
        newFGDC = transform(deploymentFGDC, cameraMake=XSLT.strparam(cameraMake), cameraModel=XSLT.strparam(cameraModel))

        log.debug("Updating deployment: %s, FGDC with Camera Make: %s, Model: %s\nFGDC:\n%s", deploymentPid, cameraMake, cameraModel, tostring(newFGDC, pretty_print=True).decode())

        if dryrun:
            fgdcFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + deploymentPid.replace(":", "_") + "_FGDC_output.xml"
            if not os.path.exists(os.path.dirname(fgdcFileName)):
                os.makedirs(os.path.dirname(fgdcFileName))
            f = open(fgdcFileName, "w")
            f.write(tostring(newFGDC, pretty_print=True).decode())
            f.close()
            log.info("Skipping FGDC fedora update!!! Saving to file: %s", fgdcFileName)
        else:
            response = doPost(deploymentPid, tostring(newFGDC, pretty_print=True).decode(), "FGDC", {'type': 'text/xml', 'group': 'X', 'dsLabel': 'FGDC-CTPlot%20Record', 'versionable': 'true'})
            log.debug("http POST, FGDC update pid: %s, response:\n%s", deploymentPid, tostring(response, pretty_print=True).decode())

    else:
        log.warning("FGDC has Camera Make and Model Already. Deployment: %s", deploymentPid)

    log.info("Finished updateFGDC for deployment %s", deploymentPid)


def updateFITS(resourcePid, fits, deploymentPid):
    if dryrun:
        if args.group_resources:
            fitsFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "/" + resourcePid + "_FITS_output.xml"
        else:
            fitsFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "_FITS_output.xml"

        if not os.path.exists(os.path.dirname(fitsFileName)):
            os.makedirs(os.path.dirname(fitsFileName))
        f = open(fitsFileName, "w")
        f.write(tostring(fits, pretty_print=True).decode())
        f.close()
        log.info("Skipping post FITS update!!! Saving to file: %s", fitsFileName)
    else:
        result = doPost(resourcePid, tostring(fits, pretty_print=True).decode(), "FITS", {'dsLabel': 'FITS%20Generated%20Image%20Metadata', 'mimeType': 'text/xml', 'versionable': 'false', 'group': 'X'})
        log.debug("http POST, FITS update, pid: %s response:\n%s", resourcePid, tostring(result, pretty_print=True).decode())

    log.info("Finished updateFITS for resource %s", resourcePid)


def updateOBJ(resourcePid, img, blurFileName):
    if dryrun:
        log.info("Skipping OBJ fedora update!!! Saving to file: %s", blurFileName)
    else:
        resultPut = doPut(resourcePid, None, "OBJ", {'versionable': 'true'})
        log.debug("http POST, OBJ update, pid: %s response:\n%s", resourcePid, tostring(resultPut, pretty_print=True).decode())

        imgEncoded = cv2.imencode('.jpg', img)[1]
        resultPost = doPost(resourcePid, imgEncoded.tostring(), "OBJ", {'mimeType': 'image/jpeg', 'versionable': "true"})
        log.debug("http POST, OBJ update, pid: %s response:\n%s", resourcePid, tostring(resultPost, pretty_print=True).decode())

    log.info("Finished updateOBJ for resource %s", resourcePid)


def updateSidora(resourcePid, sidoraDs, deploymentPid):
    if dryrun:
        if args.group_resources:
            sidoraFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "/" + resourcePid + "_SIDORA_output.xml"
        else:
            sidoraFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "_SIDORA_output.xml"

        if not os.path.exists(os.path.dirname(sidoraFileName)):
            os.makedirs(os.path.dirname(sidoraFileName))

        f = open(sidoraFileName, "w")
        f.write(sidoraDs)
        f.close()
        log.info("Skipping SIDORA fedora update!!! Saving to file: %s", sidoraFileName)
    else:
        result = doPost(resourcePid, sidoraDs, "SIDORA", {'mimeType': 'text/xml', 'versionable': "true"})
        log.debug("http POST, SIDORA update, pid: %s response:\n%s", resourcePid, tostring(result, pretty_print=True).decode())

    log.info("Finished updateSidora for resource %s", resourcePid)


def doUpdate(deploymentPid, resourcePid, fgdcResourcePid, datastreamList, manifest):

    filename = None
    label = None
    cameraMake = None
    cameraModel = None
    fits = None
    isBlurred = False

    log.debug("Starting update for deployment %s, resource: %s, datastreams %s", deploymentPid, resourcePid, args.datastreams)

    # we only need one resource to update the Deployment FGDC use the
    # first resource from RELS-EXT (fgdcResourcePid) to get camera info
    if "FGDC" in args.datastreams and resourcePid == fgdcResourcePid:
        try:
            # check to make sure the deployment has an FGDC ds first
            hasFGDC = datastreamList.xpath("boolean(.//*[@dsid='FGDC'])", namespaces=ns)

            if hasFGDC:
                filename, label, objVersion = getOBJ(resourcePid, deploymentPid)
                if label not in (None, '', "Observation") and filename not in (None, ''):
                    cameraMake, cameraModel, fits = getFITS(filename)
                    log.debug("deployment: %s, resource: %s, cameraMake: %s, cameraModel: %s", deploymentPid, resourcePid, cameraMake, cameraModel)
                    updateFGDC(deploymentPid, cameraMake, cameraModel)
                    log.info("Finished updating deployment %s FGDC with camera make: %s and model: %s.", deploymentPid, cameraMake, cameraModel)
                else:
                    log.error("Problem with resource for camera make and model: pid = %s", resourcePid)
                    problemList[resourcePid] = "Problem with resource for camera make and model"
            else:
                log.error("Problem deployment has no FGDC datastream: pid = %s", deploymentPid)
                problemList[deploymentPid] = "deployment has no FGDC"
        except:
            log.exception("Error Updating FGDC datastream for deployment: pid = %s", deploymentPid)
            problemList[deploymentPid] = "Error Updating FGDC datastream"

    if "OBJ" in args.datastreams:
        try:
            # TODO: check manifest
            if filename in (None, ''):
                filename, label, objVersion = getOBJ(resourcePid, deploymentPid)

            log.debug("OBJ version: %s, pid: %s", objVersion, resourcePid)

            if args.validate and "OBJ.0" in objVersion:
                if label in ('Researcher Observations', 'Volunteer Observations', 'Image Observations'):
                    log.debug("Found Observation!!! Skipping OBJ update for resource %s", resourcePid)
                elif label not in (None, '') and filename not in (None, ''):
                    imageId = os.path.splitext(label)[0]
                    log.debug("check SpeciesScientificName for resource: %s, label: %s", resourcePid, imageId)
                    xpath = str("boolean(.//ImageSequence[Image[ImageId/text() = '" + imageId + "']]/ResearcherIdentifications/Identification/SpeciesScientificName[contains('" + filterList + "', text())])")
                    hasFace = manifest.xpath(xpath)

                    if hasFace:
                        log.debug("resource: %s, label: %s, hasFace: %s", resourcePid, label, hasFace)
                        img = cv2.imread(filename, cv2.IMREAD_COLOR)
                        blurArgs = [None, blurValue, classifier]

                        # imgBlur = FaceBlurrer.faceBlur(img, blurArgs)
                        imgBlur = FaceBlurrer.blur(img, (blurValue, blurValue))

                        if args.group_resources:
                            blurFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "/" + resourcePid + "_blur_output.jpg"
                        else:
                            blurFileName = output_dir + "/" + deploymentPid.replace(":", "_") + "/" + resourcePid.replace(":", "_") + "_blur_output.jpg"

                        cv2.imwrite(blurFileName, imgBlur)

                        # exiftool_cmd = [exiftool_path, '-all=', '-TagsFromFile', "'" + filename + "'", '-exif:all', "'" + output_dir + "/" + resourcePid + "_OBJ_output.jpg'"], shell=True)]
                        exiftool_cmd = [exiftool_path, '-TagsFromFile', os.path.abspath(filename), os.path.abspath(blurFileName), '-overwrite_original', '-q']
                        log.debug("exiftool cmd %s", exiftool_cmd)

                        try:
                            subprocess.check_call(exiftool_cmd)
                        except subprocess.CalledProcessError as e:
                            output = e.output.decode()
                            log.exception("exiftool error:\n", output)

                        updateOBJ(resourcePid, imgBlur, blurFileName)
                        filename = blurFileName
                        isBlurred = True
                else:
                    log.error("Problem with resource could not update OBJ: pid = %s", resourcePid)
                    problemList[resourcePid] = "Problem with resource could not update OBJ"
            else:
                log.error("multiple OBJ versions, pid: %s", resourcePid)
                problemList[resourcePid] = "multiple OBJ versions"
        except:
            log.exception("Error updating OBJ: pid = %s", resourcePid)
            problemList[resourcePid] = "Error updating OBJ"

    if "FITS" in args.datastreams:
        # check to see if we have the FITS ds already from the FGDC step if not get FITS output from the OBJ
        try:
            # if fits in (None, ''):
            if label in (None, '') and filename in (None, ''):
                filename, label, objVersion = getOBJ(resourcePid, deploymentPid)

            if label in ('Researcher Observations', 'Volunteer Observations', 'Image Observations'):
                log.debug("Found Observation!!! Skipping FITS update for resource %s", resourcePid)
            elif label not in (None, '') and filename not in (None, ''):
                cameraMake, cameraModel, fits = getFITS(filename)
                updateFITS(resourcePid, fits, deploymentPid)
            else:
                log.error("Problem with resource could not update FITS: pid = %s", resourcePid)
                problemList[resourcePid] = "Problem with resource could not update FITS"
        except:
            log.exception("Problem with resource could not update FITS: pid = %s", resourcePid)
            problemList[resourcePid] = "Problem with resource could not update FITS"

    if "SIDORA" in args.datastreams:
        try:
            if label in (None, '') and filename in (None, ''):
                filename, label, objVersion = getOBJ(resourcePid, deploymentPid)

            if label in ('Researcher Observations', 'Volunteer Observations', 'Image Observations'):
                log.debug("Found Observation!!! Skipping SIDORA update for resource %s", resourcePid)
            elif label not in (None, '') and filename not in (None, ''):
                imageId = os.path.splitext(label)[0]
                log.debug("check SpeciesScientificName for resource: %s, label: %s", resourcePid, imageId)
                xpath = str("boolean(.//ImageSequence[Image[ImageId/text() = '" + imageId + "']]/ResearcherIdentifications/Identification/SpeciesScientificName[contains('" + filterList + "', text())])")
                hasFace = manifest.xpath(xpath)

                sidoraDs = Template(sidoraTemplate).substitute(blurRequired=str(hasFace).lower(), isBlurred=str(isBlurred).lower(), sitePID=deploymentPid)
                updateSidora(resourcePid, sidoraDs, deploymentPid)
            else:
                log.error("Problem with resource could not update SIDORA: pid = %s", resourcePid)
                problemList[resourcePid] = "Problem with resource could not update SIDORA"
        except:
            log.exception("Problem with resource could not update SIDORA: pid = %s", resourcePid)
            problemList[resourcePid] = "Problem with resource could not update SIDORA"


    if not dryrun:
        os.remove(filename)
    else:
        log.debug("dry-run keep tmp file: %s", filename)


def updateDeployment(pid):
    log.info("Processing deployment: %s, updating datastreams %s", pid, args.datastreams)

    deploymentDatastreams = doGet(pid, None, False, None)

    log.debug("deployment objectDatastreams:\n%s", tostring(deploymentDatastreams, pretty_print=True).decode())

    hasMANIFEST = deploymentDatastreams.xpath("boolean(.//*[@dsid='MANIFEST'])", namespaces=ns)
    hasRELS_EXT = deploymentDatastreams.xpath("boolean(.//*[@dsid='RELS-EXT'])", namespaces=ns)

    log.debug("deployment: %s, hasMANIFEST: %s, hasRELS_EXT: %s", pid, hasMANIFEST, hasRELS_EXT)

    ## We can also check for deployment models: ['info:fedora/si:cameraTrapCModel', 'info:fedora/si:conceptCModel']
    # if hasRELS_EXT:
    #     deploymentRelsExt = doGet(pid, ds, True, None)
    #     deploymentModels = deploymentRelsExt.xpath(".//fs:hasModel/@rdf:resource", namespaces=ns)
    #     log.debug("deployment models: %s, pid: %s", deploymentModels, pid)
    #     hasDeploymentModels = "info:fedora/si:cameraTrapCModel" in deploymentModels and "info:fedora/si:conceptCModel" in deploymentModels
    #     log.info("hasDeploymentModels %s, pid: %s", hasDeploymentModels, pid)

    if hasMANIFEST:
        manifest = doGet(pid, "MANIFEST", True, None)
        # log.debug("update deployment manifest:\n%s", tostring(manifest, pretty_print=True).decode())

        if hasRELS_EXT:
            deploymentRelsExt = doGet(pid, "RELS-EXT", True, None)
            # log.debug(tostring(deploymentRelsExt, pretty_print=True).decode())

            # pid used for getting camera make and model
            cameraInfoResourcePid = deploymentRelsExt.xpath("substring-after(.//fedora:hasResource[1]/@rdf:resource, 'info:fedora/')", namespaces=ns)

            log.debug("deployment: %s, resource pid for camera make and model: %s", pid, cameraInfoResourcePid)

            resourceList = deploymentRelsExt.xpath(".//fedora:hasResource/@rdf:resource", namespaces=ns)
            resourceList = [p.split("info:fedora/")[1] for p in resourceList]

            with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
                futures = [executor.submit(doUpdate, pid, resourcePid, cameraInfoResourcePid, deploymentDatastreams, manifest) for resourcePid in resourceList]
                concurrent.futures.wait(futures, return_when=ALL_COMPLETED)

            # for resourcePid in resourceList:
            #     doUpdate(pid, resourcePid, cameraInfoResourcePid, deploymentDatastreams, manifest)

            log.info("Finished Updating FGDC and Resources for Deployment %s", pid)

        else:
            log.error("Deployment has no RELS-EXT datastream: pid = %s", pid)
            problemList[pid] = "Deployment has no RELS-EXT datastream"
    else:
        log.error("Deployment has no MANIFEST datastream: pid = %s", pid)
        problemList[pid] = "Deployment has no MANIFEST datastream"


def createDeploymentPidFile():
    start = time.time() # let's see how long this takes

    findDeployments(ct_root, "RELS-EXT")  # fuseki found 18284 deployments
    # findDeployments("si:139944", "RELS-EXT")  # should find 61 deployments

    finish = time.time()
    log.debug("time by parallelizing %s:", (finish-start))

    # log.info("deploymentList: %s", deploymentList)

    ctPIDCount = len(deploymentList)
    log.info("deployments found: %s", str(ctPIDCount))

    log.info("Saving pid list to %s", pid_output_file)
    with open(str(pid_output_file), 'w') as f:
        for item in deploymentList:
            f.write("%s\n" % item)
    f.close()


def findDeployments(pid, ds):

    objectDatastreams = doGet(pid, None, False, None)

    isDeployment = objectDatastreams.xpath(".//*[@dsid='FGDC'] and .//*[@dsid='MANIFEST']", namespaces=ns)
    log.debug("pid: %s, isDeployment: %s", pid, isDeployment)
    hasRelsExt = objectDatastreams.xpath("boolean(.//*[@dsid='RELS-EXT'])", namespaces=ns)

    ## We can also look for deployment models: ['info:fedora/si:cameraTrapCModel', 'info:fedora/si:conceptCModel']
    # if hasRelsExt:
    #     deploymentRelsExt = doGet(pid, ds, True, None)
    #     deploymentModels = deploymentRelsExt.xpath(".//fs:hasModel/@rdf:resource", namespaces=ns)
    #     log.debug("deployment models: %s, pid: %s", deploymentModels, pid)
    #     hasDeploymentModels = "info:fedora/si:cameraTrapCModel" in deploymentModels and "info:fedora/si:conceptCModel" in deploymentModels
    #     log.info("hasDeploymentModels %s, pid: %s", hasDeploymentModels, pid)

    if not isDeployment and hasRelsExt:
        relsExtDs = doGet(pid, ds, True, None)
        resourceList = relsExtDs.xpath(".//fedora:hasConcept/@rdf:resource", namespaces=ns)

        resourceList = [p.split("info:fedora/")[1] for p in resourceList]

        log.debug("checking pid: %s, for deployments... %s, resources: %s", pid, ds, resourceList)

        with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
            futures = [executor.submit(findDeployments, pid, ds) for pid in resourceList]
            concurrent.futures.wait(futures, return_when=ALL_COMPLETED)

    else:
        deploymentList.append(pid)
        log.info("adding %s, deployment count: %s", str(pid), len(deploymentList))

    return


def required_length(nmin,nmax):
    class RequiredLength(argparse.Action):
        def __call__(self, parser, args, values, option_string=None):
            if not nmin<=len(values)<=nmax:
                msg='argument "{f}" requires between {nmin} and {nmax} arguments'.format(
                    f=self.dest,nmin=nmin,nmax=nmax)
                raise argparse.ArgumentTypeError(msg)
            setattr(args, self.dest, values)
    return RequiredLength

def dumper(obj):
    try:
        return obj.toJSON()
    except:
        return obj.__dict__

def initHttp():
    global r
    r = requests.Session()
    retry = Retry(total=retries,
                  read=retries,
                  connect=retries,
                  backoff_factor=backoff_factor,
                  status_forcelist=status_forcelist)
    r.mount('http://', HTTPAdapter(pool_connections=100,
                                   pool_maxsize=250, pool_block=True, max_retries=retry))

def yes_or_no(question):
    while "the answer is invalid":
        reply = str(input(question+' (y/n): ')).lower().strip()
        if reply[:1] == 'y':
            return True
        if reply[:1] == 'n':
            return False


def main():
    if deploymentList is not None:
        log.info("Start processing")

        if args.deleteAll:
            delete = True
        else:
            delete = yes_or_no("Removing any existing files from '" + output_dir + "' y directory!!!")

        if delete:
            for f in os.listdir(output_dir):
                log.warning("deleting dry-run file = %s/%s", output_dir, f)
                if os.path.isfile(output_dir + "/" + f):
                    os.remove(output_dir + "/" + f)
                elif os.path.isdir(output_dir + "/" + f):
                    shutil.rmtree(output_dir + "/" + f)
        else:
            log.warning("Existing will be overwritten!!!!!")
            time.sleep(15)

        start = time.time()  # let's see how long this takes

        with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
            futures = [executor.submit(updateDeployment, pid) for pid in deploymentList]
            concurrent.futures.wait(futures, return_when=ALL_COMPLETED)

        # updateDeployment("si:139946")
        # updateDeployment("si:1006954")
        # updateDeployment("test.smx.home:74")

        finish = time.time()
        log.info("Finished Updating all Deployments... elapsed time: %s:", (finish-start))
    else:
        sys.exit("Error pid list is empty!!!")

    # log.info(problemList)
    if problemList:
        w = csv.writer(open(output_dir + "/problemList.csv", "w"))
        for key, val in problemList.items():
            w.writerow([key, val])
        log.error("Problem pid list\n%s", problemList)


def str2bool(v):
    if isinstance(v, bool):
        return v
    if v.lower() in ('yes', 'true', 't', 'y', '1'):
        return True
    elif v.lower() in ('no', 'false', 'f', 'n', '0'):
        return False
    else:
        raise argparse.ArgumentTypeError('Boolean value expected.')

# TODO: Update FGDC
#  get FGDC and RELS-EXT
#  run one resource thru FITS and get make/model,
#  add make/model to FGDC via transform
#  update FGDC ds

# TODO: Update FITS
#  get RELS-EXT
#  update each resource FITS ds

# TODO: FaceBlur
#  get MANIFEST and RELS-EXT ds
#  get resources that need face blur
#  blur img and replace OBJ (may need to remove OBJ versions)
#  set not version-able

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Update Legacy CameraTrap Data for WildLife Insights')
    parser.add_argument("-i", "--infile", help="file name containing list of deployment pids", action="store")
    parser.add_argument("-o", "--outfile", help="output file name for list of deployment pids (default is deploymentList.csv)", action="store", default="deploymentList.csv")
    parser.add_argument("-ds", "--datastreams", help="datastreams to update i.e. FGDC, FITS, OBJ, SIDORA", nargs='+', action=required_length(1,4), choices={'FGDC', 'FITS', 'OBJ', 'SIDORA'})
    parser.add_argument("-v", "--verbose", help="increase output verbosity", action="store_true")
    parser.add_argument("-dr", "--dry-run", help="Store datastream changes to local file system instead of updating Fedora datastreams (default dir ./output)", type=str2bool, nargs='?', const=True, default=True)
    parser.add_argument("-V", "--validate", help="Enable Validation", type=str2bool, nargs='?', const=True, default=True)
    parser.add_argument("-dA", "--deleteAll", help="Remove all existing files from output dir.", type=str2bool, nargs='?', const=True, default=False)
    parser.add_argument("-G", "--group_resources", help="Group dry-run datastream output to individual directories", type=str2bool, nargs='?', const=True, default=False)

    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s]  %(message)s",
                        handlers=[
                            logging.FileHandler("updateLegacyCT.log"),
                            logging.StreamHandler(sys.stdout)
                        ])
    log = logging.getLogger(__name__)

    if args.verbose:
        log.setLevel(logging.DEBUG)
        log.debug("verbosity enabled: %s", str(args.verbose))
    else:
        log.setLevel(logging.INFO)

    log.info(args)

    # initialize
    if os.path.exists('update.properties'):
        config = configparser.RawConfigParser()
        config.read('update.properties')

        log.debug("using properties:\n%s",
                  json.dumps(
                      {
                          defaults: dict(config.items('defaults')) for defaults in config.sections()
                      },
                      default=dumper,
                      indent=2)
                  )

        fedora_user = config.get("defaults", "fedora.user")
        fedora_pass = config.get("defaults", "fedora.password")
        HOST = config.get("defaults", "fedora.host")
        ct_root = config.get("defaults", "ct.root")
        output_dir = config.get("defaults", "output.dir")
        threads = config.getint("defaults", "threads")
        retries = config.getint("defaults", "retries")
        backoff_factor = config.get("defaults", "backoff_factor")
        status_forcelist = tuple(map(int, config.get("defaults", "status_forcelist").split(', ')))
        filterList = config.get("defaults", "speciesScientificName.filter")
        blurValue = config.getint("defaults", "blur_value")
        classifier = config.get("defaults", "classifier")
        exiftool_path = config.get("defaults", "exiftool.path")
        pool_block = config.getboolean("defaults", "pool.block")
        log.info("pool_block = %s", pool_block)
        pooled_connections = config.get("defaults", "pooled.connections")
        polled_maxsize = config.get("defaults", "polled.maxsize")
        sidoraTemplate = open(config.get("defaults", "sidora.template"), "r+").read()
    else:
        sys.exit("missing config properties file!!!")


    # http endpoints
    FEDORA_URL = "http://" + HOST + ":8080/fedora"
    FUSEKI_URL = "http://" + HOST + ":9080/fuseki/fedora3"
    FITS_URL = "http://" + HOST + ":8080/fits-1.1.3/examine"

    initHttp()

    if args.dry_run:
        log.warning("dryrun on: %s", str(args.dry_run))
        dryrun = True
    else:
        dryrun = False

    if args.infile:
        if os.path.exists(args.infile):
            log.info("using infile name: %s", args.infile)
            deploymentList = [line.rstrip('\n') for line in open(args.infile)]
            log.debug("deployment pids:\n%s", deploymentList)
        else:
            sys.exit("Error: infile does not exist!!!")
    else:
        log.info("Starting find deployment pids...")
        if args.outfile:
            pid_output_file = args.outfile
            deploymentList = list()
            createDeploymentPidFile()
            log.info("Finished creating deployment PID list...")
            # sys.exit()
        else:
            sys.exit("ERROR: Must provide an infile containing list of PIDS to update OR outfile name for deployment pids to be saved to!!!")

    if args.datastreams:
        problemList = dict()
        main()
    else:
        sys.exit("No further processing. Datastreams not provided!!!")

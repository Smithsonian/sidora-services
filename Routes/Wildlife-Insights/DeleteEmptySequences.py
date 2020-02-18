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
import rdflib
from rdflib.namespace  import RDF
from concurrent.futures import ALL_COMPLETED
from string import Template

import datetime as datetime
import requests
from lxml.etree import fromstring, tostring, parse, XSLT
from requests.adapters import HTTPAdapter
from requests.auth import HTTPBasicAuth
# from requests.packages.urllib3.util.retry import Retry
from urllib3 import Retry

ns = {"fsmgmt": "http://www.fedora.info/definitions/1/0/management/",
      "fedora": "info:fedora/fedora-system:def/relations-external#",
      "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "ri": "http://www.w3.org/2005/sparql-results#",
      "fits": "http://hul.harvard.edu/ois/xml/ns/fits/fits_output",
      "fs": "info:fedora/fedora-system:def/model#"}

global FEDORA_URL
global FUSEKI_URL
global FITS_URL
global deploymentLogs

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
def doGet(pid, ds, content):
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

    if 'application/json' in content_type.lower():
        if ds == "CSV":
            log.info("CSV Found - is JSON")
        # extracting data in json format
        fuseki_data = req.json()
        log.debug(fuseki_data)
        return fuseki_data["results"]["bindings"]
    else:
        if ds == "CSV":
            if req.content is not None:
                return str(req.content)
        response = fromstring(req.content)
        if ds == "CSV":
            log.info("CSV Found")
        log.debug("pid: %s, response:\n%s", pid, tostring(response, pretty_print=True).decode())
        return response

def doPut(pid, data, ds, params):
    FEDORA_PARAMS = params

    URL = FEDORA_URL + "/objects/" + pid + "/datastreams/" + ds

    req = r.put(url=URL, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))
    log.info("pid: %s, url: %s", pid, req.url + "<--------------------")
    response = fromstring(req.content)
    log.debug("put to url: %s, response: %s", req.url, tostring(response, pretty_print=True).decode())
    return response


def doDelete(pid, params):
    try:
        passed = True;
        FEDORA_PARAMS = params

        URL = FEDORA_URL + "/objects/" + pid

        req = r.delete(url=URL, params=FEDORA_PARAMS, auth=HTTPBasicAuth(fedora_user, fedora_pass))

        log.info("pid: %s, url: %s", pid, req.url + "<--------------------")
        response = fromstring(req.content)
        if req.status_code != 200:
            passed = False

        log.debug("delete at url: %s, response: %s", req.url, tostring(response, pretty_print=True).decode())

        return [passed, response]
    except Exception as exc:
        log.error("Error in delete api call: " + str(exc))
        return [False, str(exc)]


def getOBJ(pid):

    objLabel = None
    fileName = None

    if pid not in (None, ''):
        objDatastreamProfile = doGet(pid, "OBJ", False)

        objMIME = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsMIME/text()", namespaces=ns)[0]
        objLabel = objDatastreamProfile.xpath("//fsmgmt:datastreamProfile/fsmgmt:dsLabel/text()", namespaces=ns)[0]
        log.debug("OBJ mimeType: %s label: %s", objMIME, objLabel)

        if "image" in objMIME:
            log.debug("downloading %s OBJ...", pid)
            # Save OBJ to tmp file
            fileName = doGet(pid, "OBJ", False)
        elif "csv" in objMIME:
            log.debug("Found Observation!!! Skipping Downloading Resource %s", pid)
        else:
            log.error("Problem resource: mimeType= %s resource pid = %s", objMIME, pid)
            problemList[pid] = "resource is not an image. mimeType=" + objMIME
    else:
        log.error("no resources %s", pid)
        problemList[pid] = "resources pid is null"

    return fileName, objLabel, objMIME


def removeOBJ(resourcePid):
    log.info("%s is an empty sequence image resource. Saving to file")
    #TODO: Verify purge object or pure obj datastream
    resultDelete = doDelete(resourcePid, {'versionable': 'true'})
    if resultDelete not in (None, ""):
        log.info("http DELETE, OBJ remove, pid: %s response:\n%s", resourcePid, tostring(resultDelete, pretty_print=True).decode())
    log.info("Finished removeOBJ for resource %s", resourcePid)


def doUpdate(deploymentPid, resourcePid, csvfields):
    #log.info("doUpdate for " + deploymentPid)
    filename = None
    label = None

    try:
        # TODO: check manifest
        if filename in (None, ''):
            filename, label, mime = getOBJ(resourcePid)

        log.debug("OBJ pid: %s", resourcePid)
        deploymentLogs[deploymentPid].append("OBJ pid: " + resourcePid)

        if label not in (None, '') and filename not in (None, ''):
            imageId = os.path.splitext(label)[0]
            isEmpty = False

            for row in csvfields:
                if len(row) > 1:
                    # pull out the sequence id and check against empty sequence list
                    sequenceId = row[2].strip()
                    if sequenceId in imageId:
                        #log.info("Sequence species scientific name: " + row[5])
                        deploymentLogs[deploymentPid].append("Sequence species scientific name: " + row[5])
                        seqName = row[5].replace("\"", "")
                        if seqName == "No Animal" or seqName == "Blank" or seqName == "Camera Misfire" or seqName == "False trigger" or seqName == "Time Lapse":
                            log.debug(imageId + " is part of empty sequence " + row[2] + " - scientific name is " + row[5])
                            deploymentLogs[deploymentPid].append(imageId + " is part of empty sequence " + row[2] + " - scientific name is " + row[5])
                            isEmpty = True
            log.debug("check SpeciesScientificName for resource: %s, label: %s", resourcePid, imageId)

            if isEmpty:
                log.info("Empty pid found for resource: " + resourcePid);
                deploymentLogs[deploymentPid].append("Empty pid found for resource: " + resourcePid)
                if not dryrun:
                    response = removeOBJ(resourcePid)
                    isEmpty = response[0]
            else:
                log.debug("Resource %s is not empty. Adding to list for updated RELS-EXT", resourcePid)
                deploymentLogs[deploymentPid].append("Resource " + resourcePid +" is not empty. Adding to list for updated RELS-EXT")
            return [resourcePid, label, isEmpty, mime]
        else:
            log.error("Problem with resource could not update OBJ: pid = %s", resourcePid)
            deploymentLogs[deploymentPid].append("Problem with resource could not update OBJ: pid = " + resourcePid)
            problemList[resourcePid] = "Problem with resource could not update OBJ"
    except:
        log.exception("Error updating OBJ: pid = %s", resourcePid)
        deploymentLogs[deploymentPid].append("Error updating OBJ: pid = " + resourcePid)
        problemList[resourcePid] = "Error updating OBJ"


def updateDeployment(pid):
    deploymentLogs[pid] = []
    deploymentDatastreams = doGet(pid, None, False)

    log.debug("deployment objectDatastreams:\n%s", tostring(deploymentDatastreams, pretty_print=True).decode())
    deploymentLogs[pid].append("log output for deployment: " + pid)
    log.info("deploymentLogs[" + pid + "] initialized")

    hasMANIFEST = deploymentDatastreams.xpath("boolean(.//*[@dsid='MANIFEST'])", namespaces=ns)
    hasRELS_EXT = deploymentDatastreams.xpath("boolean(.//*[@dsid='RELS-EXT'])", namespaces=ns)

    if hasMANIFEST:
        manifest = doGet(pid, "MANIFEST", True)
        if hasRELS_EXT:

            isParseable = False
            hasResources = False

            deploymentRelsExt = doGet(pid, "RELS-EXT", True)
            graph = rdflib.Graph()
            graph.parse(data=tostring(deploymentRelsExt), format='xml')
            if graph:
                isParseable = True
            resourceList = []
            # Get all resource nodes and compile them into a list
            for s, p, o in graph.triples((None, None, None)):
                if "hasResource" in p:
                    resourceList.append(str(o))
            resourceList = [p.split("info:fedora/")[1] for p in resourceList]
            #if length of resourceList is greater than true, this check passes
            if len(resourceList) > 0:
                hasResources = True

            keepList = list()
            emptyList = list()

            for resource in resourceList:
                csv_pid = ""

                # get the pid for the researcher observation csv pid
                for i in resourceList:
                    obj = doGet(i, "DC", True)
                    if "Researcher Observations" in str(tostring(obj)):
                        csv_pid = i
                        break
                # get csv datastream and process
                csvvals = doGet(csv_pid, "CSV", "True")
                csvrows = csvvals.split(r"\n")
                csvfields = []
                for row in csvrows:
                    csvfields.append(row.split(","))
                data = doUpdate(pid, resource, csvfields)
                if data not in (None, ''):
                    crumbtrailValues = getDeploymentCrumbtrail(data[0], data[1])
                    resourcePidCrumbtrail = crumbtrailValues[0]
                    resourceLabelCrumbtrail = crumbtrailValues[1]
                    if data[2]:
                        log.debug("data[1] is: %s, adding to emptyList", str(data[1]))
                        deploymentLogs[pid].append("data[1 is: " + str(data[1]) + ", adding to emptyList")
                        emptyList.append([resourcePidCrumbtrail, resourceLabelCrumbtrail])
                    else:
                        log.debug("data[1] is %s, adding to keepList", str(data[1]))
                        deploymentLogs[pid].append("data[1 is: " + str(data[1]) + ", adding to keepList")
                        keepList.append([resourcePidCrumbtrail, resourceLabelCrumbtrail])
            gf = graph
            #for each breadcrumb collection of pids, check if it matches the object
            #if it does, remove from rels-ext
            for epid in emptyList:
                bc = epid[0]
                bcpids = bc.split("_")
                pd = bcpids[len(bcpids) - 1]
                for t in gf.triples((None, None, None)):
                    if pd in str(t):
                        gf.remove(t)
            #if not dryrun and the edited graph is not null and there are empty sequences found, update rels-ext
            if not dryrun and gf is not None and hasResources and isParseable:
                updateRELS_EXT(gf.serialize(format='nt'), pid)

            log.info("Finished Updating Resources for Deployment %s", pid)
            deploymentLogs[pid].append("Finished Updating Resources for Deployment " + pid)
            if(len(emptyList)):
                return [keepList, emptyList, pid]
            else:
                return [keepList, emptyList, None]
        else:
            log.error("Deployment has no RELS-EXT datastream: pid = %s", pid)
            deploymentLogs[pid].append(("Deployment has no RELS-EXT datastream: pid = " + pid))
            problemList[pid] = "Deployment has no RELS-EXT datastream"
    else:
        log.error("Deployment has no MANIFEST datastream: pid = %s", pid)
        deploymentLogs[pid].append("Deployment has no MANIFEST datastream: pid = " + pid)
        problemList[pid] = "Deployment has no MANIFEST datastream"
    log.info("Reached end of deployment processing for pid: " + pid)

def writeToDeploymentLog(pid):
    log.info("writing to deployment logs for deployment: " + pid)
    date = datetime.datetime.now()
    filename = str(date.day) + "-" + str(date.month) + "-" + str(date.year) + "-" + str(date.hour) + ":" + str(
        date.minute) + ":" + str(date.second) + "_deployment-" + str(pid) + ".log"
    with open(output_dir + "/" + filename, 'w') as f:
        for item in deploymentLogs[pid]:
            f.write("%s\n" % item)

def updateRELS_EXT(RELS_EXT_DS, deploymentPid):
    try:
        result = doPut(deploymentPid, RELS_EXT_DS, "RELS-EXT", {'mimeType': 'text/xml', 'versionable': "true"})
        log.info("http PUT, RELS_EXT update, pid: %s response:\n%s", deploymentPid, tostring(result, pretty_print=True).decode())
        deploymentLogs[deploymentPid].append("http PUT, RELS_EXT update, pid: " + deploymentPid + " response:\n" + tostring(result, pretty_print=True).decode())
    except Exception as e:
        log.error("Error updating RELS-EXT: %s", str(e))
        deploymentLogs[deploymentPid].append("Error updating RELS-EXT: " + str(e))
    log.info("Finished updateRELS_EXT for deployment %s", deploymentPid)
    deploymentLogs[deploymentPid].append("Finished updateRELS_EXT for deployment " + deploymentPid)


def getDeploymentCrumbtrail(pid, label):
    try:
        query = "SELECT ?ctPID ?ctLabel ?sitePID ?siteLabel ?parkPID ?parkLabel ?projectPID ?projectLabel ?subprojectPID ?subprojectLabel FROM <info:edu.si.fedora#ri> WHERE { ?ctPID <info:fedora/fedora-system:def/relations-external#hasResource> <info:fedora/" + pid + "> ; <info:fedora/fedora-system:def/model#label> ?ctLabel . OPTIONAL { ?sitePID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#hasModel> <info:fedora/si:ctPlotCModel> ; <info:fedora/fedora-system:def/model#label> ?siteLabel . ?parkPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?sitePID ; <info:fedora/fedora-system:def/model#label> ?parkLabel } . OPTIONAL { ?plotPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#label> ?plotLabel . ?parkPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#label> ?parkLabel } . ?projectPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?parkPID ; <info:fedora/fedora-system:def/model#label> ?projectLabel . ?subprojectPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#label> ?subprojectLabel OPTIONAL { ?plotPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#label> ?plotLabel . ?parkPID <info:fedora/fedora-system:def/relations-external#hasConcept> ?ctPID ; <info:fedora/fedora-system:def/model#label> ?parkLabel } . }"
        # defining a params dict for the parameters to be sent
        FUSEKI_PARAMS = {'query': query}

        # sending get request and saving the response as response object
        fuseki_request = r.get(url=FUSEKI_URL, params=FUSEKI_PARAMS)
        # extracting data in json format
        fuseki_data = fuseki_request.json()

        bindings = fuseki_data["results"]["bindings"][0]

        # extract PIDS
        deploymentPID = bindings["ctPID"]["value"].replace("info:fedora/", "")

        try:
            plotPID = "_" + bindings["plotPID"]["value"].replace("info:fedora/", "") + "-"
        except:
            plotPID = "_"

        subprojectPID = bindings["subprojectPID"]["value"].replace("info:fedora/", "")
        projectPID = bindings["projectPID"]["value"].replace("info:fedora/", "")

        pidCrumbtrail = projectPID + "_" + subprojectPID + plotPID + deploymentPID + "_" + pid

        #extract Labels

        deploymentLabel = bindings["ctLabel"]["value"]

        try:
            plotLabel = "_" + bindings["plotLabel"]["value"] + "_"
        except:
            plotLabel = "_"

        subprojectLabel = bindings["subprojectLabel"]["value"]
        projectLabel = bindings["projectLabel"]["value"]

        labelCrumbtrail = projectLabel + "_" + subprojectLabel + plotLabel + deploymentLabel + "_" + label

        return [pidCrumbtrail, labelCrumbtrail]
    except Exception as e:
        log.error("Error retrieving fuseki data: %s", str(e))
        deploymentLogs[pid].append("Error retrieving fuseki data: " + str(e))
        return ["", ""]

def createDeploymentPidFile():
    start = time.time() # let's see how long this takes

    findDeployments(ct_root, "RELS-EXT")  # fuseki found 18284 deployments
    # findDeployments("si:139944", "RELS-EXT")  # should find 61 deployments

    finish = time.time()
    log.debug("time by parallelizing %s:", (finish-start))

    log.debug("deploymentList: %s", deploymentList)

    ctPIDCount = len(deploymentList)
    log.info("deployments found: %s", str(ctPIDCount))

    log.info("Saving pid list to %s", pid_output_file)
    with open(str(pid_output_file), 'w') as f:
        for item in deploymentList:
            f.write("%s\n" % item)
    f.close()


def findDeployments(pid, ds):
    objectDatastreams = doGet(pid, None, False)

    isDeployment = objectDatastreams.xpath(".//*[@dsid='FGDC'] and .//*[@dsid='MANIFEST']", namespaces=ns)
    log.debug("pid: %s, isDeployment: %s", pid, isDeployment)
    hasRelsExt = objectDatastreams.xpath("boolean(.//*[@dsid='RELS-EXT'])", namespaces=ns)

    if not isDeployment and hasRelsExt:
        relsExtDs = doGet(pid, ds, True)
        #resourceList = relsExtDs.xpath(".//fedora:hasConcept/@rdf:resource", namespaces=ns)
        graph = rdflib.Graph()
        graph.parse(data=tostring(relsExtDs), format='xml')
        resourceList = []
        for s, p, o in graph.triples((None, None, None)):
            if "hasConcept" in p:
                resourceList.append(str(o))
        log.debug("Length of resource list: " + str(len(resourceList)))

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

        #if args.deleteAll:
        #   delete = True
        #else:
        #    delete = yes_or_no("Removing any existing files from '" + output_dir + "' y directory!!!")
        #delete = True

        #if delete:
            #for f in os.listdir(output_dir):
                #log.warning("deleting dry-run file = %s/%s", output_dir, f)
                #if os.path.isfile(output_dir + "/" + f):
                #   os.remove(output_dir + "/" + f)
                #elif os.path.isdir(output_dir + "/" + f):
                #    shutil.rmtree(output_dir + "/" + f)
        #else:
        #    log.warning("Existing will be overwritten!!!!!")
        #    time.sleep(15)

        start = time.time()  # let's see how long this takes
        removeList = list()
        keepList = list()
        emptyDeployments = list()

        with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
            futures = [executor.submit(updateDeployment, pid) for pid in deploymentList]
            concurrent.futures.wait(futures, return_when=ALL_COMPLETED)

        # updateDeployment("si:139946")
        # updateDeployment("si:1006954")
        # updateDeployment("test.smx.home:74")

        finish = time.time()

        for future in concurrent.futures.as_completed(futures):
            try:
                data = future.result()
            except Exception as exc:
                log.error("An error occurred in retrieving future from updateDeployment: %s", str(exc))
            else:
                if data not in (None, ''):
                    #print('adding good pid to RELS-EXT list %s', data)
                    removeList = removeList + data[1]
                    keepList = keepList + data[0]
                    if data[2]:
                        emptyDeployments.append(data[2])

        for dep in deploymentLogs:
            writeToDeploymentLog(dep)

        date = datetime.datetime.now()
        date_string = str(date.day) + "-" + str(date.month) + "-" + str(date.year) + "-" + str(date.hour) + ":" + str(
            date.minute) + ":" + str(date.second)

        log.debug("Length of removeList is %d", len(removeList))
        log.debug("Date String is: " + date_string)

        emptiesFileName = "empty_sequence_output_" + date_string + ".csv"
        log.debug("emptiesFileName: " + emptiesFileName)

        if not os.path.exists(os.path.dirname(output_dir + "/" + emptiesFileName)):
            os.makedirs(os.path.dirname(output_dir + "/" + emptiesFileName))

        w = csv.writer(open(output_dir + "/" + emptiesFileName, "w"))
        w.writerow(["dryrun: " + str(dryrun)])
        w.writerow(["Objects Purged: "])
        w.writerow(["count: " + str(len(removeList))])
        for removeSet in removeList:
            w.writerow([removeSet[0], removeSet[1]])
        w.writerow(["Objects Preserved: "])
        w.writerow(["count: " + str(len(keepList))])
        for keepSet in keepList:
            w.writerow([keepSet[0], keepSet[1]])
        w.writerow(["Deployments with Empty Sequences: "])
        w.writerow(["count: " + str(len(emptyDeployments))])
        for deployment in emptyDeployments:
            w.writeRow([deployment])
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
                            logging.FileHandler("DeleteEmptySequences.log"),
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
        #filterList = config.get("defaults", "speciesScientificName.filter")
        blurValue = config.getint("defaults", "blur_value")
        classifier = config.get("defaults", "classifier")
        exiftool_path = config.get("defaults", "exiftool.path")
        pool_block = config.getboolean("defaults", "pool.block")
        log.info("pool_block = %s", pool_block)
        pooled_connections = config.get("defaults", "pooled.connections")
        polled_maxsize = config.get("defaults", "polled.maxsize")
        relsExtTemplate = open(config.get("defaults", "relsext.template"), "r+").read()
    else:
        sys.exit("missing config properties file!!!")


    # http endpoints
    FEDORA_URL = "http://" + HOST + ":8080/fedora"
    FUSEKI_URL = "http://" + HOST + ":9080/fuseki/fedora3"
    FITS_URL = "http://" + HOST + ":8080/fits-1.1.3/examine"

    deploymentLogs = {}

    initHttp()

    # dryrun = False

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
    problemList = dict()
    main()

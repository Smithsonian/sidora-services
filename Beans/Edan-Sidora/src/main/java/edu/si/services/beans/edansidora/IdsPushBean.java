/*
 * Copyright 2015-2016 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 */

package edu.si.services.beans.edansidora;

import org.apache.camel.Header;
import org.apache.camel.PropertyInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IdsPushBean {
	static final int BUFFER = 2048;

	//@PropertyInject(value = "si.ct.uscbi.idsPushLocation")
	private String pushLocation = null;

	private static String inputLocation = null;

	private static String deploymentId = null;

	//@PropertyInject(value = "si.ct.uscbi.tempLocationForZip")
	private String tempLocation = null;

	private Map<String, String> ignored = new HashMap<String, String>();

	private static final Logger LOG = LoggerFactory.getLogger(IdsPushBean.class);

	public Map<String, String> createZipAndPush() {
		String errorName = "";
		String errorInfo = "";
		String completed = "0";
		String completedInformation = "Not started";

		try {
			BufferedInputStream origin = null;
			String destFilename = "ExportEmammal_emammal_image_"+this.deploymentId+".zip";
			String destLocation = this.tempLocation + destFilename;
			FileOutputStream dest = new FileOutputStream(destLocation);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			completedInformation = "Started Zip file.";

			//out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			// get a list of files from current directory
			String currDir = this.inputLocation;
			File f = new File(currDir);
			completedInformation = "File for list:"+f;
			String files[] = f.list();
			if (files == null){
				// Was probably given a file in the directory instead of the directory itself.
				files = f.getParentFile().list();
				currDir = f.getParentFile().getAbsolutePath() + File.separator;
			}
			completedInformation = "Read file list:"+files;
			String assetXmlText = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n<Assets>";
			for (int i=0; i<files.length; i++) {
				completedInformation = "Started file:"+files[i];
				// Do not include the manifest file
				if (
						!files[i].endsWith(".xml") 
						&& (files[i].indexOf(".") > -1)
						&& (ignored.get(files[i].substring(0,files[i].length()-4)) == null)
					){
					System.out.println("Adding: "+files[i]);
					FileInputStream fi = new FileInputStream(currDir + files[i]);
					origin = new BufferedInputStream(fi, BUFFER);
					String zipEntryName = "emammal_image_" + files[i];
					ZipEntry entry = new ZipEntry(zipEntryName);
					out.putNextEntry(entry);
					int count;
					while((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
					}
					origin.close();
					assetXmlText += "\r\n  <Asset Name=\"";
					assetXmlText += zipEntryName.substring(0,zipEntryName.length()-4);
					assetXmlText += "\" IsPublic=\"Yes\" IsInternal=\"No\" MaxSize=\"3000\" InternalMaxSize=\"4000\">";
					assetXmlText += zipEntryName;
					assetXmlText += "</Asset>";
				}
			}
			assetXmlText += "\r\n</Assets>";
			ZipEntry entry = new ZipEntry("ExportEmammal_emammal_image_"+this.deploymentId+".xml");
			out.putNextEntry(entry);
			out.write(assetXmlText.getBytes());
			out.close();
			completedInformation = "Closed Zip File";
			File zipFile = new File(destLocation);
			copyFileUsingFileStreams(zipFile, new File(this.pushLocation+destFilename));
			completedInformation = "Copied Zip File to:"+this.pushLocation+destFilename;
			boolean zipDeleted = zipFile.delete();
			completedInformation = "Removed Temp Zip File? "+zipDeleted;
			completed = "1";
		} catch(Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			errorName = e.toString();
			errorInfo = sw.toString();
		}
		Map<String, String> toReturn = new HashMap<String, String>();
		toReturn.put("errorString", errorName);
		toReturn.put("stackTrace", errorInfo);
		toReturn.put("completed", completed);
		toReturn.put("completionInformation", completedInformation);
		return toReturn;
	}
	
	public void addToIgnoreList(String imageId){
      ignored.put(imageId, imageId);		
	}

	public String getPushLocation() {
		return pushLocation;
	}

	public void setPushLocation(String pushLocation) {
		this.pushLocation = pushLocation;
	}

	public String getInputLocation() {
		return inputLocation;
	}

	public void setInputLocation(String inputLocation) {
		this.inputLocation = inputLocation;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public static void main (String argv[]) {
		IdsPushBean ipb = new IdsPushBean();
		ipb.setInputLocation("C:\\temp\\inputLoc\\deployment.xml");
		ipb.setTempLocation("C:\\temp\\");
		ipb.setDeploymentId("testDeploymentId");
		ipb.setPushLocation("C:\\temp\\finalLoc\\");
		ipb.addToIgnoreList("ignoreme");
		Map<String, String> returned = ipb.createZipAndPush();
		for(Map.Entry<String, String> entry : returned.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
		    System.out.println(key+"\t"+value);
		}
		System.out.println("done");
	}

	public String getTempLocation() {
		return tempLocation;
	}

	public void setTempLocation(String tempLocation) {
		this.tempLocation = tempLocation;
	}
	/*
	 *  had issues using Files.copy resulting in
	 * java.lang.UnsatisfiedLinkError: no nio in java.library.path
	 * So, we'll do it manually...
	 */

	private static void copyFileUsingFileStreams(File source, File dest)
			throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output.close();
		}
	}	

}

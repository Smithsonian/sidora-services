package edu.si.services.edansidora;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IdsPushBean {
	static final int BUFFER = 2048;

	private String pushLocation = null;
	private String inputLocation = null;
	private String deploymentId = null;
	private String tempLocation = null; 


	public Map<String, String> createZipAndPush() {
		String errorName = "";
		String errorInfo = "";
		String completed = "0";
		String completedInformation = "Not started";
		try {
			BufferedInputStream origin = null;
			String destFilename = "ExportAssets_emammal_image_"+this.deploymentId+".zip";
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
				if (!files[i].endsWith(".xml")){
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
			ZipEntry entry = new ZipEntry("ExportAssets_emammal_image_"+this.deploymentId+".xml");
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

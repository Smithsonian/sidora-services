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

import org.apache.camel.PropertyInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A bean which we use in the route
 */
public class EdanApiBean {

	private static final Logger LOG = LoggerFactory.getLogger(EdanApiBean.class);

//	@PropertyInject(value = "si.ct.uscbi.server")
	private String server;

//	@PropertyInject(value = "si.ct.uscbi.appId")
	private String appId;

//	@PropertyInject(value = "si.ct.uscbi.edanKey")
	private String edanKey;

//	@PropertyInject(value = "si.ct.uscbi.authType", defaultValue = "1")
	private int authType;

	private EdanApi edanApi = null;

	public int getAuthType() {
		return authType;
	}

	public void setAuthType(int authType) {
		this.authType = authType;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getEdanKey() {
		return edanKey;
	}

	public void setEdanKey(String edanKey) {
		this.edanKey = edanKey;
	}

	public boolean startConnection() {
		if (this.appId == null || this.server == null || this.edanKey == null){
			return false;
		}
		this.edanApi = new EdanApi(this.server, this.appId, this.edanKey, this.authType);
		return true;
	}

	public Map<String,String> sendRequest(String edanUri, String service) {
		if (this.edanApi == null) return null;
		return  edanApi.sendRequest(edanUri, service);
	}

	public Map<String, String> sendRequest(String uri, String service, boolean post) {
		if (this.edanApi == null) return null;
		return edanApi.sendRequest(uri, service, post);
	}
	public Map<String, String> encodeHeaderSample(String uri) {
         if (this.edanApi == null) edanApi = new EdanApi("1","2","3");
		return edanApi.encodeHeaderSample(uri);
	}
	public String simpleReturnNonce() {
        if (this.edanApi == null) edanApi = new EdanApi("1","2","3");
		return edanApi.simpleReturnNonce();
	}
}

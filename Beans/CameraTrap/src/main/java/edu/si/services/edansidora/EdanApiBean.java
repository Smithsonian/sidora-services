package edu.si.services.edansidora;

import java.util.Map;

/**
 * A bean which we use in the route
 */
public class EdanApiBean   {

	private String server = null;
	private String appId = null;
	private String edanKey = null;
	private int authType = 1;
	public int getAuthType() {
		return authType;
	}

	public void setAuthType(int authType) {
		this.authType = authType;
	}

	private EdanApi edanApi = null;

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

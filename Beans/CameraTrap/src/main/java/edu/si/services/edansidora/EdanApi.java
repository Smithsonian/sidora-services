package edu.si.services.edansidora;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.cxf.common.util.Base64Utility;

import java.util.Date;
import java.util.HashMap;

/**
 * @file EDANInterface This is a pretty raw class that just generates a call to
 *       the EDAN API.
 *
 */

public class EdanApi {
  private String server;
  private String app_id;
  private String edan_key;
  /**
   * int indicates the authentication type/level 0 for unsigned/trusted/T1
   * requests; (currently unused) 1 for signed/T2 requests; 2 for password based
   * (unused)
   */
  private int auth_type = 1;

  /**
   * bool tracks whether the request was successful based on response header
   * (200 = success)
   */
  //private boolean valid_request = false;
  public String result_format = "json";

  /**
   * Constructor
   */
  public EdanApi(String server, String app_id, String edan_key, int auth_type) {
    build(server, app_id, edan_key, auth_type);
  }

  public EdanApi(String server, String app_id, String edan_key) {
    build(server, app_id, edan_key, 1);
  }

  private void build(String server, String app_id, String edan_key, int auth_type) {
    this.server = server;
    this.app_id = app_id;
    this.edan_key = edan_key;
    this.auth_type = auth_type;
    this.result_format = "json";
    //this.valid_request = false;
  }

  /**
   * Creates the header for the request to EDAN. Takes $uri, prepends a nonce,
   * and appends the date and appID key. Hashes as sha1() and base64_encode()
   * the result.
   * 
   * @param uri
   *          The URI (string) to be hashed and encoded.
   * @throws UnsupportedEncodingException
   * @throws NoSuchAlgorithmException
   * @returns Array containing all the elements and signed header value
   */
  private Map<String, String> encodeHeader(String uri) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    String ipnonce = this.get_nonce(); // Alternatively you could do:
                                       // get_nonce(8, '-'.get_nonce(8));
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String sdfDate = df.format(new Date());

    Map<String, String> toReturn = new HashMap<String, String>();
    toReturn.put("X-AppId", this.app_id);
    toReturn.put("X-RequestDate", sdfDate);
    toReturn.put("X-AppVersion", "EDANInterface-0.10.1");

    // For signed/T2 requests
    if (this.auth_type == 1) {
      String auth = "{" + ipnonce + "}\n{" + uri + "}\n{" + sdfDate + "}\n{" + this.edan_key + "}";
      String content = SimpleSHA1.SHA1plusBase64(auth);
      toReturn.put("X-Nonce", ipnonce);
      toReturn.put("X-AuthContent", content);
    }

    return toReturn;
  }
  public String simpleReturnNonce(){
    return this.get_nonce(); // Alternatively you could do:
  }
  public Map<String, String> encodeHeaderSample(String uri){
    try{
    	return encodeHeader(uri);
    }catch(Exception e){
    	Map<String, String> toReturn = new HashMap<String, String>();
    	toReturn.put("errorString", e.toString());
    	return toReturn;
    }
  }
  /**
   * Perform a http request
   * 
   * @param uri
   *          A string containing the querystring that gets appended to the
   *          service url
   * @param service
   *          The service name you are curling
   *          {metadataService,tagService,collectService}
   * @param POST
   *          boolean, defaults to false; on true $uri sent CURLOPT_POSTFIELDS
   * @param info
   *          reference, if passed will be set with the output of curl_getinfo
   */
  public Map<String, String> sendRequest(String uri, String service) {
    return this.sendRequest(uri, service, false);
  }
  
  public String testSendRequest(String uri, String service){
	  boolean post = false;
	    HttpURLConnection connection = null;
	    String targetURL = this.server + service;
	    String urlParameters = "";
	    Map<String, String> toReturn = new HashMap<String, String>();
	    try {
	      // Create connection
	      String requestMethod = "";
	      if (post) {
	        requestMethod = "POST";
	        urlParameters += ("encodedRequest=" + EdanApi.base64encode(uri));
	      } else {
	        requestMethod = "GET";
	        targetURL = targetURL + "?" + uri;
	      }
	      toReturn.put("targetURL", targetURL);
	      URL url = new URL(targetURL);
	      connection = (HttpURLConnection) url.openConnection();
	      connection.setRequestMethod(requestMethod);
	      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
	      //return "Went up to encode...";
	      Map<String, String> encodedHeaders = this.encodeHeader(uri);
	      return "Done encodedHeaders";
	    }catch(Exception e){
	    	return "Got exception: "+e.toString();
	    }
  }

  public Map<String, String> sendRequest(String uri, String service, boolean post){
	System.setProperty("http.agent", "");
	
	  //, Map<String, String> info) {
    // Hash the request for tracking/profiling/caching
    // String hash = Hash.md5(uri + service + (post ? "1" : "0"));
    HttpURLConnection connection = null;
    String targetURL = this.server + service;
    String urlParameters = "";
    Map<String, String> toReturn = new HashMap<String, String>();
    try {
      // Create connection
      String  requestMethod = "GET";
      targetURL = targetURL + "?" + uri;
      
      toReturn.put("targetURL", targetURL);
      URL url = new URL(targetURL);
      connection = (HttpURLConnection) url.openConnection();
      //connection.setRequestProperty("User-Agent",  null);
      //connection.setRequestProperty("Pragma",  null);
      connection.setRequestProperty("Accept",  "*/*");
      
      connection.setRequestMethod(requestMethod);
      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
      Map<String, String> encodedHeaders = this.encodeHeader(uri);
      for(Map.Entry<String, String> entry : encodedHeaders.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        connection.setRequestProperty(key, value);
      }
      connection.setRequestProperty("X-RequestDate","2017-02-08 15:37:44");//BBB
      //connection.setRequestProperty("X-Nonce",null); //"ofkehqyrvnblp3s");//BBB
      connection.setUseCaches(false);
      connection.setDoOutput(true);

      // Send request
      DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
      
      wr.writeBytes(urlParameters);
      wr.close();

      // Get Response
      InputStream is = connection.getInputStream();
      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      StringBuilder response = new StringBuilder(); // or StringBuffer if Java
                                                    // version 5+
      String line;
      while ((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\r');
      }
      rd.close();
      toReturn.put("response",response.toString());
      return toReturn;
    } catch (Exception e) {
        toReturn.put("errorMessage",e.getMessage());
        toReturn.put("errorString",e.toString());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        toReturn.put("stackTrace", sw.toString());
        return toReturn;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    /*
     * $ch = curl_init();
     * 
     * 
     * curl_setopt($ch, CURLOPT_HEADER, 0); curl_setopt($ch, CURLOPT_HTTPHEADER,
     * $this->encodeHeader($uri)); curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
     * curl_setopt($ch, CURLINFO_HEADER_OUT, 1);
     * 
     * $response = curl_exec($ch); $info = curl_getinfo($ch);
     * 
     * if ($info['http_code'] == 200) { $this->valid_request = TRUE; } else {
     * $this->valid_request = FALSE; }
     * 
     * curl_close($ch); //$GLOBALS['edan_hashes'][$hash] = $response;
     * 
     * return $response;
     */
  }

  /**
   * Generates a nonce.
   *
   * @param int
   *          $length (optional) Int representing the length of the random
   *          string.
   * @param string
   *          $prefix (optional) String containing a prefix to be prepended to
   *          the random string.
   *
   * @return string Returns a string containing a randomized set of letters and
   *         numbers $length long with $prefix prepended.
   */
  private String get_nonce() {
    return get_nonce(15);
  }

  private String get_nonce(int length) {
    return get_nonce(length, "");
  }

  private String get_nonce(int length, String prefix) {
    String password = "";
    String possible = "0123456789abcdefghijklmnopqrstuvwxyz";

    int i = 0;

    while (i < length) {
      char singleChar = possible.charAt((int) Math.floor(Math.random() * possible.length()));

      if (password.indexOf(singleChar) == -1) {
        password += singleChar;
        i++;
      }
    }

    return prefix + password;
  }

  private static String base64encode(String myText) {
    byte[] b = myText.getBytes(Charset.forName("UTF-8"));
    String returnEncode = Base64Utility.encode(b);
    return returnEncode;
  }

  @SuppressWarnings("deprecation")
public static void main(String[] args) {
    System.out.println("Hello World");
    EdanApi ea = new EdanApi("", "", "");
    String sampleJson = ""
    		+"\n{"
    		+"\n \"project_id\": \"p125\","
    		+"\n \"project_name\": \"Sample Triangle Camera Trap Survey Project\","
    		+"\n \"sub_project_id\": \"sp818\","
    		+"\n \"sub_project_name\": \"Triangle Wild\","
    		+"\n \"deployment_id\": \"d18981\","
    		+"\n \"deployment_name\": \"RaleighBYO 416\","
    		+"\n \"image_sequence_id\": \"d18981s2\","
    		+"\n \"image\": {"
    		+"\n   \"id\": \"emammal_ct:1696705\","
    		+"\n   \"online_media\": {"
    		+"\n     \"mediaCount\": 1,"
    		+"\n     \"media\": ["
    		+"\n       {"
    		+"\n         \"content\": \"emammal_ct:1696705\","
    		+"\n         \"idsId\": \"emammal_ct:1696705\","
    		+"\n         \"type\": \"Images\","
    		+"\n         \"caption\": \"Camera Trap Image White-tailed Deer\","
    		+"\n         \"content\": \"emammal_ct:1696705\","
    		+"\n       }"
    		+"\n     ]"
    		+"\n   },"
    		+"\n   \"date_time\": \"2016-02-24 18:45:30\","
    		+"\n   \"photo_type\": \"animal\","
    		+"\n   \"photo_type_identified_by\": \"\","
    		+"\n   \"interest_ranking\": \"None\""
    		+"\n"
    		+"\n },"
    		+"\n \"image_identifications\": ["
    		+"\n  {"
    		+"\n    \"iucn_id\": \"42394\","
    		+"\n    \"species_scientific_name\": \"Odocoileus virginianus\","
    		+"\n    \"individual_animal_notes\": \"\","
    		+"\n    \"species_common_name\": \"White-tailed Deer\","
    		+"\n    \"count\": 1,"
    		+"\n    \"age\": \"\","
    		+"\n    \"sex\": \"\","
    		+"\n    \"individual_id\": \"\","
    		+"\n    \"animal_recognizable\": \"\","
    		+"\n    \"individual_animal_notes\": \"\","
    		+"\n  }"
    		+"\n ]";
    sampleJson = "{\"type\":\"gg\",\"content\":{\"test\":\"ok edit\"},\"url\":\"RB Test Model 10\",\"status\":1,\"title\":\"RB Test Model 10\",\"publicSearch\":false}";
    String edanUri = "type=emammal_image&content=" + URLEncoder.encode(sampleJson) + "&title=fakeItem&status=0&publicSearch=0";
    
    		
    Map<String, String> ma = ea.sendRequest(edanUri, "content/v1.1/admincontent/createContent.htm");
    for(Map.Entry<String, String> entry : ma.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        System.out.println("K:"+key+"\tval:"+value);
        // do what you have to do here
        // In your case, an other loop.
    }    
  }
}

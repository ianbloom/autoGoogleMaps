import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import groovy.json.JsonSlurper;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;

//define credentials and url
def accessId = 'dSpe6j9eTQXs3Iph7jCU'
def accessKey = 'dcm!p2d2w79V=5f}+[354xL=g{k442Y6h5qV}C_6'
def account = 'ianbloom'

def requestVerb = 'POST';
def resourcePath = '/dashboard/dashboards';
def queryParameters = '';
def url = 'https://' + account + '.logicmonitor.com' + '/santaba/rest' + resourcePath + queryParameters;
def data = '{"name":"USA","description":"","groupId":1,"sharable":true}'; // Single quoted as contents include double quotes. Only used when updating data, but being blank elsewise shouldn't hurt. In for template purposes.

responseDict =  LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

responseBody = responseDict.body;
output = new JsonSlurper().parseText(responseBody);

rootDashboardId = null;
if(output.data == null) {
	print("STATUS IS: " + output.status);
	// IF STATUS IS 600 THEN GET DASHBOARD ID
}
else {
	rootDashboardId = output.data.id;
	print("DASHBOARD ID IS: " + rootDashboardId);
	// IF THERE IS DATA (FIRST RUN) THEN CAPTURE ID OF ROOT DASH
	// THIS ID WILL BE USED TO FORM URL TO RETURN TO ROOT DASHBOARD WINDOW
}





def LMGET(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {
	// DATA SHOULD BE EMPTY
	// Initialize dictionary to hold response code and response body
	responseDict = [:];

	// Construcst URL to POST to from specified input
	url = 'https://' + _account + '.logicmonitor.com' + '/santaba/rest' + _resourcePath + _queryParameters;

	// Get current time
	epoch = System.currentTimeMillis();

	// Calculate signature
	requestVars = _requestVerb + epoch + _data + _resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(_accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	http_request = new HttpGet(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}

def LMPOST(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {

	// Initialize dictionary to hold response code and response body
	responseDict = [:];

	// Construcst URL to POST to from specified input
	url = 'https://' + _account + '.logicmonitor.com' + '/santaba/rest' + _resourcePath + _queryParameters;

	StringEntity params = new StringEntity(_data,ContentType.APPLICATION_JSON);

	// Get current time
	epoch = System.currentTimeMillis();

	// Calculate signature
	requestVars = _requestVerb + epoch + _data + _resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(_accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	http_request = new HttpPost(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	http_request.setHeader("Accept", "application/json");
	http_request.setHeader("Content-type", "application/json");
	http_request.setEntity(params);
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}

return 0;
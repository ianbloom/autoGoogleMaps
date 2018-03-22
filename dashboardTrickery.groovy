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
def rootGroupName = 'USA'
def rootGroup = '39'

// First we create a dashboard with the name of the root group and capture its dashboard ID
//
// If this dashboard already exists, we execute a GET request to obtain the dashboard name

def requestVerb = 'POST';
def resourcePath = '/dashboard/dashboards';
def queryParameters = '';
def data = '{"name":"' + rootGroupName + '","description":"","groupId":1,"sharable":true}';

// Attempt to POST a dashboard with the name of the root group
responseDict =  LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

responseBody = responseDict.body;
output = new JsonSlurper().parseText(responseBody);

// Initialize variable to hold dashboard ID of dashboard with the name of the root group
rootDashboardId = null;
if(output.data == null) {
	// IF OUTPUT DATA IS EMPTY, THEN A DASHBOARD OF THIS NAME EXISTS, GET ITS ID

	requestVerb = 'GET';
	resourcePath = '/dashboard/dashboards';
	queryParameters = '?filter=name~USA';
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);
	rootDashboardId = responseJSON.data.items[0].id;
}
else {
	rootDashboardId = output.data.id;
	// IF THERE IS DATA (FIRST RUN) THEN CAPTURE ID OF ROOT DASH
	// THIS ID WILL BE USED TO FORM URL TO RETURN TO ROOT DASHBOARD WINDOW
}

/////////////////
// TEXT WIDGET //
/////////////////

// First see if root dash has an existing text widget with the name rootGroupName_menu
requestVerb = 'GET';
resourcePath = '/dashboard/dashboards/' + rootDashboardId + '/widgets';
queryParameters = '?filter=name~' + rootGroupName + '_menu';
data = ''

responseDict =  LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
responseBody = responseDict.body;
responseJSON = new JsonSlurper().parseText(responseBody);
textWidgetId = null;

// If root dash does not have a text widget, post one
if(responseJSON.data.total == 0) {
	requestVerb = 'POST';
	resourcePath = '/dashboard/widgets';
	queryParameters = '';
	html = 'Hello World!';
	data = '{"name":"' + rootGroupName + '_menu","type":"text","dashboardId":"' + rootDashboardId + '","content":"' + html + '"}';

	responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);

	// Capture textWidgetId
	textWidgetId = responseJSON.data.id;

}
// If root dash DOES have a text widget, PUT to update
else {
	textWidgetId = responseJSON.data.items[0].id;
	println("WIDGET ID:  " + textWidgetId);

	requestVerb = 'PUT';
	resourcePath = '/dashboard/widgets/' + textWidgetId;
	queryParameters = '';
	html = 'I PUT THIS HERE';
	data = '{"name":"' + rootGroupName + '_menu","type":"text","dashboardId":"' + rootDashboardId + '","content":"' + html + '"}';

	responseDict = LMPUT(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
}

////////////////
// MAP WIDGET //
////////////////

// First see if root dash has an existing text widget with the name rootGroupName_menu
requestVerb = 'GET';
resourcePath = '/dashboard/dashboards/' + rootDashboardId + '/widgets';
queryParameters = '?filter=name~' + rootGroupName + '_map';
data = ''

responseDict =  LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
responseBody = responseDict.body;
responseJSON = new JsonSlurper().parseText(responseBody);
textWidgetId = null;

// If root dash does not have a gmap widget, post one
if(responseJSON.data.total == 0) {
	requestVerb = 'POST';
	resourcePath = '/dashboard/widgets';
	queryParameters = '';

	mapPoints = '[{"type":"group","deviceGroupFullPath":"' + rootGroupName + '"}]'
	data = '{"name":"' + rootGroupName + '_map","type":"gmap","dashboardId":"' + rootDashboardId + '","mapPoints":' + mapPoints + '}';

	responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);

	// Capture textWidgetId
	mapWidgetId = responseJSON.data.id;
	println("WIDGET ID:  " + mapWidgetId);

	println("body . " + responseDict.body);
	println("code . " + responseDict.code);

}
// If root dash DOES have a gmap widget, PUT to update
else {
	mapWidgetId = responseJSON.data.items[0].id;
	println("WIDGET ID:  " + textWidgetId);

	requestVerb = 'PUT';
	resourcePath = '/dashboard/widgets/' + textWidgetId;
	queryParameters = '';

	mapPoints = '[{"type":"group","deviceGroupFullPath":"' + rootGroupName + '"}]'
	data = '{"name":"' + rootGroupName + '_map","type":"gmap","dashboardId":"' + rootDashboardId + '","mapPoints":' + mapPoints + '}';

	responseDict = LMPUT(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	println("WIDGET ID:  " + mapWidgetId);

	println("body . " + responseDict.body);
	println("code . " + responseDict.code);
}









/////////////////////////////////////
// Santa's Little Helper Functions //
/////////////////////////////////////

def LMPUT(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {

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
	http_request = new HttpPut(url);
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
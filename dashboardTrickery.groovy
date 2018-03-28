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

def accessId = hostProps.get("lmaccess.id");
def accessKey = hostProps.get("lmaccess.key");
def account = hostProps.get("lmaccount");
def rootGroupName = hostProps.get("rootgroupname");
def rootGroup = hostProps.get("rootgroupid");
def dashGroup = hostProps.get("dashgroupname");

// Attempt to create a dashboard group to hold all dashboards.  If it exists, capture ID, if not, create and capture ID.

requestVerb = 'POST';
resourcePath = '/dashboard/groups';
queryParameters = '';
data = '{"name":"' + dashGroup + '"}';

// First we attempt to POST a dashboard group with the name dashGroup
responseDict =  LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

responseBody = responseDict.body;
output = new JsonSlurper().parseText(responseBody);

// Initialize dashGroupId variable
dashGroupId = null;
// If this dashboard group already exists, then GET its ID
if(output.data == null) {
	requestVerb = 'GET';
	resourcePath = '/dashboard/groups';
	queryParameters = '?filter=name~' + dashGroup;
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);
	dashGroupId = responseJSON.data.items[0].id;
}
// If this dashboard group does not already exist, lets capture this ID
else {
	dashGroupId = output.data.id;
}


// First we create a dashboard with the name of the root group and capture its dashboard ID
//
// If this dashboard already exists, we execute a GET request to obtain the dashboard name

requestVerb = 'POST';
resourcePath = '/dashboard/dashboards';
queryParameters = '';
data = '{"name":"' + rootGroupName + '","description":"","groupId":' + dashGroupId + ',"sharable":true}';

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
	queryParameters = '?filter=name~' + rootGroupName + '&filter=groupId~' + dashGroupId;
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

/////////////////////
// SUBGROUP GETTER //
/////////////////////

// Get all subgroups with custom property location that are children of the group with ID rootGroup (39 in this case)
requestVerb = 'GET';
resourcePath = '/device/groups/';
queryParameters = '?filter=parentId~' + rootGroup + '&fields=name,id';
data = ''

responseDict =  LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
responseBody = responseDict.body;
responseJSON = new JsonSlurper().parseText(responseBody);

// subGroupArray holds a JSON object for each subgroup with properties name, id, and dashID to add
subGroupArray = responseJSON.data.items;

///////////////////////////////
// SUBGROUP DASHBOARD POSTER //
///////////////////////////////

subGroupArray.each { item ->
	dashName = item.name;
	requestVerb = 'POST';
	resourcePath = '/dashboard/dashboards';
	queryParameters = '';
	data = '{"name":"' + dashName + '","description":"","groupId":' + dashGroupId + ',"sharable":true}';

	// Attempt to POST a dashboard with the name of the root group
	responseDict =  LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

	responseBody = responseDict.body;
	output = new JsonSlurper().parseText(responseBody);

	// Initialize variable to hold dashboard ID of dashboard with the name of the root group
	dashId = null;
	if(output.data == null) {
		// IF OUTPUT DATA IS EMPTY, THEN A DASHBOARD OF THIS NAME EXISTS, GET ITS ID

		requestVerb = 'GET';
		resourcePath = '/dashboard/dashboards';
		queryParameters = '?filter=name~' + dashName + '&filter=groupId~' + dashGroupId;
		data = '';

		responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
		responseBody = responseDict.body;
		responseJSON = new JsonSlurper().parseText(responseBody);
		dashId = responseJSON.data.items[0].id;
		// APPEND THE DASHID PROPERTY TO THE JSON MAPS IN SUBGROUPARRAY
		item["dashId"] = dashId;
	}
	else {
		dashId = output.data.id;
		// APPEND THE DASHID PROPERTY TO THE JSON MAPS IN SUBGROUPARRAY
		item["dashId"] = dashId;
		// IF THERE IS DATA (FIRST RUN) THEN CAPTURE ID OF ROOT DASH
		// THIS ID WILL BE USED TO FORM URL TO RETURN TO ROOT DASHBOARD WINDOW
	}
}

///////////////////////////
// SUBGROUP TEXT WIDGETS //
///////////////////////////

subGroupArray.each { item ->
	// First see if root dash has an existing text widget with the name rootGroupName_menu
	requestVerb = 'GET';
	resourcePath = '/dashboard/dashboards/' + item.dashId + '/widgets';
	queryParameters = '?filter=name~' + item.name + '_menu';
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
		//html = 'Hello World!';
		html = "<a href='https://ianbloom.logicmonitor.com/santaba/uiv3/dashboard/index.jsp#dashboard=" + rootDashboardId + "' target='_top'>" + rootGroupName + "</a><br />"
		data = '{"name":"' + item.name + '_menu","type":"text","dashboardId":"' + item.dashId + '","content":"' + html + '","rowSpan":2}';

		responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

		responseBody = responseDict.body;
		responseJSON = new JsonSlurper().parseText(responseBody);
		println('body . ' + responseDict.body);

		// Capture textWidgetId
		textWidgetId = responseJSON.data.id;

	}
	// If root dash DOES have a text widget, PUT to update
	else {
		textWidgetId = responseJSON.data.items[0].id;

		requestVerb = 'PUT';
		resourcePath = '/dashboard/widgets/' + textWidgetId;
		queryParameters = '';
		//html = 'I PUT THIS HERE';
		html = "<a href='https://ianbloom.logicmonitor.com/santaba/uiv3/dashboard/index.jsp#dashboard=" + rootDashboardId + "' target='_top'>" + rootGroupName + "</a><br />"
		data = '{"name":"' + item.name + '_menu","type":"text","dashboardId":"' + item.dashId + '","content":"' + html + '","rowSpan":2}';

		responseDict = LMPUT(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	}
}


//////////////////////////
// SUBGROUP MAP WIDGETS //
//////////////////////////

subGroupArray.each { item ->
	// First see if root dash has an existing text widget with the name rootGroupName_menu
	requestVerb = 'GET';
	resourcePath = '/dashboard/dashboards/' + item.dashId + '/widgets';
	queryParameters = '?filter=name~' + item.name + '_map';
	data = ''

	responseDict =  LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);
	mapWidgetId = null;

	// If root dash does not have a gmap widget, post one
	if(responseJSON.data.total == 0) {
		requestVerb = 'POST';
		resourcePath = '/dashboard/widgets';
		queryParameters = '';

		mapPoints = '[{"type":"device","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '","deviceDisplayName":"*"}]';
		data = '{"name":"' + item.name + '_map","type":"gmap","dashboardId":"' + item.dashId + '","mapPoints":' + mapPoints + ',"rowSpan":2,"colSpan":2}';

		responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

		responseBody = responseDict.body;
		responseJSON = new JsonSlurper().parseText(responseBody);

		// Capture textWidgetId
		mapWidgetId = responseJSON.data.id;
	}
	// If root dash DOES have a gmap widget, PUT to update
	else {
		mapWidgetId = responseJSON.data.items[0].id;

		requestVerb = 'PUT';
		resourcePath = '/dashboard/widgets/' + mapWidgetId;
		queryParameters = '';

		//mapPoints = '[{"type":"group","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '"}]'
		mapPoints = '[{"type":"device","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '","deviceDisplayName":"*"}]';
		data = '{"name":"' + item.name + '_map","type":"gmap","dashboardId":"' + item.dashId + '","mapPoints":' + mapPoints + ',"rowSpan":2,"colSpan":2}';

		responseDict = LMPUT(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	}
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
	//html = 'Hello World!';
	html = "";
	subGroupArray.each { item ->
		html += "<a href='https://ianbloom.logicmonitor.com/santaba/uiv3/dashboard/index.jsp#dashboard=" + item.dashId + "' target='_top'>" + item.name + "</a><br />";
	}
	data = '{"name":"' + rootGroupName + '_menu","type":"text","dashboardId":"' + rootDashboardId + '","content":"' + html + '","rowSpan":2}';

	responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);

	// Capture textWidgetId
	textWidgetId = responseJSON.data.id;

}
// If root dash DOES have a text widget, PUT to update
else {
	textWidgetId = responseJSON.data.items[0].id;

	requestVerb = 'PUT';
	resourcePath = '/dashboard/widgets/' + textWidgetId;
	queryParameters = '';
	//html = 'I PUT THIS HERE';
	html = "";
	subGroupArray.each { item ->
		html += "<a href='https://ianbloom.logicmonitor.com/santaba/uiv3/dashboard/index.jsp#dashboard=" + item.dashId + "' target='_top'>" + item.name + "</a><br />";
	}
	data = '{"name":"' + rootGroupName + '_menu","type":"text","dashboardId":"' + rootDashboardId + '","content":"' + html + '","rowSpan":2}';

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
mapWidgetId = null;

// If root dash does not have a gmap widget, post one
if(responseJSON.data.total == 0) {
	requestVerb = 'POST';
	resourcePath = '/dashboard/widgets';
	queryParameters = '';

	//mapPoints = '[{"type":"group","deviceGroupFullPath":"' + rootGroupName + '"}]'
	mapPoints = '[';
	subGroupArray.each { item ->
		if(item == subGroupArray.last()) {
			mapPoints += '{"type":"group","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '"}'
		}
		else {
			mapPoints += '{"type":"group","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '"},';
		}
	}
	mapPoints += ']';
	data = '{"name":"' + rootGroupName + '_map","type":"gmap","dashboardId":"' + rootDashboardId + '","mapPoints":' + mapPoints + ',"rowSpan":2,"colSpan":2}';

	responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);

	responseBody = responseDict.body;
	responseJSON = new JsonSlurper().parseText(responseBody);

	// Capture textWidgetId
	mapWidgetId = responseJSON.data.id;
}
// If root dash DOES have a gmap widget, PUT to update
else {
	mapWidgetId = responseJSON.data.items[0].id;

	requestVerb = 'PUT';
	resourcePath = '/dashboard/widgets/' + mapWidgetId;
	queryParameters = '';

	//mapPoints = '[{"type":"group","deviceGroupFullPath":"' + rootGroupName + '"}]'
	mapPoints = '[';
	subGroupArray.each { item ->
		if(item == subGroupArray.last()) {
			mapPoints += '{"type":"group","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '"}'
		}
		else {
			mapPoints += '{"type":"group","deviceGroupFullPath":"' + rootGroupName + '/' + item.name + '"},';
		}
	}
	mapPoints += ']';
	data = '{"name":"' + rootGroupName + '_map","type":"gmap","dashboardId":"' + rootDashboardId + '","mapPoints":' + mapPoints + ',"rowSpan":2,"colSpan":2}';

	responseDict = LMPUT(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
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
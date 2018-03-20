import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import groovy.json.JsonSlurper;

//define credentials and url
def accessId = 'dSpe6j9eTQXs3Iph7jCU'
def accessKey = 'dcm!p2d2w79V=5f}+[354xL=g{k442Y6h5qV}C_6'
def account = 'ianbloom'

def resourcePath = '/device/groups'
def queryParameters = '?fields=name,id,customProperties&filter=parentId~39'
def url = 'https://' + account + '.logicmonitor.com' + '/santaba/rest' + resourcePath + queryParameters;
def data = '' // Single quoted as contents include double quotes. Only used when updating data, but being blank elsewise shouldn't hurt. In for template purposes.

//get current time
epoch = System.currentTimeMillis();

//calculate signature
requestVars = 'GET' + epoch + data + resourcePath;

hmac = Mac.getInstance('HmacSHA256');
secret = new SecretKeySpec(accessKey.getBytes(), 'HmacSHA256');
hmac.init(secret);
hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
signature = hmac_signed.bytes.encodeBase64();

// HTTP Get
CloseableHttpClient httpclient = HttpClients.createDefault();
httpGet = new HttpGet(url);
httpGet.addHeader('Authorization' , 'LMv1 ' + accessId + ':' + signature + ':' + epoch);
response = httpclient.execute(httpGet);
responseBody = EntityUtils.toString(response.getEntity());
code = response.getStatusLine().getStatusCode();

holderDictArray = []

// If HTTP GET for subgroups of root is successful, obtain name, ID, and location custom property
if (code == 200) {
	
	rootGroups = new JsonSlurper().parseText(responseBody);
	groupArray = rootGroups.data.items;
	groupArrayLength = groupArray.size();
	for(i=0; i<groupArrayLength; i++) {
		// For each device group, obtain name, id, and all customProperties
		groupName = groupArray[i].name;
		groupId = groupArray[i].id;
		customProperties = groupArray[i].customProperties;

		// Filter based on the presence of customProperties
		if(customProperties != []) {
			customProperties.each { property ->
				// If a device group contains the 'location' property, continue
				if(property.name == "location") {
					// Initialize holderDict which will be a lookup for POST of Dashboard and Widget
					holderDict = [:];
					holderDict['groupName']=groupName;
					holderDict['groupId']=groupId;
					holderDict['location']=property.value;
					// Append current holderDict for individual device group to holderDictArray
					holderDictArray.add(holderDict);
				}
			}
		}
	}
}

holderDictArray.each { item ->
	print(item['groupName']);
	print("\n")
	print(item['groupId']);
	print("\n")
	print(item['location']);
	print("\n")
	print("\n")
}

// LETS MAKE A DASHBOARD FOR THE ROOT FOLDER, THEN DASHBOARDS FOR THE STATES, THEN LINK THEM UP

holderDictArray.each { item, index ->

	resourcePath = '/dashboard/dashboards'
	queryParameters = '?name=' + item['groupName']
	url = 'https://' + account + '.logicmonitor.com' + '/santaba/rest' + resourcePath + queryParameters;
	data = '' // Single quoted as contents include double quotes. Only used when updating data, but being blank elsewise shouldn't hurt. In for template purposes.

	//get current time
	epoch = System.currentTimeMillis();

	//calculate signature
	requestVars = 'POST' + epoch + data + resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	httpGet = new HttpGet(url);
	httpGet.addHeader('Authorization' , 'LMv1 ' + accessId + ':' + signature + ':' + epoch);
	response = httpclient.execute(httpGet);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();
}


return 0;
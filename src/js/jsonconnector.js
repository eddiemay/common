com.digitald4.common.JSONConnector = function($http, $httpParamSerializer) {
	this.baseUrl = 'json/';
	this.$http = $http;
	this.$httpParamSerializer = $httpParamSerializer;
};

com.digitald4.common.JSONConnector.prototype.baseUrl;
com.digitald4.common.JSONConnector.prototype.$http;
com.digitald4.common.JSONConnector.prototype.$httpParamSerializer;
	
com.digitald4.common.JSONConnector.prototype.performRequest =
		function(url, params, successCallback, errorCallback) {
	url = this.baseUrl + url;
	var serializedParams = this.$httpParamSerializer({json: params});
  if (serializedParams.length > 0) {
  	url += ((url.indexOf('?') === -1) ? '?' : '&') + serializedParams;
  }
	// Send
	console.log('performing action: ' + url);
	this.$http({
		method: 'GET',
		url: url,
		headers: {
			'Content-Type': 'json'
		}
	}).then(function(response) {
			if (response.data.valid) {
				successCallback(response.data.data);
			} else {
				console.log('error: ' + response.data.error);
				console.log('StackTrace:' + response.data.stackTrace);
				console.log('Request params' + response.data.requestParams);
				console.log('Query String: ' + response.data.queryString);
				errorCallback(response.data.error);
			}
		},
		function(response) {
		  console.log('Status code: ' + response.status);
		  if (response.status == 401) {
		    document.location.href = 'login.html';
		  }
			errorCallback(response);
		});
};

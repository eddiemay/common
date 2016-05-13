com.digitald4.common.JQueryConnector = function() {
	this.url = 'json/';
	this.dataType = 'json';
	this.requestType = 'GET';
};

com.digitald4.common.JQueryConnector.prototype.url;
com.digitald4.common.JQueryConnector.prototype.dataType;
com.digitald4.common.JQueryConnector.prototype.requestType;
	
com.digitald4.common.JQueryConnector.prototype.performRequest =
		function(url, params, successCallback, errorCallback) {
	// Send
	logRequest(this.url + url, params);
	$.ajax({
		url: this.url + url,
		dataType: this.dataType,
		type: this.requestType,
		data: params,
		success: function(response, textStatus, XMLHttpRequest) {
			if (response.valid) {
				successCallback(response.data);
			} else {
				console.log('error: ' + response.error);
				console.log('StackTrace:' + response.stackTrace);
				errorCallback(response.error);
			}
		},
		error: function(XMLHttpRequest, textStatus, errorThrown) {
			errorCallback(errorThrown);
		}
	});
};

logRequest = function(url, params) {
	var text = 'url{';
	for (var param in params) {
		text += param + ': ' + params[param] + ', ';
	}
  console.log('performing action: ' + text + '}');
};

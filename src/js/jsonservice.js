com.digitald4.common.JSONService = function(proto, restService) {
	this.restService = restService;
	this.service = proto + 's';
};

com.digitald4.common.JSONService.prototype.restService;
com.digitald4.common.JSONService.prototype.service;

com.digitald4.common.JSONService.prototype.performRequest = function(method, url, request, success, error) {
  this.restService.performRequest(method, url, request, success, error);
};

com.digitald4.common.JSONService.prototype.get = function(id, success, error) {
	this.performRequest('GET', this.service + '/' + id, undefined, success, error);
};

com.digitald4.common.JSONService.prototype.list = function(filter, success, error) {
	this.performRequest('GET', this.service, filter, success, error);
};

com.digitald4.common.JSONService.prototype.create = function(newJSON, success, error) {
  newJSON.$$hashKey = undefined;
	this.performRequest('POST', this.service, {proto: newJSON}, success, error);
};

com.digitald4.common.JSONService.prototype.update = function(proto, props, success, error) {
	var request = {id: proto.id, update: []};
	for (var p = 0; p < props.length; p++) {
	  var value = proto[props[p]];
	  value = typeof(value) == 'object' ? JSON.stringify(value) : value.toString();
	  request.update.push({property: props[p], value: value});
	}
	this.performRequest('POST', this.service + '/' + proto.id, request, success, error);
};

com.digitald4.common.JSONService.prototype.Delete = function(id, success, error) {
	this.performRequest('DELETE', this.service + '/' + id, undefined, success, error);
};

com.digitald4.common.ApiConnector = function($http, $httpParamSerializer, sessionWatcher) {
	this.baseUrl = 'api/';
	this.$http = $http;
	this.$httpParamSerializer = $httpParamSerializer;
	this.sessionWatcher = sessionWatcher;
};

com.digitald4.common.ApiConnector.prototype.baseUrl;
com.digitald4.common.ApiConnector.prototype.$http;
com.digitald4.common.ApiConnector.prototype.$httpParamSerializer;

com.digitald4.common.ApiConnector.prototype.performRequest =
		function(method, url, params, successCallback, errorCallback) {
  this.sessionWatcher.extendTime();
	url = this.baseUrl + url;
	var data = undefined;
	if (method == 'GET') {
    var serializedParams = this.$httpParamSerializer(params);
    if (params != undefined && serializedParams.length > 0) {
      url += ((url.indexOf('?') === -1) ? '?' : '&') + serializedParams;
    }
  } else {
    data = this.$httpParamSerializer({json: JSON.stringify(params)});
  }
	// Send
	this.$http({
		method: method,
		url: url,
		headers: {
		  'Content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
		},
		data: data
	}).then(function(response) {
			if (!response.data.error) {
				successCallback(response.data);
			} else {
				console.log('error: ' + response.data.error);
				console.log('StackTrace: ' + response.data.stackTrace);
				console.log('Request params: ' + response.data.requestParams);
				console.log('Query String: ' + response.data.queryString);
				errorCallback(response.data.error);
			}
		},
		function(response) {
		  console.log('Status code: ' + response.status);
		  if (response.status == 401) {
		    document.location.href = 'login.html';
		  }
			errorCallback(response.data.error);
		});
};

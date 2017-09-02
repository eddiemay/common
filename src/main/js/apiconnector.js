com.digitald4.common.ApiConnector = ['$http', '$httpParamSerializer', 'globalData',
    function($http, $httpParamSerializer, globalData) {
	this.baseUrl = 'api/';
	this.$http = $http;
	this.$httpParamSerializer = $httpParamSerializer;

	this.performRequest = function(method, url, params, successCallback, errorCallback) {
	  globalData.extendTime();
    url = this.baseUrl + url;
    var data = undefined;
    if (method == 'GET' || method == 'DELETE') {
      params = params || {};
      params.idToken = globalData.idToken;
      var serializedParams = this.$httpParamSerializer(params);
      if (params != undefined && serializedParams.length > 0) {
        url += ((url.indexOf('?') === -1) ? '?' : '&') + serializedParams;
      }
    } else {
      data = this.$httpParamSerializer({json: JSON.stringify(params), idToken: globalData.idToken});
    }
    // Send
    this.$http({
      method: method,
      url: url,
      data: data,
      headers: {
        'Content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
      }
    }).then(function(response) {
      successCallback(response.data);
    }, function(response) {
      console.log('Status code: ' + response.status);
      if (response.status == 401) {
        // document.location.href = 'login.html';
      } else {
        console.log('error: ' + response.data.error);
        console.log('StackTrace: ' + response.data.stackTrace);
        console.log('Request params: ' + response.data.requestParams);
        console.log('Query String: ' + response.data.queryString);
        errorCallback(response.data.error);
      }
    });
  };
}];

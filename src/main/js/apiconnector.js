com.digitald4.common.ApiConnector = ['$http', '$httpParamSerializer', 'globalData',
    function($http, $httpParamSerializer, globalData) {
  this.baseUrl = '_api/';
  this.$http = $http;
  this.$httpParamSerializer = $httpParamSerializer;

  this.performRequest = function(method, url, params, data, successCallback, errorCallback) {
    url = this.baseUrl + url;
    params = params || {};
    params.idToken = globalData.activeSession ? globalData.activeSession.idToken : undefined;
    var serializedParams = this.$httpParamSerializer(params);
    if (params != undefined && serializedParams.length > 0) {
      url += (url.indexOf('?') === -1 ? '?' : '&') + serializedParams;
    }

    // Send
    this.$http({
      method: method,
      url: url,
      data: data ? JSON.stringify(data) : undefined,
      headers: {'Content-type': 'application/json'}
    }).then(function(response) {
      successCallback(response.data);
    }, function(response) {
      console.log('Status code: ' + response.status);
      if (response.status == 401) {
        globalData.user = globalData.activeSession = undefined;
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

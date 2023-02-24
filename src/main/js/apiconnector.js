com.digitald4.common.ApiConnector = ['$http', '$httpParamSerializer', 'globalData',
    function($http, $httpParamSerializer, globalData) {
  this.baseUrl = '_api/';
  this.$http = $http;
  this.$httpParamSerializer = $httpParamSerializer;
  this.globalData = globalData;

  this.sendRequest = function(request, successCallback, errorCallback) {
    errorCallback = errorCallback || notifyError;
    var url = this.baseUrl + request.url;
    var params = request.params || {};
    params.idToken = globalData.activeSession ? globalData.activeSession.id : params.idToken;
    var serializedParams = this.$httpParamSerializer(params);
    if (params != undefined && serializedParams.length > 0) {
      url += (url.indexOf('?') === -1 ? '?' : '&') + serializedParams;
    }

    // Send
    this.$http({
      method: request.method || 'GET',
      url: url,
      data: request.data ? JSON.stringify(request.data) : undefined,
      headers: request.headers || {'Content-type': 'application/json'}
    }).then(function(response) {
      successCallback(response.data);
    }, function(response) {
      console.log('Status code: ' + response.status);
      if (response.status == 401) {
        globalData.activeSession = undefined;
      } else {
        console.log('message: ' + response.data.error.message);
        errorCallback(response.data.error);
      }
    });
  }
}];

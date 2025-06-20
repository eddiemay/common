com.digitald4.common.ApiConnector = ['$http', '$httpParamSerializer', 'globalData',
    function($http, $httpParamSerializer, globalData) {
  this.baseUrl = '';
  this.apiUrl = '_api/';
  this.globalData = globalData;

  this.sendRequest = function(request, successCallback, errorCallback) {
    errorCallback = errorCallback || notifyError;
    var url = request.request_url || this.baseUrl + this.apiUrl + request.url;
    var params = request.params || {};
    params.idToken = globalData.activeSession ? globalData.activeSession.id : params.idToken;
    var serializedParams = $httpParamSerializer(params);
    if (params != undefined && serializedParams.length > 0) {
      url += (url.indexOf('?') === -1 ? '?' : '&') + serializedParams;
    }

    // Send
    $http({
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
        errorCallback(response.data.error);
      } else if (response.data && response.data.error) {
        console.log('message: ' + response.data.error.message);
        errorCallback(response.data.error);
      } else {
        errorCallback('❌ Error Submitting Request');
      }
    });
  }
}];

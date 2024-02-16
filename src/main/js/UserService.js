var createUserService = function($cookies, apiConnector, globalData) {
  var userService = new com.digitald4.common.JSONService('user', apiConnector);
  userService.login = function(username, password, success, error) {
    var request = {username: username, password: CryptoJS.MD5(password).toString().toUpperCase()};
    this.sendRequest({action: 'login', method: 'POST', data: request}, function(activeSession) {
      globalData.activeSession = activeSession;
      $cookies.putObject('activeSession', activeSession);
      success(activeSession);
    }, error);
  }

  userService.setPassword = function(userId, password, success, error) {
    var request = {userId: userId, password: CryptoJS.MD5(password).toString().toUpperCase()};
    this.sendRequest({action: 'updatePassword', method: 'POST', data: request}, success, error);
  }

  userService.logout = function(success, error) {
    if (globalData.activeSession) {
      this.sendRequest({action: 'logout'}, success, error);
      globalData.activeSession = undefined;
      $cookies.remove('activeSession');
    }
  }

  userService.getActiveSession = function(idToken, success, error) {
    this.sendRequest(
        {action: 'activeSession', params: {idToken: idToken}}, success, error);
  }

  return userService;
}

com.digitald4.common.UserService = ['$cookies', 'apiConnector', 'globalData', createUserService];

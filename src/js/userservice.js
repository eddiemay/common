com.digitald4.common.UserService = function(apiConnector) {
  var userService = new com.digitald4.common.JSONService('user', apiConnector);

  userService.login = function(username, password) {
    this.performRequest(['login', 'POST'], undefined, {username: username, password: password}, function() {
      document.location.href = './';
    }, notify);
  };

  userService.logout = function() {
    this.performRequest(['logout'], undefined, undefined, function() {
      document.location.href = "login.html";
    }, notify);
  };

  userService.getActive = function(success, error) {
    this.performRequest(['active'], undefined, undefined, success, error);
  };
 return userService;
};
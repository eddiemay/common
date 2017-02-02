com.digitald4.common.UserService = function(restService) {
  var userService = new com.digitald4.common.ProtoService('user', restService);

  userService.login = function(username, password) {
    this.performRequest('login', {username: username, password: password}, function() {
      document.location.href = './';
    }, notify);
  };

  userService.logout = function() {
    this.performRequest('logout', {}, function() {
      document.location.href = "login.html";
    }, notify);
  };

  userService.getActive = function(success, error) {
    this.performRequest('active', {}, success, error);
  };
 return userService;
};
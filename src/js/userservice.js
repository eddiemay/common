com.digitald4.common.UserService = function(restService) {
  var userService = new com.digitald4.common.JSONService('user', restService);

  userService.login = function(username, password) {
    this.performRequest('POST', this.service + '/login', {username: username, password: password}, function() {
      document.location.href = './';
    }, notify);
  };

  userService.logout = function() {
    this.performRequest('GET', this.service + '/logout', undefined, function() {
      document.location.href = "login.html";
    }, notify);
  };

  userService.getActive = function(success, error) {
    this.performRequest('GET', this.service + '/active', undefined, success, error);
  };
 return userService;
};
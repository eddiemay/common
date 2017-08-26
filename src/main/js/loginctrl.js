com.digitald4.common.LoginController = function(userService, sessionWatcher) {
  this.userService = userService;
  sessionWatcher.disable();
};

com.digitald4.common.LoginCtrl = ['userService', 'sessionWatcher', com.digitald4.common.LoginController];

com.digitald4.common.LoginController.prototype.login = function() {
  this.userService.login(this.username, this.password);
};

com.digitald4.common.LoginController.prototype.recoverPassword = function() {
};

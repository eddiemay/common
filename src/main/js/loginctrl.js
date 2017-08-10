com.digitald4.common.LoginCtrl = function(userService, sessionWatcher) {
  this.userService = userService;
  sessionWatcher.disable();
};

com.digitald4.common.LoginCtrl.prototype.userService;

com.digitald4.common.LoginCtrl.prototype.login = function() {
  this.userService.login(this.username, this.password);
};

com.digitald4.common.LoginCtrl.prototype.recoverPassword = function() {
};

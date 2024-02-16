com.digitald4.common.LoginController = function($window, userService, sessionWatcher) {
  this.style = {top: ($window.visualViewport.pageTop + 20) + 'px'};
  this.userService = userService;
  this.sessionWatcher = sessionWatcher;
  sessionWatcher.disable();
}

com.digitald4.common.LoginCtrl = ['$window', 'userService', 'sessionWatcher', com.digitald4.common.LoginController];

com.digitald4.common.LoginController.prototype.login = function() {
  this.userService.login(this.username, this.password, function(activeSession) {
    this.sessionWatcher.enable();
	  if (this.onLoginSuccess) {
	    this.onLoginSuccess();
	  }
  }.bind(this));
}

com.digitald4.common.LoginController.prototype.showSignUpDialog = function() {
  this.signUpDialogShown = true;
}

com.digitald4.common.LoginController.prototype.closeSignUpDialog = function() {
  this.signUpDialogShown = false;
}

com.digitald4.common.LoginController.prototype.processSignUp = function() {
  if (this.retypePassword != this.password) {
    notify('Passwords do not match');
    return;
  }
  var user = {
    email: this.email,
    firstName: this.firstName,
    lastName: this.lastName,
    password: this.password,
    typeId: 4
  };
  this.userService.create(user, function(user) {});
}

com.digitald4.common.LoginController.prototype.toggleRecoveryShown = function() {
  this.recoveryShown = !this.recoveryShown;
}

com.digitald4.common.LoginController.prototype.recoverPassword = function() {
}



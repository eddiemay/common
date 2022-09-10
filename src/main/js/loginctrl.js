com.digitald4.common.LoginController = function($location, userService, sessionWatcher, globalData) {
  this.locationProvider = $location;
  this.userService = userService;
  this.globalData = globalData;
  this.sessionWatcher = sessionWatcher;
  sessionWatcher.disable();
}

com.digitald4.common.LoginCtrl = ['$location', 'userService', 'sessionWatcher', 'globalData', com.digitald4.common.LoginController];

com.digitald4.common.LoginController.prototype.login = function() {
  this.userService.login(this.email, this.password, function(activeSession) {
    this.globalData.activeSession = activeSession;
    this.locationProvider.search('idToken', activeSession.idToken);
	  this.sessionWatcher.enable();
	  if (this.onLoginSuccess) {
	    this.onLoginSuccess();
	  }
  }.bind(this), notify);
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
  this.userService.create(user, function(user) {}, notify);
}

com.digitald4.common.LoginController.prototype.toggleRecoveryShown = function() {
  this.recoveryShown = !this.recoveryShown;
}

com.digitald4.common.LoginController.prototype.recoverPassword = function() {
}



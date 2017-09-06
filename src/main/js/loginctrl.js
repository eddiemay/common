com.digitald4.common.LoginController = function(userService, sessionWatcher, globalData) {
  this.userService = userService;
  this.globalData = globalData;
  sessionWatcher.disable();
};

com.digitald4.common.LoginCtrl = ['userService', 'sessionWatcher', 'globalData', com.digitald4.common.LoginController];

com.digitald4.common.LoginController.prototype.login = function() {
  this.userService.login(this.email, this.password, function(user) {
    this.globalData.user = user;
  }.bind(this), notify);
};

com.digitald4.common.LoginController.prototype.showSignUpDialog = function() {
  this.signUpDialogShown = true;
};

com.digitald4.common.LoginController.prototype.closeSignUpDialog = function() {
  this.signUpDialogShown = false;
};

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
  this.userService.create(user, function(user) {
    this.globalData.user = user;
  }.bind(this), notify);
};

com.digitald4.common.LoginController.prototype.toggleRecoveryShown = function() {
  this.recoveryShown = !this.recoveryShown;
};

com.digitald4.common.LoginController.prototype.recoverPassword = function() {
};



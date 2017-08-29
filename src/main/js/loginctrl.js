com.digitald4.common.LoginController = function(userService, sessionWatcher) {
  this.userService = userService;
  sessionWatcher.disable();
};

com.digitald4.common.LoginCtrl = ['userService', 'sessionWatcher', com.digitald4.common.LoginController];

com.digitald4.common.LoginController.prototype.login = function() {
  this.userService.login(this.email, this.password);
};

com.digitald4.common.LoginController.prototype.recoverPassword = function() {
};

com.digitald4.common.LoginController.prototype.showSignUpDialog = function() {
  this.signUpDialogShown = true;
};

com.digitald4.common.LoginController.prototype.closeSignUpDialog = function() {
  this.signUpDialogShown = false;
};

com.digitald4.common.LoginController.prototype.processSignUp = function() {
  if (this.retypePassword != this.password) {
    notify('Passwords don\'t match');
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
    document.location.href = './';
  }, notify);
};



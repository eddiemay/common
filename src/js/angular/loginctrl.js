com.digitald4.common.LoginCtrl = function(restService) {
  this.restService = restService;
};

com.digitald4.common.LoginCtrl.prototype.restService;

com.digitald4.common.LoginCtrl.prototype.login = function() {
  this.restService.performRequest('login', {username: this.username, password: this.password},
      function() {
        document.location.href = './';
      }, notify);
};

com.digitald4.common.LoginCtrl.prototype.recoverPassword = function() {
};

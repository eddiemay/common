com.digitald4.common.UserController = function($routeParams, userService) {
	this.userId = parseInt($routeParams.id, 10);
	this.userService = userService;
  this.userTypes = [
      {id: 0, name: 'Unknown'},
      {id: 1, name: 'Standard'},
      {id: 2, name: 'Admin'}];
	this.refresh();
}

com.digitald4.common.UserCtrl = ['$routeParams', 'userService', com.digitald4.common.UserController];

com.digitald4.common.UserController.prototype.refresh = function() {
	this.userService.get(this.userId, function(user) {this.user = user}.bind(this));
}

com.digitald4.common.UserController.prototype.update = function(prop) {
  this.userService.update(this.user, [prop], function(user) {this.user = user}.bind(this));
}
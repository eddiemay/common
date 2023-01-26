com.digitald4.common.UserController = function($routeParams, userService, generalDataService) {
	this.userId = parseInt($routeParams.id, 10);
	this.userService = userService;
	this.generalDataService = generalDataService;
  this.userTypes = [
      {id: 0, name: 'Unknown'},
      {id: 1, name: 'Standard'},
      {id: 2, name: 'Admin'}];
	this.refresh();
}

com.digitald4.common.UserCtrl =
    ['$routeParams', 'userService', 'generalDataService', com.digitald4.common.UserController];

com.digitald4.common.UserController.prototype.refresh = function() {
	this.userService.get(this.userId, function(user) {
		this.user = user;
	}.bind(this), notifyError);
}

com.digitald4.common.UserController.prototype.update = function(prop) {
  this.userService.update(this.user, [prop], function(user) {
    this.user = user;
  }.bind(this), notifyError);
}
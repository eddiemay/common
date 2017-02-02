com.digitald4.common.UserCtrl = function($routeParams, restService, generalDataService) {
	this.userId = parseInt($routeParams.id, 10);
	this.userService = new com.digitald4.common.ProtoService('user', restService);
	this.generalDataService = generalDataService;
	this.refresh();
};

com.digitald4.common.UserCtrl.prototype.refresh = function() {
	this.userService.get(this.userId, function(user) {
		this.user = user;
	}.bind(this), notify);
};

com.digitald4.common.UserCtrl.prototype.update = function(prop) {
	this.userService.update(this.user, [prop], function(user) {
		this.user = user;
	}.bind(this), notify);
};
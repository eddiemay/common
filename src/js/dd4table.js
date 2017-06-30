com.digitald4.common.TableCtrl = function($scope, apiConnector) {
  this.scope = $scope;
	this.metadata = $scope.metadata;
	this.base = this.metadata.base;
	this.jsonService = new com.digitald4.common.JSONService(this.base.entity, apiConnector);
	this.metadata.refresh = this.refresh.bind(this);
	this.refresh();
};

com.digitald4.common.TableCtrl.prototype.refresh = function() {
  this.loading = this.scope.loading = true;
	this.jsonService.list(this.metadata.filter, function(entities) {
	  this.entities = entities;
	  this.loading = this.scope.loading = false;
	}.bind(this), notify);
};

com.digitald4.common.TableCtrl.prototype.update = function(entity, prop) {
  this.loading = this.scope.loading = true;
  var index = this.entities.indexOf(entity);
  this.jsonService.update(entity, [prop], function(entity) {
      this.entities.splice(index, 1, entity);
      this.loading = this.scope.loading = false;
    }.bind(this), notify);
};

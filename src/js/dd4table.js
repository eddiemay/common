com.digitald4.common.TableCtrl = function($scope, restService) {
	this.metadata = $scope.metadata;
	this.base = $scope.metadata.base;
	this.protoService = new com.digitald4.common.ProtoService(this.base.entity, restService);
	this.refresh();
};

com.digitald4.common.TableCtrl.prototype.refresh = function() {
  this.loading = true;
	this.protoService.list(this.metadata.request, function(entities) {
			this.entities = entities;
			this.loading = false;
		}.bind(this), notify);
};

com.digitald4.common.TableCtrl.prototype.update = function(entity, prop) {
  this.loading = true;
  var index = this.entities.indexOf(entity);
  this.protoService.update(entity, prop, function(entity_) {
      this.entities.splice(index, 1, entity_);
      this.loading = false;
    }.bind(this), notify);
};

com.digitald4.common.module.controller('TableCtrl', com.digitald4.common.TableCtrl);

com.digitald4.common.module.directive('dd4Table', function() {
  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    scope: {metadata: '=dd4Table'},
    controller: 'TableCtrl as tableCtrl',
    templateUrl: 'js/common/dd4table.html'
  };
});
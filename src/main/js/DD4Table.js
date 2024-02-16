com.digitald4.common.TableController = function($scope, apiConnector) {
  this.scope = $scope;
	var metadata = $scope.metadata;
	var base = metadata.base || {};
	this.title = metadata.title || base.title;
	this.columns = metadata.columns || base.columns;
	this.filter = metadata.filter || base.filter;
	this.controlFilters = metadata.controlFilters || base.controlFilters || [];
	this.orderBy = metadata.orderBy || base.orderBy;
	this.jsonService =
	    new com.digitald4.common.JSONService(metadata.entity || base.entity, apiConnector);
	metadata.refresh = this.refresh.bind(this);
	this.refresh();
}

com.digitald4.common.TableCtrl = ['$scope', 'apiConnector', com.digitald4.common.TableController];

com.digitald4.common.TableController.prototype.refresh = function() {
  this.loading = this.scope.loading = true;
  var request = {filter: this.filter, orderBy: this.orderBy};
  for (var f = 0; f < this.controlFilters.length; f++) {
    var controlFilter = this.controlFilters[f];
    if (controlFilter.selected && controlFilter.selected != 'All') {
      if (request.filter) {
        request.filter = request.filter + ',' + controlFilter.prop + '=' + controlFilter.selected;
      } else {
        request.filter = controlFilter.prop + '=' + controlFilter.selected;
      }
    }
  }
  this.jsonService.list(request, function(response) {
    this.entities = response.items;
    this.loading = this.scope.loading = false;
  }.bind(this));
}

com.digitald4.common.TableController.prototype.update = function(entity, prop) {
  this.loading = this.scope.loading = true;
  var index = this.entities.indexOf(entity);
  this.jsonService.update(entity, [prop], function(entity) {
    this.entities.splice(index, 1, entity);
    this.loading = this.scope.loading = false;
  }.bind(this));
}

com.digitald4.common.TableController.prototype.getDate = function(value) {
  if (value && value.millis) {
    return value.millis;
  } else if (value && value.epochSecond) {
    return value.epochSecond * 1000;
  }

  return value;
}

com.digitald4.common.TableController = function($scope, apiConnector) {
  var metadata = this.metadata || $scope.metadata;
  if (this.metadata) {
    console.log(this.metadata);
    this.pageSize = metadata.pageSize || "50";
  }
  this.pageToken = 1;
  var base = metadata.base || {};
  this.title = metadata.title || base.title;
  this.columns = metadata.columns || base.columns;
  this.filter = metadata.filter || base.filter;
  this.orderBy = metadata.orderBy || base.orderBy;
  this.jsonService = new com.digitald4.common.JSONService(metadata.entity || base.entity, apiConnector);
  metadata.refresh = this.refresh.bind(this);
  this.refresh();
}

com.digitald4.common.TableCtrl = ['$scope', 'apiConnector', com.digitald4.common.TableController];

com.digitald4.common.TableController.prototype.previous = function() {
  if (this.pageToken > 1) {
    this.refresh(this.pageToken - 1);
  }
}

com.digitald4.common.TableController.prototype.refresh = function(pageToken) {
  this.loading = true;
  this.pageToken = pageToken || 1;
  var request = {filter: this.filter, pageSize: this.pageSize, pageToken: this.pageToken, orderBy: this.orderBy};
  for (var c = 0; c < this.columns.length; c++) {
    var column = this.columns[c];
    if (column.filter && column.filter != '*All') {
      if (request.filter) {
        request.filter = request.filter + ',' + column.prop + '=' + column.filter;
      } else {
        request.filter = column.prop + '=' + column.filter;
      }
    }
  }

  this.jsonService.list(request, function(response) {
    this.resultList = response;
    var page = this.pageToken;
    this.start = Math.min((page - 1) * this.pageSize + 1, response.totalSize);
    this.totalSize = response.totalSize;
    this.end = Math.min(this.pageSize * page, response.totalSize);
    this.loading = false;
  }.bind(this));
}

com.digitald4.common.TableController.prototype.next = function() {
  if (this.resultList.pages.length > this.pageToken) {
    this.refresh(this.pageToken + 1);
  }
}

com.digitald4.common.TableController.prototype.setSort = function(col) {
  this.orderBy = (this.orderBy == col.prop) ? col.prop + " DESC" : col.prop;
  this.refresh();
}

com.digitald4.common.TableController.prototype.update = function(entity, prop) {
  this.loading = this.scope.loading = true;
  var index = this.resultList.items.indexOf(entity);
  this.jsonService.update(entity, [prop], function(entity) {
    this.resultList.items.splice(index, 1, entity);
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

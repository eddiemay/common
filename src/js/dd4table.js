com.digitald4.common.TableCtrl = function($scope, restService) {
  this.scope = $scope;
	this.metadata = $scope.metadata;
	this.base = this.metadata.base;
	this.protoService = new com.digitald4.common.ProtoService(this.base.entity, restService);
	this.refresh();
};

com.digitald4.common.TableCtrl.prototype.refresh = function() {
  this.scope.loading = true;
	this.protoService.list(this.metadata.request, function(entities) {
			this.entities = entities;
			this.scope.loading = false;
		}.bind(this), notify);
};

com.digitald4.common.TableCtrl.prototype.update = function(entity, prop) {
  this.scope.loading = true;
  var index = this.entities.indexOf(entity);
  this.protoService.update(entity, prop, function(entity_) {
      this.entities.splice(index, 1, entity_);
      this.scope.loading = false;
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
    templateUrl: 'js/common/dd4table.html',
    compile: function() {
      return {
        post: function(scope, element, attributes) {
          scope.$watch('loading', function(loading) {
            if (!loading) {
              var table = $(element.find('table'));
              //table.dataTable();
              oTable = table.dataTable({
                  //"bSort": false,

                  /*
                   * We set specific options for each columns here. Some columns contain raw data to enable correct sorting, so we convert it for display
                   * @url http://www.datatables.net/usage/columns
                   */
                  aoColumns: [
                    { bSortable: false},	// No sorting for this columns, as it only contains checkboxes
                    { sType: 'string' },
                    { sType: 'string' },
                    { sType: 'string' },
                    { sType: 'string' },
                    { sType: 'string' }
                  ],

                  "aoColumnDefs": [
                    { "bSortable": false, "aTargets": [ 0 ] }
                  ],

                  //"order": [[ 1, "asc" ]],


                  /*
                   * Set DOM structure for table controls
                   * @url http://www.datatables.net/examples/basic_init/dom.html
                   */
                  sDom: '<"block-controls"<"controls-buttons"p>>rti<"block-footer clearfix"l>',

                  /*
                   * Callback to apply template setup
                   */
                  fnDrawCallback: function() {
                    table.parent().applyTemplateSetup();
                  },
                  fnInitComplete: function() {
                    table.parent().applyTemplateSetup();
                  }
                });

              // Sorting arrows behaviour
              table.find('thead .sort-up').click(function(event) {
                // Stop link behaviour
                event.preventDefault();

                // Find column index
                var column = table.closest('th'),
                  columnIndex = column.parent().children().index(column.get(0));

                // Send command
                oTable.fnSort([[columnIndex, 'asc']]);

                // Prevent bubbling
                return false;
              });
              table.find('thead .sort-down').click(function(event) {
                // Stop link behaviour
                event.preventDefault();

                // Find column index
                var column = table.closest('th'),
                  columnIndex = column.parent().children().index(column.get(0));

                // Send command
                oTable.fnSort([[columnIndex, 'desc']]);

                // Prevent bubbling
                return false;
              });
            }
          });
        }
      }
    },
  };
});
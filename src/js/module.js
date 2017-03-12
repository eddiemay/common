com.digitald4.common.module = angular.module('DD4Common', [])
    .service('sessionWatcher', com.digitald4.common.SessionWatcher)
    .service('apiConnector', com.digitald4.common.ApiConnector)
    .service('userService', com.digitald4.common.UserService)
    .service('generalDataService', com.digitald4.common.GeneralDataService)
    .controller('LoginCtrl', com.digitald4.common.LoginCtrl)
    .controller('UserCtrl', com.digitald4.common.UserCtrl);

com.digitald4.common.module.directive('onChange', function() {
  return function(scope, element, attrs) {
  	var startingValue = element.val();
  	element.bind('blur', function() {
  		// console.log('evaluating: ' + startingValue + ' vs ' + element.val());
  		if (startingValue != element.val()) {
  			scope.$apply(attrs.onChange);
  			startingValue = element.val();
  		}
  	});
  }
});

com.digitald4.common.module.directive('mapauto', function() {
  return function(scope, element, attrs) {
    if (typeof(google) != 'undefined') {
      google.maps.event.addDomListener(window, 'load', addMapAutoComplete(element.get(0), function(place) {
        scope.place = place;
        scope.$apply(attrs.mapauto);
      }));
    }
  }
});

com.digitald4.common.module.directive('dd4Address', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '='
    },
    template: '<span><label data-ng-if="label">{{label}}</label>' +
        '<input type="text" value="{{ngModel.address}}" class="full-width"/></span>',
    replace: true,
    require: 'ngModel',
    link: function(scope, element, attrs) {
      var textField = $('input', element);
      $compile(textField)(scope.$parent);

      if (typeof(google) != 'undefined') {
        google.maps.event.addDomListener(window, 'load', addMapAutoComplete(textField[0], function(place) {
          if (typeof(gpsAdress) == 'undefined') {
            scope.$parent.$eval(attrs.ngModel + ' = {}');
            gpsAddress = scope.$parent.$eval(attrs.ngModel);
          }
          gpsAddress.address = place.formatted_address;
          gpsAddress.latitude = place.geometry.location.lat();
          gpsAddress.longitude = place.geometry.location.lng();
          scope.$parent.$apply(attrs.dd4Address);
        }));
      }
    }
  }
});

com.digitald4.common.module.directive('dd4Datepicker', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '='
    },
    template: '<span><label data-ng-if="label">{{label}}</label>' +
        '<input type="text" class="datepicker" value="{{ngModel | date:\'MM/dd/yyyy\'}}" size="10"/>' +
        '<img src="images/icons/fugue/calendar-month.png" width="16" height="16"/></span>',
    replace: true,
    require: 'ngModel',
    link: function(scope, element, attrs) {
      var textField = $('input', element);//.
          //attr('data-ng-model', 'date').
          //val(scope.$parent.$eval(attrs.ngModel));

      var update = function(date) {
        var currentValue = scope.$parent.$eval(attrs.ngModel);
        var newDate = new Date(currentValue);
        newDate.setFullYear(date.getFullYear());
        newDate.setMonth(date.getMonth());
        newDate.setDate(date.getDate());
        var newValue = newDate.getTime();
        /* if (!newValue && !currentValue) {
           return;
        } */
        if (currentValue != newValue) {
          console.log('Time changed from ' + currentValue + ' to ' + newValue);
          scope.$parent.$eval(attrs.ngModel + ' = ' + newValue);
          scope.$parent.$apply(attrs.dd4Datepicker);
        }
      };

      textField.bind('blur', function() {
        update(new Date(textField.val()));
      });
      $compile(textField)(scope.$parent);

      /*
       * Datepicker
       * Thanks to sbkyle! http://themeforest.net/user/sbkyle
       */
      $(textField).datepick({
        alignment: 'bottom',
        showOtherMonths: true,
        selectOtherMonths: true,
        renderer: {
          picker: '<div class="datepick block-border clearfix form"><div class="mini-calendar clearfix">' +
              '{months}</div></div>',
          monthRow: '{months}',
          month: '<div class="calendar-controls" style="white-space: nowrap">' +
              '{monthHeader:M yyyy}' +
              '</div>' +
              '<table cellspacing="0">' +
                '<thead>{weekHeader}</thead>' +
                '<tbody>{weeks}</tbody></table>',
          weekHeader: '<tr>{days}</tr>',
          dayHeader: '<th>{day}</th>',
          week: '<tr>{days}</tr>',
          day: '<td>{day}</td>',
          monthSelector: '.month',
          daySelector: 'td',
          rtlClass: 'rtl',
          multiClass: 'multi',
          defaultClass: 'default',
          selectedClass: 'selected',
          highlightedClass: 'highlight',
          todayClass: 'today',
          otherMonthClass: 'other-month',
          weekendClass: 'week-end',
          commandClass: 'calendar',
          commandLinkClass: 'button',
          disabledClass: 'unavailable'
        },
        onSelect: function(dateText, inst) {
          console.log('Selected: ' + dateText);
          update(new Date(dateText));
          $(this).change();
        }
      });
    }
  }
});



com.digitald4.common.module.directive('dd4Timepicker', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '='
    },
    template: '<div><input type="text" value="{{ngModel | date:\'shortTime\'}}" size="8"/></div>',
    replace: true,
    require: 'ngModel',
    link: function(scope, element, attrs) {
      var textField = $('input', element);

      textField.bind('blur', function() {
        var currentValue = scope.$parent.$eval(attrs.ngModel);
        var d = new Date(currentValue);
        var time = textField.val().toLowerCase().match(/(\d+)(?::(\d\d))?\s*(p?)/);
        d.setHours(parseInt(time[1]) + (time[3] && time[1] < 12 ? 12 : 0));
        d.setMinutes(parseInt(time[2]) || 0);
        var newValue = d.getTime();
        /* if (!newValue && !currentValue) {
          return;
        } */
        if (currentValue != newValue) {
          console.log('Time changed from ' + currentValue + ' to ' + newValue);
          scope.$parent.$eval(attrs.ngModel + ' = ' + newValue);
          scope.$parent.$apply(attrs.dd4Timepicker);
        }
      });
      $compile(textField)(scope.$parent);
    }
  }
});

com.digitald4.common.module.directive('myDirective', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '='
    },
    template: '<div class="some"><label for="{{id}}">{{label}}</label>' +
      '<input id="{{id}}" ng-model="value"></div>',
    replace: true,
    require: 'ngModel',
    link: function($scope, elem, attr, ctrl) {
      $scope.label = attr.ngModel;
      $scope.id = attr.ngModel;
      console.debug(attr.ngModel);
      console.debug($scope.$parent.$eval(attr.ngModel));
      var textField = $('input', elem).
        attr('ng-model', attr.ngModel).
        val($scope.$parent.$eval(attr.ngModel));

      $compile(textField)($scope.$parent);
    }
  };
});

com.digitald4.common.module.controller('TableCtrl', com.digitald4.common.TableCtrl);

com.digitald4.common.module.directive('dd4Table', function() {
  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    scope: {metadata: '=dd4Table'},
    controller: 'TableCtrl as tableCtrl',
    templateUrl: 'js/html/dd4table.html',
    compile: function() {
      return {
        post: function(scope, element, attributes) {
          scope.$watch('loading', function(loading) {
            if (loading) {
              return;
            }
            setTimeout(function() {
              var table = $(element.find('table'));
              //table.dataTable();
              oTable = table.dataTable({
                //"bSort": false,

                /*
                 * We set specific options for each columns here. Some columns contain raw data to enable correct sorting, so we convert it for display
                 * @url http://www.datatables.net/usage/columns
                 */
                /*aoColumns: [
                  { bSortable: false},	// No sorting for this columns, as it only contains checkboxes
                  { sType: 'string' },
                  { sType: 'string' },
                  { sType: 'string' },
                  { sType: 'string' },
                  { sType: 'string' }
                ],*/

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
            }, 500);
          });
        }
      }
    },
  };
});
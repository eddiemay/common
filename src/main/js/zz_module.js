com.digitald4.common.module = angular.module('DD4Common', ['ngCookies'])
    .factory('globalData', function() { return new com.digitald4.common.GlobalData(); })
    .factory('flags', function() { return {}; })
    .service('apiConnector', com.digitald4.common.ApiConnector)
    .service('fileService', com.digitald4.common.FileService)
    .service('flagService', com.digitald4.common.FlagService)
    .service('generalDataService', com.digitald4.common.GeneralDataService)
    .service('sessionWatcher', com.digitald4.common.SessionWatcher)
    .service('userPreferences', com.digitald4.common.UserPreferences)
    .service('userService', com.digitald4.common.UserService)
    .controller('DD4AppCtrl', ['$cookies', '$interval', '$scope', 'flags', 'globalData', 'userService', 'sessionWatcher',
        function($cookies, $interval, $scope, flags, globalData, userService, sessionWatcher) {
      this.flags = flags;
      this.globalData = globalData;
      globalData.activeSession = $cookies.getObject('activeSession');
      this.isUserLoggedIn = function() { return globalData.activeSession != undefined; }
      if (globalData.activeSession) {
        sessionWatcher.enable();
      }
      this.logout = function() {
        userService.logout();
      }
      this.getFileUrl = function(fileReference, type) {
        return userService.getFileUrl(fileReference, type);
      }
      interval = $interval(function() {
        flags.ready = true;
        $interval.cancel(interval);
      }, 2 * 1000);
    }])
    .controller('UserCtrl', com.digitald4.common.UserCtrl)
    .component('dd4Chat', {
      templateUrl: 'js/html/dd4chat.html',
      controller: com.digitald4.common.ChatCtrl,
      bindings: {
        title: '@',
        description: '@',
        url: '@',
        chatFunc: '&?'
      }
    })
    .component('dd4Input', {
      templateUrl: 'js/html/dd4input.html',
      controller: function() {
        if (this.ngModel) {
          this.ack = true;
        }
      },
      bindings: {
        label: '@',
        type: '@',
        name: '@',
        options: '<',
        ngModel: '=',
        onChange: '&'
      }
    })
    .component('dd4Login', {
      templateUrl: 'js/html/login.html',
      controller: com.digitald4.common.LoginCtrl,
      restrict: 'AE',
      bindings: {
        allowSignup: '@',
        label: '@',
        onCancel: '&',
        onLoginSuccess: '&',
      }
    })
    .directive('dd4Time', function() {
      return {
        restrict: 'E',
        scope: {
          label: '@',
          ngModel: '=',
          onUpdate: '&'
        },
        template: '<span><label data-ng-if="$ctrl.label">{{$ctrl.label}}</label>'
            + '<input type="text" value="{{ngModel | date:\'HH:mm\'}}" data-on-change="handleChange()" size="5"></span>',
        link: function(scope, element, attrs) {
          var textField = $('input', element);
          scope.handleChange = function() {
            scope.$parent.$eval(attrs.ngModel + ' = ' + new Date('01/01/1979 ' + textField.val()).getTime());
            scope.onUpdate();
          };
        }
      };
    })
    .directive('dd4MultiCheck', ['$compile', function($compile) {
      return {
        restrict: 'E',
        scope: {
          label: '@',
          options: '<',
          ngModel: '=',
          onUpdate: '&'
        },
        template: '<span><label data-ng-if="label">{{label}}</label>'
            + '  <span data-ng-repeat="option in options">'
            + '    <input type="checkbox" data-ng-model="option.selected" data-ng-change="handleChange()"/>{{option.name}}'
            + '</span></span>',
        link: function(scope, element, attrs) {
          if (typeof scope.options[0] === 'string') {
            var options = []
            for (var i = 0; i < scope.options.length; i++) {
              options.push({id: scope.options[i], name: scope.options[i]});
            }
            this.options = scope.options = options;
          }
          scope.$watch('ngModel', function(selected) {
            selected = selected || [];
            for (var i = 0; i < scope.options.length; i++) {
              scope.options[i].selected = selected.indexOf(scope.options[i].id) >= 0;
            }
          });
          scope.handleChange = function() {
            scope.$parent.$eval(attrs.ngModel + ' = []');
            var selected = scope.$parent.$eval(attrs.ngModel);
            for (var i = 0; i < scope.options.length; i++) {
              if (scope.options[i].selected) {
                selected.push(scope.options[i].id);
              }
            }
            scope.onUpdate();
          };
        }
      }
    }])
    .directive('onChange', function() {
      return function(scope, element, attrs) {
        var startingValue = element.val();
        element.bind('blur', function() {
          if (startingValue != element.val()) {
            scope.$apply(attrs.onChange);
            startingValue = element.val();
           }
        });
       }
    })
    .directive('onEnter', function () {
      return function(scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
          if (event.which === 13) {
            scope.$apply(function () {
              scope.$eval(attrs.onEnter);
            });
            event.preventDefault();
          }
        });
      };
    })
    .component('youtubeVideo', {
      controller: function() {
        this.url = 'https://www.youtube.com/embed/' + this.video;
        this.width = this.width || 420;
        this.height = this.height || 345;
      },
      bindings: {
        video: '@',
        width: '@',
        height: '@'
      },
      template: '<iframe ng-src="{{$ctrl.url | trusted}}" width="{{$ctrl.width}}" height="{{$ctrl.height}}"></iframe>'
    })


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

com.digitald4.common.module.directive('dd4Address', ['$compile', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      label: '@',
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
          if (typeof(gpsAddress) == 'undefined') {
            // scope.$parent.$eval(attrs.ngModel + ' = {}');
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
}]);

com.digitald4.common.module.directive('dd4Datepicker', ['$compile', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '=',
      onUpdate: '&'
    },
    template: '<span><label data-ng-if="label">{{label}}</label>' +
        '<input type="text" class="datepicker" value="{{ngModel | date:\'MM/dd/yyyy\'}}" size="10"/>' +
        '&nbsp;<img src="images/icons/fugue/calendar-month.png" width="16" height="16"/></span>',
    replace: true,
    require: 'ngModel',
    link: function(scope, element, attrs) {
      var textField = $('input', element);

      var update = function(date) {
        var currentValue = scope.$parent.$eval(attrs.ngModel);
        var newDate = new Date(currentValue);
        newDate.setFullYear(date.getFullYear());
        newDate.setMonth(date.getMonth());
        newDate.setDate(date.getDate());
        var newValue = newDate.getTime();

        if (isNaN(newValue)) {
            newValue = undefined;
        }
        if (currentValue != newValue) {
          console.log('DateTime changed from ' + currentValue + ' to ' + newValue);
          scope.$parent.$eval(attrs.ngModel + ' = ' + newValue);
          scope.$parent.$apply(attrs.dd4Datepicker);
          if (attrs.onUpdate) {
            scope.$parent.$eval(attrs.onUpdate);
          }
        }
      };

      textField.bind('blur', function() { update(new Date(textField.val())); });
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
}]);

com.digitald4.common.module.directive('dd4Timepicker', ['$compile', function($compile) {
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
}]);

com.digitald4.common.module.controller('TableCtrl', com.digitald4.common.TableCtrl);

com.digitald4.common.module.component('dd4Table', {
  controller: com.digitald4.common.TableCtrl,
  bindings: {
    metadata: '=',
  },
  templateUrl: 'js/html/dd4table.html',
})
com.digitald4.common.module.directive('dd4Table', function() {
  return {
    restrict: 'A',
    replace: true,
    transclude: true,
    scope: {metadata: '=dd4Table'},
    controller: 'TableCtrl as $ctrl',
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
                /*
                 * We set specific options for each columns here. Some columns contain raw data to
                 * enable correct sorting, so we convert it for display
                 * @url http://www.datatables.net/usage/columns
                 */
                aoColumnDefs: [
                  {"bSortable": false, "aTargets": [0]},
                  {"bSearchable": false, "aTargets": [0]}
                ],

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

              /* Sorting arrows behaviour
              table.find('thead .sort-up').click(function(event) {
                // Stop link behaviour
                event.preventDefault();

                // Find column index
                var column = table.closest('th');
                var columnIndex = column.parent().children().index(column.get(0));

                // Send command
                oTable.fnSort([[columnIndex, 'asc']]);

                // Prevent bubbling
                return false;
              });
              table.find('thead .sort-down').click(function(event) {
                // Stop link behaviour
                event.preventDefault();

                // Find column index
                var column = table.closest('th');
                var columnIndex = column.parent().children().index(column.get(0));

                // Send command
                oTable.fnSort([[columnIndex, 'desc']]);

                // Prevent bubbling
                return false;
              }); */
            }, 1000);
          });
        }
      }
    },
  };
});
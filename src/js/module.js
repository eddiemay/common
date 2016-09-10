com.digitald4.common.module = angular.module('DD4Common', []);

com.digitald4.common.module.service('restService', com.digitald4.common.JSONConnector);
com.digitald4.common.module.service('generalDataService', com.digitald4.common.GeneralDataService);

com.digitald4.common.module.controller('LoginCtrl', com.digitald4.common.LoginCtrl);

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
    var google = google || null;
    if (google) {
      google.maps.event.addDomListener(window, 'load', addMapAutoComplete(element.get(0), function(place) {
        scope.place = place;
        scope.$apply(attrs.mapauto);
      }));
    }
  }
});

com.digitald4.common.module.directive('dd4Datepicker', function($compile) {
  return {
    restrict: 'AE',
    scope: {
      ngModel: '='
    },
    template: '<div><label data-ng-if="label">{{label}}</label>' +
        '<input type="text" class="datepicker" value="{{ngModel | date:\'MM/dd/yyyy\'}}" size="10"/>' +
        '<img src="images/icons/fugue/calendar-month.png" width="16" height="16"/></div>',
    replace: true,
    require: 'ngModel',
    link: function(scope, element, attrs) {
      var textField = $('input', element);//.
          //attr('data-ng-model', 'date').
          //val(scope.$parent.$eval(attrs.ngModel));

      textField.bind('blur', function() {
        var newValue = new Date(textField.val()).getTime();
        if (scope.$parent.$eval(attrs.ngModel) != newValue) {
          scope.$parent.$eval(attrs.ngModel + ' = ' + newValue);
          scope.$parent.$apply(attrs.dd4Datepicker);
        }
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
          scope.$parent.$eval(attrs.ngModel + ' = ' + new Date(dateText).getTime() + ';');
          $(this).change();
          scope.$parent.$apply(attrs.dd4Datepicker);
        }
      });
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
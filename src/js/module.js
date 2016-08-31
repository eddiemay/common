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
  };
});

com.digitald4.common.module.directive('mapauto', function() {
  return function(scope, element, attrs) {
		google.maps.event.addDomListener(window, 'load', addMapAutoComplete(element.get(0), function(place) {
			scope.place = place;
			scope.$apply(attrs.mapauto);
		}));
  };
});
com.digitald4.common.UserPreferences = ['$cookies', function($cookies) {
  var userPreferences = $cookies.getObject('userPreferences') || {};

  this.put = function(prop, value) {
    if (userPreferences[prop] != value) {
      userPreferences[prop] = value;
      $cookies.putObject('userPreferences', userPreferences);
    }
  }

  this.get = function(prop) {
    return userPreferences[prop];
  }
}];
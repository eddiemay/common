var ONE_MINUTE = 60000;
var SESSION_TIME = 30 * ONE_MINUTE;

com.digitald4.common.SessionWatcher = ['globalData', 'userService', function(globalData, userService) {
  var interval;

  this.startTimer = function() {
    interval = setInterval(function() {
      if (Date.now() > globalData.expiration) {
        userService.logout(function() {
          globalData.user = undefined;
        }, notify);
      } else {
        console.log(((globalData.expiration - Date.now()) / 1000) + ' seconds remainning in session.');
      }
    }, ONE_MINUTE);
  };

  this.disable = function() {
    console.log("Disabling session watcher");
    clearInterval(interval);
    globalData.expiration = 0;
  };
}];
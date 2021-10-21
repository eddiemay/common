com.digitald4.common.SessionWatcher = ['globalData', 'userService', function(globalData, userService) {
  var interval;

  this.enable = function() {
    console.log("Enabling session watcher");
    interval = setInterval(function() {
      if (Date.now() > globalData.expiration) {
        userService.logout();
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
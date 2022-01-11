com.digitald4.common.SessionWatcher = ['globalData', 'userService', function(globalData, userService) {
  var interval;

  this.enable = function() {
    console.log("Enabling session watcher");
    interval = setInterval(function() {
      var now = Date.now();
      if (globalData.activeSession == undefined) {
        // If the session has ended, disable the watcher.
        this.disable();
      } else if (now > globalData.activeSession.expTime) {
        // When the time is up, we need to check the server session to find out if it as truly expired.
        userService.getActiveSession(function(session) {
          globalData.activeSession = session;
          if (session == undefined || session.state == "CLOSED") {
            userService.logout();
          } else {
            console.log(
              'Session extended, ' + ((session.expTime - Date.now()) / 1000) + ' seconds remaining in session.');
          }
        }, function() {
          // If we get an error trying to refresh the session then just logout.
          userService.logout();
        });
      } else {
        console.log(((globalData.activeSession.expTime - now) / 1000) + ' seconds remaining in session.');
      }
    }.bind(this), ONE_MINUTE);
  };

  this.disable = function() {
    console.log("Disabling session watcher");
    clearInterval(interval);
  };
}];
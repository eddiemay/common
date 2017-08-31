var ONE_MINUTE = 60000;
var SESSION_TIME = 30 * ONE_MINUTE;

com.digitald4.common.SessionWatcher = ['globalData', function(globalData) {
  var expiration = 0;
  var interval;

  var startTimer = function() {
    interval = setInterval(function() {
      if (Date.now() > expiration) {
        // TODO(eddiemay) Need to call logout on the server.
        globalData.idToken = undefined;
      } else {
        console.log(((expiration - Date.now()) / 1000) + ' seconds remainning in session.');
      }
    }, ONE_MINUTE);
  };

  this.extendTime = function() {
    if (expiration == 0) {
      startTimer();
    }
    expiration = Date.now() + SESSION_TIME;
  };

  this.disable = function() {
    console.log("Disabling session watcher");
    clearInterval(interval);
    expiration = 0;
  };
}];
var ONE_MINUTE = 60000;
var SESSION_TIME = 30 * ONE_MINUTE;

com.digitald4.common.SessionWatcher = function() {
  this.extendTime();
  var interval = setInterval(function() {
    if (Date.now() > this.expiration) {
      document.location.href = 'login.html';
    } else {
      console.log(((this.expiration - Date.now()) / 1000) + ' seconds remainning in session.')
    }
  }.bind(this), ONE_MINUTE);

  this.disable = function() {
    console.log("Disabling session watcher");
    clearInterval(interval);
  };
};

com.digitald4.common.SessionWatcher.prototype.extendTime = function() {
  this.expiration = Date.now() + SESSION_TIME;
};
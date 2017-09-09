var ONE_MINUTE = 60000;
var SESSION_TIME = 30 * ONE_MINUTE;

com.digitald4.common.GlobalData = function() {
  this.expiration = 0;

  this.extendTime = function() {
    this.expiration = Date.now() + SESSION_TIME;
  };
};
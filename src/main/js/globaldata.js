com.digitald4.common.GlobalData = function() {
  this.expiration = 0;

  this.extendTime = function() {
    this.expiration = Date.now() + SESSION_TIME;
  };
};
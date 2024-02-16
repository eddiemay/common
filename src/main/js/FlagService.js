com.digitald4.common.FlagServ = function(apiConnector, flags) {
  this.jsonService = new com.digitald4.common.JSONService('flag', apiConnector);
  this.flags = flags;
  this.refresh();
}

com.digitald4.common.FlagService = ['apiConnector', com.digitald4.common.FlagServ];

com.digitald4.common.FlagServ.prototype.refresh = function() {}
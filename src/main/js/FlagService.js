com.digitald4.common.FlagServ = function(apiConnector, flags) {
  this.jsonService = new com.digitald4.common.JSONService('flag', apiConnector);
  this.flags = flags;
  this.refresh();
}

com.digitald4.common.FlagService = ['apiConnector', 'flags', com.digitald4.common.FlagServ];

com.digitald4.common.FlagServ.prototype.refresh = function() {
	this.jsonService.list({pageSize: 0}, function(response) {
		var flagList = response.items;
		for (var x = 0; x < flagList.length; x++) {
			this.flags[flagList[x].id] = flagList[x].value;
		}
  }.bind(this));
}
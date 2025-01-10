com.digitald4.common.FlagServ = function(apiConnector, flags) {
  this.jsonService = new com.digitald4.common.JSONService('flag', apiConnector);
  this.flags = flags;
  this.refresh();
}

com.digitald4.common.FlagService = ['apiConnector', 'flags', com.digitald4.common.FlagServ];

com.digitald4.common.FlagServ.prototype.refresh = function() {
  this.callers = [];
	this.jsonService.list({pageSize: 0}, function(response) {
		var flagList = response.items;
		for (var x = 0; x < flagList.length; x++) {
			this.flags[flagList[x].id] = flagList[x].value;
		}
		for (var c = 0; c < this.callers.length; c++) {
		  this.callers[c](this.flags);
		}
		this.flags.ready = true;
		this.callers = undefined;
  }.bind(this));
}

/**
* Async function to get the full list of flags set for the user.
* @param success - Function(flags) - Function for the flags that are set for the user.
*/
com.digitald4.common.FlagServ.prototype.getAll = function(success, error) {
  if (this.callers) {
    this.callers.push(success);
  } else {
    success(this.flags);
  }
}
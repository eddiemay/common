com.digitald4.common.GeneralDataServ = function(apiConnector) {
  this.jsonService = new com.digitald4.common.JSONService('generalData', apiConnector);
  this.refresh();
}

com.digitald4.common.GeneralDataService = ['apiConnector', com.digitald4.common.GeneralDataServ];

com.digitald4.common.GeneralDataServ.prototype.refresh = function() {
  this.callers = [];
  this.jsonService.list({pageSize: 0}, function(response) {
    var generalDatas = response.items;
    var map = {};
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      if (generalData.data) {
        generalData.data = JSON.parse(generalData.data);
      }
      generalData.generalDatas = [];
      map[generalData.id] = generalData;
    }
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      if (generalData.groupId > 0) {
        var groupLeader = map[generalData.groupId];
        if (groupLeader) {
          groupLeader.generalDatas.push(generalData);
        } else {
          console.log('Can not find group id: ' + generalData.groupId + ' for ' + generalData.toString());
        }
      }
    }
    this.generalDataMap = map;
    for (var c = 0; c < this.callers.length; c++) {
      this.callers[c](map);
    }
    this.callers = undefined;
  }.bind(this));
}

com.digitald4.common.GeneralDataServ.prototype.get = function(id, success, error) {
  success = success || function() {};
  if (!this.generalDataMap) {
    this.callers.push(function(generalDataMap) {success(generalDataMap[id] || {generalDatas: []})});
    // For legacy callers before cache is set, returns empty result.
    return {generalDatas: []};
  } else {
    success(this.generalDataMap[id] || {generalDatas: []});
  }

  // This is here for legacy purposes of callers that expect a return.
  return this.generalDataMap[id] || {generalDatas: []};
}

com.digitald4.common.GeneralDataServ.prototype.list = function(groupId, success, error) {
  success = success || function() {};
  var result = this.get(groupId, function(gd) {success(gd.generalDatas)}, error);

  // This is here for legacy purposes of callers that expect a return.
  // Will return empty array if data has not been fetched.
  return result.generalDatas;
}
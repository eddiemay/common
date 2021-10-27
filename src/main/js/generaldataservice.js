com.digitald4.common.GeneralDataServ = function(apiConnector) {
  this.jsonService = new com.digitald4.common.JSONService('generalData', apiConnector);
  this.refresh();
};

com.digitald4.common.GeneralDataService = ['apiConnector', com.digitald4.common.GeneralDataServ];

com.digitald4.common.GeneralDataServ.prototype.generalDataMap = {};

com.digitald4.common.GeneralDataServ.prototype.refresh = function() {
  this.jsonService.list({}, function(response) {
    var generalDatas = response.results;
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
  }.bind(this), notify);
};

com.digitald4.common.GeneralDataServ.prototype.get = function(id) {
  return this.generalDataMap[id] || {};
};

com.digitald4.common.GeneralDataServ.prototype.list = function(groupId) {
  return this.get(groupId).generalDatas;
};
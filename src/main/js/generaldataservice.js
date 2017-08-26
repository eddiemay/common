com.digitald4.common.GeneralDataServ = function(apiConnector) {
  this.jsonService = new com.digitald4.common.JSONService('general_data', apiConnector);
  this.refresh();
};

com.digitald4.common.GeneralDataService = ['apiConnector', com.digitald4.common.GeneralDataServ];

com.digitald4.common.GeneralDataServ.prototype.generalDataHash = {};

com.digitald4.common.GeneralDataServ.prototype.refresh = function() {
  this.jsonService.list({}, function(listResponse) {
    var generalDatas = listResponse.result;
    var hash = {};
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      if (generalData.data) {
        generalData.data = JSON.parse(generalData.data);
      }
      generalData.generalDatas = [];
      hash[generalData.id] = generalData;
    }
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      if (generalData.group_id) {
        var group = hash[generalData.group_id];
        if (group) {
          hash[generalData.group_id].generalDatas.push(generalData);
        } else {
          console.log('Can not find group id: ' + generalData.group_id + ' for ' + generalData.toString());
        }
      }
    }
    this.generalDataHash = hash;
  }.bind(this), notify);
};

com.digitald4.common.GeneralDataServ.prototype.get = function(id) {
  return this.generalDataHash[id] || {};
};

com.digitald4.common.GeneralDataServ.prototype.list = function(groupId) {
  return this.get(groupId).generalDatas;
};
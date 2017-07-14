com.digitald4.common.GeneralDataService = function(apiConnector) {
  this.jsonService = new com.digitald4.common.JSONService('general_data', apiConnector);
  this.refresh();
};

com.digitald4.common.GeneralDataService.prototype.jsonService;
com.digitald4.common.GeneralDataService.prototype.generalDataHash = {};

com.digitald4.common.GeneralDataService.prototype.refresh = function() {
  this.jsonService.list({}, function(listResponse) {
    var generalDatas = listResponse.items;
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

com.digitald4.common.GeneralDataService.prototype.get = function(id) {
  return this.generalDataHash[id] || {};
};

com.digitald4.common.GeneralDataService.prototype.list = function(groupId) {
  return this.get(groupId).generalDatas;
};
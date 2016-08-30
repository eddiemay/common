com.digitald4.common.GeneralDataService = function(restService) {
  this.protoService = new com.digitald4.common.ProtoService('general_data', restService);
  this.refresh();
};

com.digitald4.common.GeneralDataService.prototype.protoService;
com.digitald4.common.GeneralDataService.prototype.generalDataHash = {};

com.digitald4.common.GeneralDataService.prototype.refresh = function() {
  this.protoService.list({}, function(generalDatas) {
    var hash = {};
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      generalData.generalDatas = [];
      hash[generalData.id] = generalData;
    }
    for (var x = 0; x < generalDatas.length; x++) {
      var generalData = generalDatas[x];
      if (generalData.group_id) {
        hash[generalData.group_id].generalDatas.push(generalData);
      }
    }
    this.generalDataHash = hash;
  }.bind(this), notify);
};

com.digitald4.common.GeneralDataService.prototype.get = function(id) {
  return this.generalDataHash[id] || {};
};

com.digitald4.common.GeneralDataService.prototype.list = function(parentId) {
  return this.get(parentId).generalDatas;
};
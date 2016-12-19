com.digitald4.common.ProtoService = function(proto, restService) {
	this.restService = restService;
	this.proto = proto;
};

com.digitald4.common.ProtoService.prototype.restService;
com.digitald4.common.ProtoService.prototype.proto;

com.digitald4.common.ProtoService.prototype.performRequest = function(action, request, success, error) {
  this.restService.performRequest(this.proto + '/' + action, request, success, error);
};

com.digitald4.common.ProtoService.prototype.get = function(id, success, error) {
	this.performRequest('get', {id: id}, success, error);
};

com.digitald4.common.ProtoService.prototype.list = function(params, success, error) {
	this.performRequest('list', {query_param: params}, success, error);
};

com.digitald4.common.ProtoService.prototype.create = function(newProto, success, error) {
  newProto.$$hashKey = undefined;
  var proto = JSON.stringify(newProto);
  console.log('proto: ' + proto);
	this.performRequest('create', {proto: proto}, success, error);
};

com.digitald4.common.ProtoService.prototype.update = function(proto, props, success, error) {
	var request = {id: proto.id, update: []};
	for (var p = 0; p < props.length; p++) {
	  var prop = props[p];
	  request.update.push({property: prop, value: proto[prop].toString()});
	}
	this.performRequest('update', request, success, error);
};

com.digitald4.common.ProtoService.prototype.Delete = function(id, success, error) {
	this.performRequest('delete', {id: id}, success, error);
};

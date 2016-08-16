com.digitald4.common.ProtoService = function(proto, restService) {
	this.restService = restService;
	this.getUrl = proto;
	this.listUrl = proto + 's';
	this.createUrl = 'create_' + proto;
	this.updateUrl = 'update_' + proto;
	this.deleteUrl = 'delete_' + proto;
};

com.digitald4.common.ProtoService.prototype.restService;

com.digitald4.common.ProtoService.prototype.get = function(id, success, error) {
	this.restService.performRequest(this.getUrl, {id: id}, success, error);
};

com.digitald4.common.ProtoService.prototype.list = function(params, success, error) {
	this.restService.performRequest(this.listUrl, params, success, error);
};

com.digitald4.common.ProtoService.prototype.create = function(newProto, success, error) {
	this.restService.performRequest(this.createUrl, {proto: newProto}, success, error);
};

com.digitald4.common.ProtoService.prototype.update = function(proto, prop, success, error) {
	var request = {id: proto.id, property: prop, value: proto[prop].toString()};
	this.restService.performRequest(this.updateUrl, request, success, error);
};

com.digitald4.common.ProtoService.prototype.Delete = function(id, success, error) {
	this.restService.performRequest(this.deleteUrl, {id: id}, success, error);
};

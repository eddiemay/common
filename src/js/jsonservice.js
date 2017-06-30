com.digitald4.common.JSONService = function(proto, apiConnector) {
	this.apiConnector = apiConnector;
	this.service = proto + 's';
};

com.digitald4.common.JSONService.prototype.apiConnector;
com.digitald4.common.JSONService.prototype.service;

/**
 * Performs the specified request.
 *
 * @param {string | array} method The HTTP method to use for the request. If this is a custom action then action, method
 *   as an array i.e. ['summary', 'post']. If the method is GET then method can be left off of array.
 * @param {string | number | Object} requestParams The request parameters to use to build the url. If standard url this
 *  can simplily be the id of the item being requested. If an object id should be .id of that object.
 * @param {Object} request The body information to send to the server.
 * @param {!function(!Object)} success The call back function to call after a successful submission.
 * @param {!function(!Object)} error The call back function to call after a submission error.
 */
com.digitald4.common.JSONService.prototype.performRequest = function(method, requestParams, request, success, error) {
  var url = [];
  var id;
  if (typeof(requestParams) == 'object') {
    for (var prop in requestParams) {
      if (prop == 'id') {
        id = requestParams[prop];
      } else if (requestParams[prop]) {
        url.push(prop.indexOf('_id') == -1 ? prop : prop.substring(0, prop.length - 3) + 's');
        url.push(requestParams[prop]);
      }
    }
  } else if (typeof(requestParams) != 'undefined') {
    id = requestParams;
  }
  url.push(this.service);
  if (id) {
    url.push(id);
  }
  var customAction = undefined;
  if (typeof(method) == 'object') {
    customAction = method[0];
    method = method[1] || 'GET';
  }

  this.apiConnector.performRequest(method, url.join('/') + (customAction ? ':' + customAction : ''), request, success, error);
};

com.digitald4.common.JSONService.prototype.get = function(id, success, error) {
	this.performRequest('GET', id, undefined, success, error);
};

com.digitald4.common.JSONService.prototype.list = function(filter, success, error) {
	this.performRequest('GET', undefined, filter, success, error);
};

com.digitald4.common.JSONService.prototype.create = function(proto, success, error) {
  proto.$$hashKey = undefined;
	this.performRequest('POST', undefined, {proto: JSON.stringify(proto)}, success, error);
};

com.digitald4.common.JSONService.prototype.update = function(proto, props, success, error) {
	var updated = {};
	for (var p = 0; p < props.length; p++) {
	  updated[props[p]] = proto[props[p]];
	}
	this.performRequest('POST', proto.id, {proto: JSON.stringify(updated)}, success, error);
};

com.digitald4.common.JSONService.prototype.Delete = function(id, success, error) {
	this.performRequest('DELETE', id, undefined, success, error);
};

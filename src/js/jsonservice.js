com.digitald4.common.JSONService = function(proto, apiConnector) {
	this.apiConnector = apiConnector;
	this.service = proto + 's';
};


/**
 * Performs the specified request.
 *
 * @param {string | array} method The HTTP method to use for the request. If this is a custom action then action, method
 *   as an array i.e. ['summary', 'post']. If the method is GET then method can be left off of array.
 * @param {string | number | Object} requestParams The request parameters to use to build the url. If standard url this
 *  can simply be the id of the item being requested. If an object id should be .id of that object.
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


/**
* Gets an object from the data store by id.
*
* @param {number} id The unique id of the object to restrieve.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.get = function(id, success, error) {
	this.performRequest('GET', id, undefined, success, error);
};


/**
* Gets a list of objects from the data store.
*
* @param {string | number | Object} requestParams The request parameters to use to build the url. If standard url this
*  can simply be the id of the item being requested. If an object id should be .id of that object.
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.list_ = function(requestParams, listOptions, success, error) {
  this.performRequest('GET', requestParams, listOptions, function(response) {
    response.result = response.result || [];
    success(response);
  }, error);
};


/**
* Gets a list of objects from the data store.
*
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.list = function(listOptions, success, error) {
  this.list_(undefined, listOptions, success, error);
};


/**
* Creates a new object.
*
* @param {Object} proto The object to create.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.create = function(proto, success, error) {
  proto.$$hashKey = undefined;
	this.performRequest('POST', undefined, {proto: proto}, success, error);
};


/**
* Updates an object in the data store.
*
* @param {Object} proto The object to update.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.update = function(proto, props, success, error) {
	var updated = {};
	for (var p = 0; p < props.length; p++) {
	  updated[props[p]] = proto[props[p]];
	}
	this.performRequest('POST', proto.id, {proto: updated, updateMask: props.join()}, success, error);
};


/**
* Deletes an object from the data store.
*
* @param {number} id The id of the object to delete.
* @param {!function(!Object)} success The call back function to call after a successful submission.
* @param {!function(!Object)} error The call back function to call after a submission error.
*/
com.digitald4.common.JSONService.prototype.Delete = function(id, success, error) {
	this.performRequest('DELETE', id, undefined, success, error);
};

com.digitald4.common.JSONService = function(resource, apiConnector) {
	this.apiConnector = apiConnector;
	this.service = resource + 's';
};


/**
 * Performs the specified request.
 *
 * @param {string | array} method The HTTP method to use for the request. If this is a custom action then action, method
 *   as an array i.e. ['summary', 'post']. If the method is GET then method can be left off of array.
 * @param {string | number | Object} requestParams The request parameters to use to build the url. If standard url this
 *  can simply be the id of the item being requested. If an object id should be .id of that object.
 * @param {Object} request The body information to send to the server.
 * @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
 * @param {!function(!Object)} onError The call back function to call after a submission onError.
 */
com.digitald4.common.JSONService.prototype.performRequest = function(method, requestParams, request, onSuccess, onError) {
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

  this.apiConnector.performRequest(method, url.join('/') + (customAction ? ':' + customAction : ''), request, onSuccess, onError);
};


/**
* Gets an object from the data store by id.
*
* @param {number} id The unique id of the object to restrieve.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.get = function(id, onSuccess, onError) {
	this.performRequest('GET', id, undefined, onSuccess, onError);
};


/**
* Gets a list of objects from the data store.
*
* @param {string | number | Object} requestParams The request parameters to use to build the url. If standard url this
*  can simply be the id of the item being requested. If an object id should be .id of that object.
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.list_ = function(requestParams, listOptions, onSuccess, onError) {
  this.performRequest('GET', requestParams, listOptions, function(response) {
    response.result = response.result || [];
    onSuccess(response);
  }, onError);
};


/**
* Gets a list of objects from the data store.
*
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.list = function(listOptions, onSuccess, onError) {
  this.list_(undefined, listOptions, onSuccess, onError);
};


/**
* Creates a new object.
*
* @param {Object} entity The object to create.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.create = function(entity, onSuccess, onError) {
  entity.$$hashKey = undefined;
	this.performRequest('POST', undefined, {entity: entity}, onSuccess, onError);
};


/**
* Updates an object in the data store.
*
* @param {Object} entity The object to update.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.update = function(entity, props, onSuccess, onError) {
	var updated = {};
	for (var p = 0; p < props.length; p++) {
	  updated[props[p]] = entity[props[p]];
	}
	this.performRequest('POST', entity.id, {entity: updated, updateMask: props.join()}, onSuccess, onError);
};


/**
* Deletes an object from the data store.
*
* @param {number} id The id of the object to delete.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.Delete = function(id, onSuccess, onError) {
	this.performRequest('DELETE', id, undefined, onSuccess, onError);
};

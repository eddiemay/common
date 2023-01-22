com.digitald4.common.JSONService = function(resource, apiConnector) {
	this.apiConnector = apiConnector;
	this.service = resource + 's/v1';
}

/**
 * Performs the specified request.
 *
 * @param {string | array} method The HTTP method to use for the request. If this is a custom action then action, method
 *   as an array i.e. ['summary', 'post']. If the method is GET then method can be left off of array.
 * @param {string | number | Object} urlParams The request parameters to use to build the url. If standard url this
 *  can simply be the id of the item being requested. If an object id should be the id of that object.
 * @param {array} reqParams The request parameters that will show after under '?' in the url i.e. ?updateMask=name,email
 * @param {Object} data The body post data information to send to the server.
 * @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
 * @param {!function(!Object)} onError The call back function to call after a submission onError.
 */
com.digitald4.common.JSONService.prototype.performRequest =
    function(method, urlParams, reqParams, data, onSuccess, onError) {
  var customAction = undefined;
  if (typeof(method) == 'object') {
    customAction = method[0];
    method = method[1] || 'GET';
  }
  this.sendRequest(
      {method: method, urlParams: urlParams, action: customAction, params: reqParams, data: data}, onSuccess, onError);
}


/**
 * Performs the specified request.
 *
 * @param {Object{method:string, action:string, urlParams:{string | number | Object}, params: Object, data: Object}}
 *   The request information of http method, the action to be performed, any url parameters to be applied, the request
 *   parameters and the post data. A url will be built from this information or a url may be specified as well.
 * @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
 * @param {!function(!Object)} onError The call back function to call after a submission onError.
 */
com.digitald4.common.JSONService.prototype.sendRequest = function(request, onSuccess, onError) {
  var urlParams = request.urlParams;
  var url = [];
  url.push(this.service);
  var id;
  if (typeof(urlParams) == 'object') {
    for (var prop in urlParams) {
      if (prop == 'id') {
        id = urlParams[prop];
        url.push(id);
      } else if (urlParams[prop]) {
        url.push(prop.indexOf('_id') == -1 ? prop : prop.substring(0, prop.length - 3) + 's');
        url.push(urlParams[prop]);
      }
    }
  } else if (typeof(urlParams) != 'undefined') {
    id = urlParams;
    url.push(id);
  }
  if (request.action) {
    url.push(request.action);
  } else if (!id && !urlParams) {
    url.push("_");
  }

  request.url = request.url || url.join('/');
  this.apiConnector.sendRequest(request, onSuccess, onError);
}

/**
* Creates a new object.
*
* @param {Object} entity The object to create.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.create = function(entity, onSuccess, onError) {
  entity.$$hashKey = undefined;
  this.sendRequest({action: 'create', method: 'POST', data: entity}, onSuccess, onError);
}

/**
* Gets an object from the data store by id.
*
* @param {number} id The unique id of the object to restrieve.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.get = function(id, onSuccess, onError) {
	this.sendRequest({action: 'get', method: 'GET', urlParams: id}, onSuccess, onError);
}

/**
* Gets a list of objects from the data store.
*
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.list = function(request, onSuccess, onError) {
  this.list_(undefined, request, onSuccess, onError);
}

/**
* Gets a list of objects from the data store.
*
* @param {string | number | Object} urlParams The request parameters to use to build the url. If standard url this
*  can simply be the id of the item being requested. If an object id should be .id of that object.
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.list_ = function(urlParams, listOptions, onSuccess, onError) {
  this.sendRequest({method: 'GET', action: 'list', urlParams: urlParams, params: listOptions}, function(response) {
    onSuccess(processPagination(response));
  }, onError);
}

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
	this.sendRequest(
	    {method: 'PUT', action: 'update', params: {id: entity.id, updateMask: props.join()}, data: updated}, onSuccess, onError);
}

/**
* Deletes an object from the data store.
*
* @param {number} id The id of the object to delete.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.Delete = function(id, onSuccess, onError) {
  this.sendRequest({method: 'DELETE', action: 'delete', urlParams: id}, onSuccess, onError);
}

/**
* Search of objects from the data store.
*
* @param {Object{searchText, orderBy, pageSize, pageToken}} request The parameters associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.search = function(request, onSuccess, onError) {
  this.sendRequest({method: 'GET', action: 'search', params: request}, function(response) {
    onSuccess(processPagination(response));
  }, onError);
}

/**
* Updates a batch of objects in the data store.
*
* @param {Object} entity The object to update.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.batchUpdate = function(entities, props, onSuccess, onError) {
	var updates = [];
	for (var e = 0; e < entities.length; e++) {
	  var entity = entities[e];
	  var updated = {id: entity.id};
    for (var p = 0; p < props.length; p++) {
      updated[props[p]] = entity[props[p]];
    }
    updates.push(updated);
	}
	this.sendRequest(
	    {method: 'PUT', action: 'batchUpdate',  params: {updateMask: props.join()}, data: {items: updates}},
	    onSuccess, onError);
}

processPagination = function(response) {
  response.items = response.items || [];
  response.pageToken = response.pageToken || 0;
  response.pageSize = response.pageSize || 0;
  response.totalSize = response.totalSize || response.items.length;

  response.start = (response.pageToken - 1) * response.pageSize;
  response.end = response.start + response.items.length;
  if (response.end > 0) {
    response.start++;
  }

  response.pages = [];
  if (response.pageSize > 0) {
    for (var p = 0; p < Math.ceil(response.totalSize / response.pageSize);) {
      response.pages.push(++p);
    }
  }

  return response;
}

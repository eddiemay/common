com.digitald4.common.JSONService = function(resource, apiConnector) {
	this.apiConnector = apiConnector;
	this.service = resource + 's/v1';
}

/**
 * Performs the specified request.
 *
 * @param {Object{method:string, action:string, params: Object, data: Object}}
 *   The request information of http method, the action to be performed, any url parameters to be applied, the request
 *   parameters and the post data. A url will be built from this information or a url may be specified as well.
 * @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
 * @param {!function(!Object)} onError The call back function to call after a submission onError.
 */
com.digitald4.common.JSONService.prototype.sendRequest = function(request, onSuccess, onError) {
  request.url = request.url || (this.service + '/' + request.action);
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
  this.sendRequest({action: 'create', method: 'POST', data: entity}, onSuccess, onError);
}

/**
* Gets an object from the data store by id.
*
* @param {number} id The unique id of the object to fetch.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.get = function(id, onSuccess, onError) {
	this.sendRequest({action: 'get', method: 'GET', params: {id: id}}, onSuccess, onError);
}

/**
* Gets a list of objects from the data store.
*
* @param {Object{filter, orderBy, pageSize, pageToken}} listOptions The options associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.list = function(request, onSuccess, onError) {
  this.sendRequest({action: 'list', method: 'GET', params: request}, function(response) {
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
	    {action: 'update', method: 'PUT', params: {id: entity.id, updateMask: props.join()}, data: updated},
	    onSuccess, onError);
}

/**
* Deletes an object from the data store.
*
* @param {number} id The id of the object to delete.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.Delete = function(id, onSuccess, onError) {
  this.sendRequest({action: 'delete', method: 'DELETE', params: {id: id}}, onSuccess, onError);
}

/**
* Search of objects from the data store.
*
* @param {Object{searchText, orderBy, pageSize, pageToken}} request The parameters associated with a list request.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.search = function(request, onSuccess, onError) {
  this.sendRequest({action: 'search', method: 'GET', params: request}, function(response) {
    onSuccess(processPagination(response));
  }, onError);
}

/**
* Creates a batch of objects in the data store.
*
* @param {Array<Object>} entities to create
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.batchCreate = function(entities, onSuccess, onError) {
	this.sendRequest(
	    {action: 'batchCreate', method: 'POST', data: {items: entities}}, onSuccess, onError);
}

/**
* Gets an object from the data store by id.
*
* @param {number} ids The ids of the objects to fetch.
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.batchGet = function(ids, onSuccess, onError) {
	this.sendRequest({action: 'batchGet', method: 'POST', data: {items: ids}}, onSuccess, onError);
}

/**
* Updates a batch of objects in the data store.
*
* @param {Array<Object>} entities to update
* @param {Array<Object>} props properties to update
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
	    {action: 'batchUpdate', method: 'PUT', params: {updateMask: props.join()}, data: {items: updates}},
	    onSuccess, onError);
}

/**
* Deletes a batch of objects in the data store.
*
* @param {Array<String>} ids of the items to delete
* @param {!function(!Object)} onSuccess The call back function to call after a onSuccessful submission.
* @param {!function(!Object)} onError The call back function to call after a submission onError.
*/
com.digitald4.common.JSONService.prototype.batchDelete = function(ids, onSuccess, onError) {
	this.sendRequest({action: 'batchDelete', method: 'PUT', params: {ids: ids.join()}}, onSuccess, onError);
}

com.digitald4.common.JSONService.prototype.getFileUrl = function(fileRef, type) {
  type = type || 'files';
  if (!fileRef) {
    return undefined;
  }
  var fileName = fileRef.name || fileRef;
  var apiConnector = this.apiConnector;
  var globalData = apiConnector.globalData;
  var idTokenParam = globalData.activeSession ? '?idToken=' + globalData.activeSession.id : '';
  return apiConnector.baseUrl + apiConnector.apiUrl + type + '/v1/' + fileName + idTokenParam;
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

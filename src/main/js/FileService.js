var FileService = function(apiConnector, globalData) {
  var fileService = new com.digitald4.common.JSONService('file', apiConnector);

  fileService.upload = function(request, callback) {
    var url = apiConnector.baseUrl + 'files/upload';
    if (globalData.activeSession) {
      url += '?idToken=' + globalData.activeSession.id;
    }
    var xhr = new XMLHttpRequest();
    xhr.addEventListener('progress', function(e) {
      var done = e.position || e.loaded, total = e.totalSize || e.total;
      console.log('xhr progress: ' + (Math.floor(done / total * 1000) / 10) + '%');
    }, false);

    xhr.onreadystatechange = function(e) {
      if (xhr.readyState == 4) {
        console.log(['xhr upload complete', e]);
        console.log(['xhr response', xhr.response]);
        callback(JSON.parse(xhr.response));
      }
    }
    xhr.onerror = function(e) {
      notify("Error uploading file");
    }
    xhr.open('post', url, true);
    var fd = new FormData;
    for (var prop in request) {
      fd.append(prop, prop == 'file' ? request[prop].files[0] : request[prop]);
    }
    xhr.send(fd);
  }

  return fileService;
}

com.digitald4.common.FileService = ['apiConnector', 'globalData', FileService];
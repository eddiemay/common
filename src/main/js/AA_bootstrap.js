var com = com || {};
com.digitald4 = com.digitald4 || {};
com.digitald4.common = com.digitald4.common || {};

notifyError = function(error) {
  if (error.message) {
    notify(error.message);
    return;
  }

  notify(error);
}

setDialogStyle = function(ctrl) {
	// ctrl.dialogStyle = {top: 0};
	ctrl.dialogStyle = {top: ctrl.window.visualViewport.pageTop + 50};
}
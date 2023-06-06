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
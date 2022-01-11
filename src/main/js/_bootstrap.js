var goog = goog || {};
goog.provide = goog.provide || function(provides) {};
goog.require = goog.require || function(requires) {};

var proto = proto || {};
proto.common = proto.common || {};

var com = com || {};
com.digitald4 = com.digitald4 || {};
com.digitald4.common = com.digitald4.common || {};

notifyError = function(error) {
  notify(error.message);
}
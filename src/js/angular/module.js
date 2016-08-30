com.digitald4.common.module = angular.module('DD4Common', []);

com.digitald4.common.module.service('restService', com.digitald4.common.JSONConnector);

com.digitald4.common.module.controller('LoginCtrl', com.digitald4.common.LoginCtrl);
com.digitald4.common.ChatCtrl = ['$http', '$httpParamSerializer', function($http, $httpParamSerializer, globalData) {
  this.chatHistory = [];
    /* {type: 'user_message', message: "What does Gen 2:3 say?"},
    {type: 'agent_message', message: "And Elohim blessed the seventh day and set it apart, because on it He rested from all His work which Elohim in creating had made."},
    {type: 'user_message', message: "What does Exo 20:8 say?"},
    {type: 'agent_message', message: "Remember the sabbath day, and keep it holy."},
    {type: 'user_message', message: "What happens if a person eats blood?"},
    {type: 'agent_message', message: "If a person eats blood, they will be cut off from their people (Leviticus 7:27)"},
  ]; */
  this.chatFunc = this.chatFunc || function(message, successCallback, errorCallback) {
    // success("not yet implemented");
    errorCallback = errorCallback || notifyError;
    $http({
        method: 'GET',
        url: this.url + '?' + $httpParamSerializer({question: message}),
        headers: {'Content-type': 'application/json'}
      }).then(function(response) {
        successCallback(response.data);
      }, function(response) {
        console.log('Status code: ' + response.status);
        if (response.data) {
          console.log('message: ' + response.data.error.message);
          errorCallback(response.data.error);
        } else {
          errorCallback('Error submitting request');
        }
    });
  }

  this.submit = function() {
    this.chatHistory.push({type: 'user_message', message: this.query});
    this.chatFunc(this.query, function(agentMessage) {
      this.chatHistory.push({type: 'agent_message', message: agentMessage});
    }.bind(this));
    this.query = "";
  }

  this.onKey = function(keyEvent) {
    if (keyEvent.which == 13) {
      this.submit();
    }
  }
}];

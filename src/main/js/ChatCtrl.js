com.digitald4.common.ChatCtrl = ['$http', '$httpParamSerializer', 'globalData',
    function($http, $httpParamSerializer, globalData) {
  this.globalData = globalData;
  globalData.showThought = globalData.showThought || false;
  globalData.chatSessionId = globalData.chatSessionId || Math.floor(Math.random() * 32768);
  var sessionId = globalData.chatSessionId;

  var replacer = function(match, p1, /* …, */ pN, offset, string, groups) {
    return '(<scripture ref="' + p1 + '"/>)';
  }

  var format = function(message) {
    return message.replaceAll('\n', '<br>').replaceAll(/\(([\w ]+\.? \d+[:\w,; \-–]*)\)/g, replacer);
  }

  globalData.chatHistory = globalData.chatHistory || [];
    /* [{type: 'user_message', message: "What does Gen 2:3 say?"},
    {type: 'agent_message', message: format("And Elohim blessed the seventh day and set it apart,\n because on it He rested from all His work which Elohim in creating had made.")},
    {type: 'user_message', message: "What does Exo 20:8 say?"},
    {type: 'agent_message', message: "Remember the sabbath day, and keep it holy."},
    {type: 'user_message', message: "What happens if a person eats blood?"},
    {type: 'agent_message', message: format("If a person eats blood, they will be cut off from their people<br><br> (Leviticus 7:27) also see (Gen 9:4)")}
  ]; */

  this.chatFunc = this.chatFunc || function(message, successCallback, errorCallback) {
    // success("not yet implemented");
    errorCallback = errorCallback || notifyError;
    $http({
      method: 'GET',
      url: this.url + '?' + $httpParamSerializer({sessionId: sessionId, question: message}),
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
    this.loading = true;
    globalData.chatHistory.push({type: 'user_message', message: this.query});
    this.chatFunc(this.query, function(responses) {
      for (var i = 0; i < responses.length - 1; i++) {
        globalData.chatHistory.push({type: 'agent_thought', message: format(responses[i])});
      }
      globalData.chatHistory.push({type: 'agent_message', message: format(responses[i])});
      this.loading = false;
    }.bind(this));
    this.query = "";
  }

  this.onKey = function(keyEvent) {
    if (keyEvent.which == 13) {
      this.submit();
    }
  }
}];

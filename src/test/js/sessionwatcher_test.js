describe('Session Watcher Tests', function() {
  it('creates session with timeout 30 minutes from now.', function() {
    var now = Date.now();
    var sessionWatcher = new com.digitald4.common.SessionWatcher();
    expect(sessionWatcher.expiration).toBe(now + 30);
  });
});
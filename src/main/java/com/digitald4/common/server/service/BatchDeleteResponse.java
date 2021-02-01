package com.digitald4.common.server.service;

public class BatchDeleteResponse {
  private final int deleted;

  public BatchDeleteResponse(int deleted) {
    this.deleted = deleted;
  }

  public int getDeleted() {
    return deleted;
  }
}

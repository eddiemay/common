package com.digitald4.common.model;

import com.google.common.collect.ImmutableList;

public class Email extends ModelObjectModUser<Long> {
  private String from;
  private String subject;
  private ImmutableList<Recipient> recipients;
  private StringBuilder message;
  private ImmutableList<FileReference> fileReferences;

  @Override
  public Email setId(Long id) {
    super.setId(id);
    return this;
  }

  public String getFrom() {
    return from;
  }

  public Email setFrom(String from) {
    this.from = from;
    return this;
  }

  public String getSubject() {
    return subject;
  }

  public Email setSubject(String subject) {
    this.subject = subject;
    return this;
  }

  public ImmutableList<Recipient> getRecipients() {
    return recipients;
  }

  public Email setRecipients(Iterable<Recipient> recipients) {
    this.recipients = ImmutableList.copyOf(recipients);
    return this;
  }

  public StringBuilder getMessage() {
    return message;
  }

  public Email setMessage(StringBuilder message) {
    this.message = message;
    return this;
  }

  public Email setMessage(String message) {
    this.message = new StringBuilder(message);
    return this;
  }

  public ImmutableList<FileReference> getFileReferences() {
    return fileReferences;
  }

  public Email setFileReferences(Iterable<FileReference> fileReferences) {
    this.fileReferences = ImmutableList.copyOf(fileReferences);
    return this;
  }

  public static class Recipient {
    public enum Type {To, Cc, Bcc};
    private Type type;
    private String address;
    private String name;

    public static Recipient createTo(String address, String name) {
      return new Recipient().setType(Type.To).setAddress(address).setName(name);
    }

    public static Recipient createCc(String address, String name) {
      return new Recipient().setType(Type.Cc).setAddress(address).setName(name);
    }

    public static Recipient createBcc(String address, String name) {
      return new Recipient().setType(Type.Bcc).setAddress(address).setName(name);
    }

    public Type getType() {
      return type;
    }

    public Recipient setType(Type type) {
      this.type = type;
      return this;
    }

    public String getAddress() {
      return address;
    }

    public Recipient setAddress(String address) {
      this.address = address;
      return this;
    }

    public String getName() {
      return name;
    }

    public Recipient setName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public String toString() {
      return name == null ? address : String.format("%s (%s) CC", address, name);
    }
  }
}

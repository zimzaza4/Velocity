package com.velocitypowered.proxy.util.exceptions;

public class UnexpectedDisconnectException extends Exception {
  public static final UnexpectedDisconnectException INSTANCE = new UnexpectedDisconnectException();

  private UnexpectedDisconnectException() {
    super();
  }

  @Override
  public Throwable fillInStackTrace() {
    return this;
  }
}

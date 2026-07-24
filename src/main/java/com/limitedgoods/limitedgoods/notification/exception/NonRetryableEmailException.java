package com.limitedgoods.limitedgoods.notification.exception;

public class NonRetryableEmailException
        extends RuntimeException {

  public NonRetryableEmailException(
          String message,
          Throwable cause
  ) {
    super(message, cause);
  }
}
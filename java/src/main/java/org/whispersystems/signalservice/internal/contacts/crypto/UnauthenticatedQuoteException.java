package org.whispersystems.signalservice.internal.contacts.crypto;


public class UnauthenticatedQuoteException extends Throwable {
  public UnauthenticatedQuoteException(String s) {
    super(s);
  }

  public UnauthenticatedQuoteException(Exception nested) {
    super(nested);
  }
}

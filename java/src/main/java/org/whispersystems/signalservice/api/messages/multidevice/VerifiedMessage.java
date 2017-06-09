package org.whispersystems.signalservice.api.messages.multidevice;


import org.whispersystems.libsignal.IdentityKey;

public class VerifiedMessage {

  public enum VerifiedState {
    DEFAULT, VERIFIED, UNVERIFIED
  }

  private final String      destination;
  private final IdentityKey identityKey;
  private final VerifiedState verified;

  public VerifiedMessage(String destination, IdentityKey identityKey, VerifiedState verified) {
    this.destination = destination;
    this.identityKey = identityKey;
    this.verified    = verified;
  }


  public String getDestination() {
    return destination;
  }

  public IdentityKey getIdentityKey() {
    return identityKey;
  }

  public VerifiedState getVerified() {
    return verified;
  }
}

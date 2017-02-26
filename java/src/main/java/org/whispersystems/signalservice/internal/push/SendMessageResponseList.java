package org.whispersystems.signalservice.internal.push;


import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.push.exceptions.NetworkFailureException;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.util.LinkedList;
import java.util.List;

public class SendMessageResponseList {

  private final List<UntrustedIdentityException> untrustedIdentities = new LinkedList<>();
  private final List<UnregisteredUserException>  unregisteredUsers   = new LinkedList<>();
  private final List<NetworkFailureException>    networkExceptions   = new LinkedList<>();

  private boolean needsSync;

  public void addResponse(SendMessageResponse response) {
    if (!needsSync && response.getNeedsSync()) {
      needsSync = true;
    }
  }

  public void addException(UntrustedIdentityException exception) {
    untrustedIdentities.add(exception);
  }

  public void addException(UnregisteredUserException exception) {
    unregisteredUsers.add(exception);
  }

  public void addException(NetworkFailureException exception) {
    networkExceptions.add(exception);
  }

  public boolean getNeedsSync() {
    return needsSync;
  }

  public boolean hasExceptions() {
    return !untrustedIdentities.isEmpty() || !unregisteredUsers.isEmpty() || !networkExceptions.isEmpty();
  }

  public List<UntrustedIdentityException> getUntrustedIdentities() {
    return untrustedIdentities;
  }

  public List<UnregisteredUserException> getUnregisteredUsers() {
    return unregisteredUsers;
  }

  public List<NetworkFailureException> getNetworkExceptions() {
    return networkExceptions;
  }



}

/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.util;

import org.whispersystems.signalservice.api.util.SignalThread;

public class JavaThread implements SignalThread {
  @Override
  public void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}

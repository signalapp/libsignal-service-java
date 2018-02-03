/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.util;

import org.whispersystems.signalservice.internal.util.Util;

public class SignalThread {

    private static boolean active = false;

    public static synchronized void onTrigger() {
        active = true;
        SignalThread.class.notifyAll();
    }

    public static synchronized void sleep(long millis) {
        Util.wait(SignalThread.class, active ? 0 : millis);
    }

}
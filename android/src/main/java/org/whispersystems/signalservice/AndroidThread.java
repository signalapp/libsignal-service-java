/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;

import org.whispersystems.signalservice.api.util.SignalThread;

public class AndroidThread extends BroadcastReceiver implements SignalThread {

  public static final String SLEEP_ACTION = "org.whispersystems.signalservice.AndroidThread.SLEEP";

  private AlarmManager alarmManager;
  private PendingIntent pendingIntent;

  public AndroidThread(Context context) {
    this.pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(SLEEP_ACTION), 0);
    alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    context.registerReceiver(this,
            new IntentFilter(SLEEP_ACTION));

  }

  @Override
  public void sleep(long millis) throws InterruptedException {
    long wakeupTime = SystemClock.elapsedRealtime() + millis;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      this.alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeupTime, pendingIntent);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      this.alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeupTime, pendingIntent);
    } else {
      this.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeupTime, pendingIntent);
    }
    synchronized (this) {
      while (SystemClock.elapsedRealtime() < wakeupTime) {
        this.wait();
      }
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      synchronized (this) {
        this.notifyAll();
      }
    } catch (Exception e) {
      // nothing to do
    }
  }
}
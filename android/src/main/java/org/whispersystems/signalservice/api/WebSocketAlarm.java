package org.whispersystems.signalservice.api;

import android.util.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.SystemClock;

import org.whispersystems.signalservice.internal.websocket.WebSocketConnection;

import java.util.concurrent.TimeUnit;

public class WebSocketAlarm {
  private static final String TAG = WebSocketAlarm.class.getSimpleName();

  private AlarmReceiver alarmReceiver;
  private Context context;

  public WebSocketAlarm(Context context) {
    this.context = context;

    alarmReceiver = new WebSocketAlarm.AlarmReceiver();
    alarmReceiver.setOrCancelAlarm(true);

    context.registerReceiver(alarmReceiver,
                             new IntentFilter(AlarmReceiver.WAKE_UP_THREADS_ACTION));
  }

  public void disable() {
    alarmReceiver.setOrCancelAlarm(false);
    context.unregisterReceiver(alarmReceiver);

    alarmReceiver = null;
    context = null;
  }

  private class AlarmReceiver extends BroadcastReceiver {
    private static final int    WAKE_UP_TIMEOUT_SECONDS = 60;
    private static final String WAKE_UP_THREADS_ACTION = "org.whispersystems.signalservice.api.WebSocketAlarm.AlarmReceiver.WAKE_UP_THREADS";

    private void setOrCancelAlarm(boolean set) {
      final Intent        intent        = new Intent(WAKE_UP_THREADS_ACTION);
      final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
      final AlarmManager  alarmManager  = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

      alarmManager.cancel(pendingIntent);

      if (set) {
        Log.w(TAG, "Setting repeating alarm to wake up the websocket.");

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                  SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(WAKE_UP_TIMEOUT_SECONDS),
                                  TimeUnit.SECONDS.toMillis(WAKE_UP_TIMEOUT_SECONDS),
                                  pendingIntent);
      } else {
        Log.w(TAG, "Canceling websocket wake-up alarm.");
      }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.w(TAG, "Waking up the websocket.");

      WebSocketConnection.Alarm.trigger();
    }
  }
}

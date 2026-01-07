package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

/**
 * Helper class for managing local notifications
 * Supports immediate notifications and scheduled notifications
 */
public class LocalNotificationHelper {
    private static final String TAG = "LocalNotificationHelper";
    private static final String CHANNEL_ID = "my_application_notifications";
    private static final String CHANNEL_NAME = "My Application Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for My Application";

    /**
     * Create notification channel for Android 8.0 (API level 26) and above
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }
    }

    /**
     * Display an immediate notification
     */
    public static void showNotification(Context context, String title, String message, Intent intent) {
        showNotification(context, title, message, intent, (int) System.currentTimeMillis());
    }

    /**
     * Display a notification with a custom notification ID
     */
    public static void showNotification(Context context, String title, String message, Intent intent, int notificationId) {
        createNotificationChannel(context);

        // Create pending intent
        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        // Default notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent);
        }

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "Notification displayed: " + notificationId);
        }
    }

    /**
     * Schedule a notification to be shown at a specific time
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param intent Intent to open when notification is clicked
     * @param delayInMillis Delay in milliseconds from now
     * @param notificationId Unique ID for the notification
     */
    public static void scheduleNotification(Context context, String title, String message, Intent intent, long delayInMillis, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Create intent for the notification receiver
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.putExtra("title", title);
        notificationIntent.putExtra("message", message);
        notificationIntent.putExtra("notificationId", notificationId);
        
        // Store the original intent as extra data
        if (intent != null) {
            notificationIntent.putExtra("targetActivity", intent.getComponent().getClassName());
            if (intent.getExtras() != null) {
                notificationIntent.putExtras(intent.getExtras());
            }
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        long triggerTime = System.currentTimeMillis() + delayInMillis;
        
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Notification scheduled for: " + triggerTime);
        }
    }

    /**
     * Schedule a daily notification at a specific time
     * @param context Application context
     * @param title Notification title
     * @param message Notification message
     * @param intent Intent to open when notification is clicked
     * @param hourOfDay Hour (0-23)
     * @param minute Minute (0-59)
     * @param notificationId Unique ID for the notification
     */
    public static void scheduleDailyNotification(Context context, String title, String message, Intent intent, int hourOfDay, int minute, int notificationId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        // If the time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.putExtra("title", title);
        notificationIntent.putExtra("message", message);
        notificationIntent.putExtra("notificationId", notificationId);
        notificationIntent.putExtra("isDaily", true);
        
        if (intent != null) {
            notificationIntent.putExtra("targetActivity", intent.getComponent().getClassName());
            if (intent.getExtras() != null) {
                notificationIntent.putExtras(intent.getExtras());
            }
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 
                        AlarmManager.INTERVAL_DAY, pendingIntent);
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 
                        AlarmManager.INTERVAL_DAY, pendingIntent);
            }
            Log.d(TAG, "Daily notification scheduled for: " + hourOfDay + ":" + minute);
        }
    }

    /**
     * Cancel a scheduled notification
     */
    public static void cancelNotification(Context context, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Notification cancelled: " + notificationId);
        }
        
        // Also cancel any displayed notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }

    /**
     * BroadcastReceiver to handle scheduled notifications
     */
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");
            int notificationId = intent.getIntExtra("notificationId", 0);
            boolean isDaily = intent.getBooleanExtra("isDaily", false);
            
            // Create intent to open the app
            Intent targetIntent = null;
            String targetActivity = intent.getStringExtra("targetActivity");
            if (targetActivity != null) {
                try {
                    targetIntent = new Intent(context, Class.forName(targetActivity));
                    if (intent.getExtras() != null) {
                        targetIntent.putExtras(intent.getExtras());
                    }
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "Target activity not found: " + targetActivity);
                    targetIntent = new Intent(context, ProductActivity.class);
                }
            } else {
                targetIntent = new Intent(context, ProductActivity.class);
            }
            
            // Show the notification
            showNotification(context, title, message, targetIntent, notificationId);
            
            // If it's a daily notification, reschedule for tomorrow
            if (isDaily) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                // The alarm manager will handle the repeat, but we can also manually reschedule if needed
            }
        }
    }
}


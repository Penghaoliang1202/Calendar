package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int NOTIFICATION_ID_BASE = 1000;

    public static void createNotificationChannel(Context context) {
        // Since minSdk is 27 (Android 8.1), notification channels are always available (API 26+)
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.enableVibration(true);
        channel.enableLights(true);

        // Set default notification sound
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        channel.setSound(soundUri, null);

        notificationManager.createNotificationChannel(channel);
    }

    public static void showReminderNotification(Context context, Reminder reminder) {
        if (context == null || reminder == null) {
            Log.e(TAG, "Context or Reminder is null");
            return;
        }

        try {
            createNotificationChannel(context);

            // Create intent to open MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            String title = reminder.getTitle();
            if (title == null || title.isEmpty()) {
                title = context.getString(R.string.app_name);
            }

            String content = reminder.getContent();
            if (content == null || content.isEmpty()) {
                content = context.getString(R.string.reminder_saved);
            }

            // Format reminder date and time
            String reminderDateTime = formatReminderDateTime(context, reminder);
            
            // Combine content with time information
            String notificationText;
            if (reminderDateTime != null && !reminderDateTime.isEmpty()) {
                notificationText = content + "\n" + reminderDateTime;
            } else {
                notificationText = content;
            }

            // Get notification sound
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(context.getString(R.string.notification_title, title))
                    .setContentText(notificationText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setSound(soundUri)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Use reminder ID hash to create unique notification ID
                int notificationId = NOTIFICATION_ID_BASE + Math.abs(reminder.getId().hashCode());
                notificationManager.notify(notificationId, builder.build());
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification", e);
        }
    }

    /**
     * Format reminder date and time for display in notification
     */
    private static String formatReminderDateTime(Context context, Reminder reminder) {
        if (reminder == null) {
            return null;
        }

        try {
            String dateStr = reminder.getDate();
            String startTimeStr = reminder.getStartTime();
            String endTimeStr = reminder.getEndTime();

            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }

            // Parse date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(dateStr);
            if (date == null) {
                return null;
            }

            // Format date for display
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String formattedDate = displayDateFormat.format(date);

            // Build time string
            StringBuilder timeBuilder = new StringBuilder();
            if (startTimeStr != null && !startTimeStr.isEmpty()) {
                timeBuilder.append(startTimeStr);
                if (endTimeStr != null && !endTimeStr.isEmpty() && !endTimeStr.equals(startTimeStr)) {
                    timeBuilder.append(" - ").append(endTimeStr);
                }
            }

            // Combine date and time
            if (timeBuilder.length() > 0) {
                return context.getString(R.string.notification_time_format, formattedDate, timeBuilder.toString());
            } else {
                return formattedDate;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to format reminder date/time", e);
            return null;
        }
    }

}


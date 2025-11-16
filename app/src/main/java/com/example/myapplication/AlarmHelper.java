package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    public static void setAlarm(Context context, Reminder reminder) {
        if (context == null || reminder == null) {
            return;
        }

        if (!reminder.isEnableNotification() || reminder.getStartTime() == null || reminder.getStartTime().isEmpty()) {
            Log.d(TAG, "Reminder notification is disabled or start time is empty");
            return;
        }

        try {
            // Parse reminder date and time
            String dateStr = reminder.getDate(); // Format: yyyy-MM-dd
            String timeStr = reminder.getStartTime(); // Format: HH:mm

            Log.d(TAG, "Setting alarm - Date: " + dateStr + ", Time: " + timeStr);

            if (dateStr == null || dateStr.isEmpty() || timeStr == null || timeStr.isEmpty()) {
                Log.e(TAG, "Reminder date or time is empty");
                return;
            }

            // Parse date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(dateStr);
            if (date == null) {
                Log.e(TAG, "Failed to parse date: " + dateStr);
                return;
            }

            // Parse time string (HH:mm)
            String[] timeParts = timeStr.split(":");
            if (timeParts.length != 2) {
                Log.e(TAG, "Invalid time format: " + timeStr);
                return;
            }

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Combine date and time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Log.d(TAG, "Reminder time (before subtracting): " + calendar.getTime());

            // Subtract notification minutes
            int minutesBefore = reminder.getNotificationMinutesBefore();
            calendar.add(Calendar.MINUTE, -minutesBefore);

            long alarmTime = calendar.getTimeInMillis();
            long currentTime = System.currentTimeMillis();
            long timeDifference = alarmTime - currentTime;

            Log.d(TAG, "Alarm will trigger at: " + calendar.getTime());
            Log.d(TAG, "Current time: " + new Date(currentTime));
            Log.d(TAG, "Time difference: " + (timeDifference / 1000) + " seconds (" + (timeDifference / 60000) + " minutes)");

            // Check if the alarm time is in the past
            if (alarmTime <= currentTime) {
                Log.w(TAG, "Alarm time is in the past, skipping. Alarm time: " + calendar.getTime() + ", Current: " + new Date(currentTime));
                return;
            }

            // Create intent for AlarmReceiver
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("reminder_id", reminder.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            // Check if AlarmManager can schedule exact alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms. User needs to grant permission in settings.");
                    // You might want to show a dialog here to guide user to settings
                    return;
                }
            }

            // Since minSdk is 27 (Android 8.1), setExactAndAllowWhileIdle is always available
            // This method is available from API 23 (Android 6.0)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Alarm set using setExactAndAllowWhileIdle");

            Log.d(TAG, "✓ Alarm successfully set for reminder: " + reminder.getId());
            Log.d(TAG, "  Reminder title: " + reminder.getTitle());
            Log.d(TAG, "  Will trigger at: " + calendar.getTime());
            Log.d(TAG, "  Notification minutes before: " + minutesBefore);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse reminder date/time", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set alarm", e);
        }
    }

    public static void cancelAlarm(Context context, Reminder reminder) {
        if (context == null || reminder == null) {
            return;
        }

        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("reminder_id", reminder.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Alarm cancelled for reminder: " + reminder.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel alarm", e);
        }
    }
}


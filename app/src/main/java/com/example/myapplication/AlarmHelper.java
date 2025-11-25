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

    /**
     * Set alarm for reminder
     * @return true if alarm was set successfully, false if alarm time is in the past
     */
    public static boolean setAlarm(Context context, Reminder reminder) {
        if (context == null || reminder == null) {
            return false;
        }

        if (!reminder.isEnableNotification() || reminder.getStartTime() == null || reminder.getStartTime().isEmpty()) {
            return false;
        }

        try {
            // Parse reminder date and time
            String dateStr = reminder.getDate(); // Format: yyyy-MM-dd
            String timeStr = reminder.getStartTime(); // Format: HH:mm

            if (dateStr == null || dateStr.isEmpty() || timeStr == null || timeStr.isEmpty()) {
                Log.e(TAG, "Reminder date or time is empty");
                return false;
            }

            // Parse date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(dateStr);
            if (date == null) {
                Log.e(TAG, "Failed to parse date: " + dateStr);
                return false;
            }

            // Parse time string (HH:mm)
            String[] timeParts = timeStr.split(":");
            if (timeParts.length != 2) {
                Log.e(TAG, "Invalid time format: " + timeStr);
                return false;
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

            // Subtract notification minutes
            int minutesBefore = reminder.getNotificationMinutesBefore();
            calendar.add(Calendar.MINUTE, -minutesBefore);

            long alarmTime = calendar.getTimeInMillis();
            long currentTime = System.currentTimeMillis();

            // Check if the alarm time is in the past
            if (alarmTime <= currentTime) {
                // Check if the reminder time itself is also in the past
                Calendar reminderTime = Calendar.getInstance();
                reminderTime.setTime(date);
                reminderTime.set(Calendar.HOUR_OF_DAY, hour);
                reminderTime.set(Calendar.MINUTE, minute);
                reminderTime.set(Calendar.SECOND, 0);
                reminderTime.set(Calendar.MILLISECOND, 0);
                
                if (reminderTime.getTimeInMillis() <= currentTime) {
                    // Reminder time is in the past, this is expected when restoring alarms
                    Log.d(TAG, "Reminder time is in the past, skipping alarm. Reminder time: " + reminderTime.getTime() + ", Current: " + new Date(currentTime));
                } else {
                    // Alarm time is in the past but reminder time is in the future
                    // This shouldn't happen if validation is correct, but log as warning
                    Log.w(TAG, "Alarm time is in the past but reminder time is in the future. Alarm time: " + calendar.getTime() + ", Reminder time: " + reminderTime.getTime() + ", Current: " + new Date(currentTime));
                }
                return false;
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
                return false;
            }

            // Check if AlarmManager can schedule exact alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarms. User needs to grant permission in settings.");
                    Log.e(TAG, "Alarm time: " + calendar.getTime() + ", Reminder ID: " + reminder.getId());
                    return false;
                }
            }

            // Since minSdk is 27 (Android 8.1), setExactAndAllowWhileIdle is always available
            // This method is available from API 23 (Android 6.0)
            // For repeating alarms, we use setExactAndAllowWhileIdle and reschedule in AlarmReceiver
            // This ensures better reliability on Android 8.0+
            try {
                // Always use setExactAndAllowWhileIdle for better reliability
                // For repeating reminders, we'll reschedule in AlarmReceiver
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                String repeatType = reminder.getRepeatType();
                if (repeatType != null && !repeatType.equals("NONE")) {
                    Log.d(TAG, "Repeating alarm set successfully. Reminder ID: " + reminder.getId() + ", Alarm time: " + calendar.getTime() + ", Repeat: " + repeatType);
                } else {
                    Log.d(TAG, "Alarm set successfully. Reminder ID: " + reminder.getId() + ", Alarm time: " + calendar.getTime());
                }
                return true;
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException when setting alarm: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Exception when setting alarm: " + e.getMessage(), e);
                return false;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse reminder date/time", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set alarm", e);
            return false;
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
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel alarm", e);
        }
    }

}


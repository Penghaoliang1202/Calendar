package com.example.myapplication;

import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.util.List;

/**
 * Debug helper class for checking reminder settings and permission status
 */
public class AlarmDebugHelper {
    private static final String TAG = "AlarmDebug";

    /**
     * Check all reminders status and print debug information
     */
    public static void debugAllReminders(Context context) {
        Log.d(TAG, "========== Reminder Debug Info ==========");
        
        ReminderManager reminderManager = new ReminderManager(context);
        List<Reminder> allReminders = reminderManager.getAllReminders();
        
        Log.d(TAG, "Total reminders: " + allReminders.size());
        
        int enabledCount = 0;
        for (Reminder reminder : allReminders) {
            if (reminder != null && reminder.isEnableNotification()) {
                enabledCount++;
                Log.d(TAG, "--- Reminder #" + enabledCount + " ---");
                Log.d(TAG, "ID: " + reminder.getId());
                Log.d(TAG, "Title: " + reminder.getTitle());
                Log.d(TAG, "Date: " + reminder.getDate());
                Log.d(TAG, "Start Time: " + reminder.getStartTime());
                Log.d(TAG, "Minutes Before: " + reminder.getNotificationMinutesBefore());
                Log.d(TAG, "Completed: " + reminder.isCompleted());
                Log.d(TAG, "Deleted: " + reminder.isDeleted());
            }
        }
        
        Log.d(TAG, "Reminders with notifications enabled: " + enabledCount);
        
        // Check permissions
        checkPermissions(context);
        
        // Check AlarmManager
        checkAlarmManager(context);
        
        Log.d(TAG, "========================================");
    }

    /**
     * Check permission status
     */
    public static void checkPermissions(Context context) {
        Log.d(TAG, "--- Permission Check ---");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int result = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS);
            boolean granted = result == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "POST_NOTIFICATIONS permission: " + (granted ? "✓ Granted" : "✗ Not granted"));
        }
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canSchedule = alarmManager.canScheduleExactAlarms();
            Log.d(TAG, "Can schedule exact alarms: " + (canSchedule ? "✓ Yes" : "✗ No (requires user authorization)"));
        }
    }

    /**
     * Check if AlarmManager is available
     */
    public static void checkAlarmManager(Context context) {
        Log.d(TAG, "--- AlarmManager Check ---");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "✗ AlarmManager not available");
        } else {
            Log.d(TAG, "✓ AlarmManager available");
        }
    }

    /**
     * Test alarm setup (set a test reminder that triggers in 1 minute)
     */
    public static void testAlarm(Context context) {
        Log.d(TAG, "========== Test Alarm ==========");
        
        // Create a test reminder (triggers in 1 minute)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.MINUTE, 1);
        
        Reminder testReminder = new Reminder(
            "test_" + System.currentTimeMillis(),
            new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()),
            "Test Reminder",
            "This is a test reminder that should trigger in 1 minute",
            new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.getTime()),
            "",
            System.currentTimeMillis()
        );
        testReminder.setEnableNotification(true);
        testReminder.setNotificationMinutesBefore(0); // Immediate reminder
        
        Log.d(TAG, "Setting test reminder, will trigger at " + calendar.getTime());
        AlarmHelper.setAlarm(context, testReminder);
        Log.d(TAG, "=================================");
    }
}

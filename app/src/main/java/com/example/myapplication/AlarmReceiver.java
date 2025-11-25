package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String reminderId = intent.getStringExtra("reminder_id");
        if (reminderId == null || reminderId.isEmpty()) {
            Log.e(TAG, "Reminder ID is missing");
            return;
        }

        // Load reminder from storage
        ReminderManager reminderManager = new ReminderManager(context);
        Reminder reminder = null;
        for (Reminder r : reminderManager.getAllReminders()) {
            if (r != null && reminderId.equals(r.getId())) {
                reminder = r;
                break;
            }
        }

        if (reminder == null) {
            Log.e(TAG, "Reminder not found: " + reminderId);
            return;
        }

        // Check if reminder is still active
        if (reminder.isCompleted() || reminder.isDeleted()) {
            Log.d(TAG, "Reminder is completed or deleted, skipping notification. Reminder ID: " + reminderId);
            return;
        }

        // Check if notification is enabled
        if (!reminder.isEnableNotification()) {
            Log.d(TAG, "Notification is disabled for reminder. Reminder ID: " + reminderId);
            return;
        }

        Log.d(TAG, "Showing notification for reminder. Reminder ID: " + reminderId + ", Title: " + reminder.getTitle());
        // Show notification
        NotificationHelper.showReminderNotification(context, reminder);
        
        // If this is a repeating reminder, schedule the next occurrence
        String repeatType = reminder.getRepeatType();
        if (repeatType != null && !repeatType.equals("NONE") && reminder.isEnableNotification() && 
            reminder.getStartTime() != null && !reminder.getStartTime().isEmpty()) {
            scheduleNextRepeat(context, reminder, repeatType);
        }
    }
    
    /**
     * Schedule the next occurrence of a repeating reminder
     */
    private void scheduleNextRepeat(Context context, Reminder reminder, String repeatType) {
        try {
            // Use current time as base to calculate next occurrence
            // This ensures we schedule from the actual trigger time, not the original date
            java.util.Calendar nextCalendar = java.util.Calendar.getInstance();
            
            // Parse the original reminder time to preserve the time
            String startTime = reminder.getStartTime();
            if (startTime == null || startTime.isEmpty()) {
                Log.e(TAG, "Start time is empty for repeating reminder. Reminder ID: " + reminder.getId());
                return;
            }
            
            String[] timeParts = startTime.split(":");
            if (timeParts.length != 2) {
                Log.e(TAG, "Invalid time format: " + startTime);
                return;
            }
            
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            // Set the time for next occurrence
            nextCalendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
            nextCalendar.set(java.util.Calendar.MINUTE, minute);
            nextCalendar.set(java.util.Calendar.SECOND, 0);
            nextCalendar.set(java.util.Calendar.MILLISECOND, 0);
            
            // Add the repeat interval
            switch (repeatType) {
                case "DAILY":
                    nextCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                    break;
                case "WEEKLY":
                    nextCalendar.add(java.util.Calendar.WEEK_OF_YEAR, 1);
                    break;
                case "MONTHLY":
                    nextCalendar.add(java.util.Calendar.MONTH, 1);
                    break;
                case "YEARLY":
                    nextCalendar.add(java.util.Calendar.YEAR, 1);
                    break;
                default:
                    Log.w(TAG, "Unknown repeat type: " + repeatType);
                    return;
            }
            
            // Format next date
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String nextDate = dateFormat.format(nextCalendar.getTime());
            
            // Update reminder date to next occurrence
            reminder.setDate(nextDate);
            reminder.setTimestamp(System.currentTimeMillis());
            
            // Save updated reminder
            ReminderManager reminderManager = new ReminderManager(context);
            reminderManager.saveReminder(reminder);
            
            // Schedule next alarm
            boolean alarmSet = AlarmHelper.setAlarm(context, reminder);
            if (alarmSet) {
                Log.d(TAG, "Next repeat scheduled successfully. Reminder ID: " + reminder.getId() + ", Next date: " + nextDate + ", Next time: " + startTime);
            } else {
                Log.w(TAG, "Failed to schedule next repeat. Reminder ID: " + reminder.getId() + ", Next date: " + nextDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule next repeat", e);
        }
    }
}


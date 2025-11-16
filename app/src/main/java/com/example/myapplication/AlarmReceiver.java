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
        if (intent == null || context == null) {
            Log.e(TAG, "Intent or Context is null");
            return;
        }

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
            return;
        }

        // Show notification
        NotificationHelper.showReminderNotification(context, reminder);
    }
}


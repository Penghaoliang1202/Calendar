package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderManager {
    private static final String TAG = "ReminderManager";
    private static final String PREFS_NAME = "reminders_prefs";
    private static final String REMINDERS_KEY = "reminders_list";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    // Cache SimpleDateFormat to avoid repeated creation
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ReminderManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveReminder(Reminder reminder) {
        if (reminder == null || reminder.getId() == null) {
            return;
        }
        List<Reminder> reminders = getAllReminders();
        String reminderId = reminder.getId();
        // Optimize: use size() once and check for null before equals
        int size = reminders.size();
        for (int i = 0; i < size; i++) {
            Reminder existingReminder = reminders.get(i);
            if (existingReminder != null && reminderId.equals(existingReminder.getId())) {
                reminders.set(i, reminder);
                saveReminders(reminders);
                return;
            }
        }
        reminders.add(reminder);
        saveReminders(reminders);
    }

    public void deleteReminder(String reminderId) {
        if (reminderId == null) {
            return;
        }
        List<Reminder> reminders = getAllReminders();
        for (Reminder reminder : reminders) {
            if (reminder != null && reminderId.equals(reminder.getId())) {
                reminder.setDeleted(true);
                reminder.setTimestamp(System.currentTimeMillis());
                saveReminders(reminders);
                return;
            }
        }
    }

    public List<Reminder> getAllReminders() {
        if (sharedPreferences == null || gson == null) {
            return new ArrayList<>();
        }
        String json = sharedPreferences.getString(REMINDERS_KEY, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<Reminder>>() {}.getType();
            List<Reminder> reminders = gson.fromJson(json, type);
            return reminders != null ? reminders : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse reminders from JSON", e);
            return new ArrayList<>();
        }
    }

    public void markAsCompleted(String reminderId) {
        if (reminderId == null) {
            return;
        }
        List<Reminder> reminders = getAllReminders();
        for (Reminder reminder : reminders) {
            if (reminder != null && reminderId.equals(reminder.getId())) {
                reminder.setCompleted(true);
                reminder.setTimestamp(System.currentTimeMillis());
                saveReminders(reminders);
                return;
            }
        }
    }

    public void restoreReminder(String reminderId) {
        if (reminderId == null) {
            return;
        }
        List<Reminder> reminders = getAllReminders();
        for (Reminder reminder : reminders) {
            if (reminder != null && reminderId.equals(reminder.getId())) {
                reminder.setCompleted(false);
                reminder.setDeleted(false);
                reminder.setTimestamp(System.currentTimeMillis());
                saveReminders(reminders);
                return;
            }
        }
    }

    public List<Reminder> getActiveReminders() {
        List<Reminder> allReminders = getAllReminders();
        List<Reminder> activeReminders = new ArrayList<>();
        // Use cached DATE_FORMAT instead of creating new SimpleDateFormat
        String today = DATE_FORMAT.format(new Date());
        
        for (Reminder reminder : allReminders) {
            if (reminder != null && !reminder.isCompleted() && !reminder.isDeleted()) {
                activeReminders.add(reminder);
            }
        }
        
        activeReminders.sort((r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            
            String date1 = r1.getDate();
            String date2 = r2.getDate();
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            
            boolean r1IsToday = date1.equals(today);
            boolean r2IsToday = date2.equals(today);
            
            if (r1IsToday && !r2IsToday) return -1;
            if (!r1IsToday && r2IsToday) return 1;
            return date1.compareTo(date2);
        });
        return activeReminders;
    }

    public List<Reminder> getCompletedReminders() {
        List<Reminder> allReminders = getAllReminders();
        List<Reminder> completedReminders = new ArrayList<>();
        for (Reminder reminder : allReminders) {
            if (reminder != null && (reminder.isCompleted() || reminder.isDeleted())) {
                completedReminders.add(reminder);
            }
        }
        completedReminders.sort((r1, r2) -> {
            if (r1 == null && r2 == null) return 0;
            if (r1 == null) return 1;
            if (r2 == null) return -1;
            return Long.compare(r2.getTimestamp(), r1.getTimestamp());
        });
        return completedReminders;
    }

    public void clearAllCompletedReminders() {
        List<Reminder> allReminders = getAllReminders();
        List<Reminder> activeReminders = new ArrayList<>();
        for (Reminder reminder : allReminders) {
            if (reminder != null && !reminder.isCompleted() && !reminder.isDeleted()) {
                activeReminders.add(reminder);
            }
        }
        saveReminders(activeReminders);
    }

    private void saveReminders(List<Reminder> reminders) {
        if (sharedPreferences == null || gson == null) {
            return;
        }
        try {
            String json = gson.toJson(reminders);
            sharedPreferences.edit().putString(REMINDERS_KEY, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save reminders", e);
        }
    }
}


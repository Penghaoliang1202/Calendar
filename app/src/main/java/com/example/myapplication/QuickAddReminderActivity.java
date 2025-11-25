package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class QuickAddReminderActivity extends AppCompatActivity {
    private static final String TAG = "QuickAddReminderActivity";
    private ReminderManager reminderManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make window transparent to show system desktop/wallpaper
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Set flags to show wallpaper/desktop
            android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
            params.flags |= android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            params.flags |= android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            getWindow().setAttributes(params);
        }
        
        try {
            // Create notification channel for Android O+
            NotificationHelper.createNotificationChannel(this);
            
            // Request notification permission (Android 13+)
            requestNotificationPermission();
            
            reminderManager = new ReminderManager(this);
            Date today = new Date();
            selectedDate = dateFormat.format(today);
            
            showQuickAddDialog();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to initialize QuickAddReminderActivity", e);
            Toast.makeText(this, "Failed to open add reminder dialog", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showQuickAddDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_quick_add_reminder, null);
            if (dialogView == null) {
                Toast.makeText(this, "Failed to load dialog layout", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            TextInputEditText titleEdit = dialogView.findViewById(R.id.quickTitleEdit);
            TextInputEditText dateEdit = dialogView.findViewById(R.id.quickDateEdit);
            TextInputEditText startTimeEdit = dialogView.findViewById(R.id.quickStartTimeEdit);
            TextInputEditText endTimeEdit = dialogView.findViewById(R.id.quickEndTimeEdit);
            SwitchMaterial notificationSwitch = dialogView.findViewById(R.id.quickNotificationSwitch);
            TextView notificationHintText = dialogView.findViewById(R.id.notificationHintText);
            
            if (titleEdit == null || dateEdit == null || startTimeEdit == null || endTimeEdit == null || notificationSwitch == null) {
                Toast.makeText(this, "Failed to initialize dialog views", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Set default date to today
            try {
                Date date = dateFormat.parse(selectedDate);
                if (date != null) {
                    dateEdit.setText(displayDateFormat.format(date));
                } else {
                    dateEdit.setText(selectedDate);
                }
            } catch (Exception e) {
                dateEdit.setText(selectedDate);
            }
            dateEdit.setOnClickListener(v -> showDatePicker(dateEdit));

            // Set default time to current time + 1 hour
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            String defaultStartTime = String.format(Locale.getDefault(), "%02d:%02d", 
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            String defaultEndTime = String.format(Locale.getDefault(), "%02d:%02d", 
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

            startTimeEdit.setText(defaultStartTime);
            startTimeEdit.setOnClickListener(v -> showTimePicker(startTimeEdit));
            
            endTimeEdit.setText(defaultEndTime);
            endTimeEdit.setOnClickListener(v -> showTimePicker(endTimeEdit));

            // Default notification on for quick add
            notificationSwitch.setChecked(true);
            
            // Show/hide notification hint based on switch state
            if (notificationHintText != null) {
                notificationHintText.setVisibility(notificationSwitch.isChecked() ? android.view.View.VISIBLE : android.view.View.GONE);
                notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
                    notificationHintText.setVisibility(isChecked ? View.VISIBLE : View.GONE));
            }

            // Auto focus on title edit and show keyboard
            titleEdit.requestFocus();
            titleEdit.postDelayed(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && titleEdit.hasFocus()) {
                        imm.showSoftInput(titleEdit, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception e) {
                    android.util.Log.w(TAG, "Failed to show keyboard", e);
                }
            }, 100);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.QuickAddDialogTheme)
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.save), null)
                    .setNegativeButton(getString(R.string.cancel), (dialog, which) -> finish());

            AlertDialog dialog = builder.create();
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
                // Make dialog window background transparent
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            
            dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            android.widget.Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            
            // Set button colors to ensure visibility
            if (positiveButton != null) {
                positiveButton.setTextColor(0xFF6200EE); // Material Design primary color
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(0xFF6200EE); // Material Design primary color
            }
            
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    String title = titleEdit.getText() != null ? titleEdit.getText().toString().trim() : "";
                    String startTime = startTimeEdit.getText() != null ? startTimeEdit.getText().toString().trim() : "";
                    String endTime = endTimeEdit.getText() != null ? endTimeEdit.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(QuickAddReminderActivity.this, 
                            getString(R.string.please_enter_title_or_content), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Validate selected date and time combination is not in the past
                    try {
                        Date selectedDateObj = dateFormat.parse(selectedDate);
                        if (selectedDateObj == null) {
                            android.util.Log.e(TAG, "Failed to parse selected date: " + selectedDate);
                            Toast.makeText(QuickAddReminderActivity.this, 
                                getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.setTime(selectedDateObj);
                        
                        // If start time is provided, validate the full date+time combination
                        if (!startTime.isEmpty()) {
                            try {
                                String[] timeParts = startTime.split(":");
                                if (timeParts.length == 2) {
                                    int hour = Integer.parseInt(timeParts[0]);
                                    int minute = Integer.parseInt(timeParts[1]);
                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
                                    selectedCalendar.set(Calendar.MINUTE, minute);
                                    selectedCalendar.set(Calendar.SECOND, 0);
                                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                                } else {
                                    android.util.Log.w(TAG, "Invalid time format: " + startTime);
                                    Toast.makeText(QuickAddReminderActivity.this, 
                                        getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (Exception e) {
                                android.util.Log.e(TAG, "Failed to parse time: " + startTime, e);
                                Toast.makeText(QuickAddReminderActivity.this, 
                                    getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            // No time provided, validate date only
                            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                            selectedCalendar.set(Calendar.MINUTE, 0);
                            selectedCalendar.set(Calendar.SECOND, 0);
                            selectedCalendar.set(Calendar.MILLISECOND, 0);
                        }
                        
                        Calendar now = Calendar.getInstance();
                        // Reset seconds and milliseconds to 0 for accurate comparison
                        now.set(Calendar.SECOND, 0);
                        now.set(Calendar.MILLISECOND, 0);
                        
                        // Check if selected date+time is in the past
                        // Also consider notificationMinutesBefore (default 5 minutes)
                        // The alarm will be set at (selectedTime - notificationMinutesBefore)
                        // So we need to ensure that time is still in the future
                        int notificationMinutesBefore = 5; // Default notification time
                        Calendar alarmCalendar = (Calendar) selectedCalendar.clone();
                        alarmCalendar.add(Calendar.MINUTE, -notificationMinutesBefore);
                        
                        long selectedTime = selectedCalendar.getTimeInMillis();
                        long alarmTime = alarmCalendar.getTimeInMillis();
                        long currentTime = now.getTimeInMillis();
                        
                        android.util.Log.d(TAG, "Validating time - Selected: " + selectedCalendar.getTime() + 
                            " (" + selectedTime + "), Alarm: " + alarmCalendar.getTime() + 
                            " (" + alarmTime + "), Current: " + now.getTime() + " (" + currentTime + 
                            "), Diff: " + (alarmTime - currentTime) + " ms");
                        
                        // Check if alarm time (selectedTime - notificationMinutesBefore) is in the past
                        // Only block if alarm time is clearly in the past (at least 1 second before now)
                        if (alarmTime < currentTime - 1000) {
                            android.util.Log.w(TAG, "Alarm time (with notification offset) is in the past, blocking save");
                            Toast.makeText(QuickAddReminderActivity.this, 
                                getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        android.util.Log.d(TAG, "Time validation passed, allowing save");
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Failed to validate date", e);
                        Toast.makeText(QuickAddReminderActivity.this, 
                            getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Get notification settings (default 5 minutes before)
                    // notificationSwitch is guaranteed to be non-null here (checked at line 86)
                    // User can toggle the switch before saving, so this value is not always true
                    @SuppressWarnings("ConstantConditions")
                    boolean enableNotification = notificationSwitch.isChecked();
                    int notificationMinutesBefore = 5; // Default

                    Reminder reminder = new Reminder(
                        UUID.randomUUID().toString(),
                        selectedDate,
                        title,
                        "", // No content for quick add
                        startTime,
                        endTime,
                        System.currentTimeMillis()
                    );
                    reminder.setEnableNotification(enableNotification);
                    reminder.setNotificationMinutesBefore(notificationMinutesBefore);
                    
                    reminderManager.saveReminder(reminder);
                    
                    // Set alarm if notification is enabled
                    if (enableNotification && !startTime.isEmpty()) {
                        android.util.Log.d(TAG, "Setting alarm for quick add reminder. Reminder ID: " + reminder.getId() + ", Date: " + selectedDate + ", Time: " + startTime + ", Notification enabled: " + enableNotification);
                        
                        // Ensure notification channel is created
                        NotificationHelper.createNotificationChannel(QuickAddReminderActivity.this);
                        
                        boolean alarmSet = AlarmHelper.setAlarm(QuickAddReminderActivity.this, reminder);
                        if (alarmSet) {
                            android.util.Log.d(TAG, "Alarm set successfully for quick add reminder. Reminder ID: " + reminder.getId());
                            Toast.makeText(QuickAddReminderActivity.this, 
                                getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.w(TAG, "Failed to set alarm for quick add reminder. Reminder ID: " + reminder.getId());
                            // Check if it's a permission issue
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
                                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                                    // Show dialog to guide user to settings
                                    showExactAlarmPermissionDialog();
                                } else {
                                    // Check notification permission
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(QuickAddReminderActivity.this, 
                                                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            Toast.makeText(QuickAddReminderActivity.this, 
                                                getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show();
                                            requestNotificationPermission();
                                        } else {
                                            String reminderDateTime = selectedDate + " " + startTime;
                                            Toast.makeText(QuickAddReminderActivity.this, 
                                                getString(R.string.reminder_time_past_detail, reminderDateTime), 
                                                Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        String reminderDateTime = selectedDate + " " + startTime;
                                        Toast.makeText(QuickAddReminderActivity.this, 
                                            getString(R.string.reminder_time_past_detail, reminderDateTime), 
                                            Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                String reminderDateTime = selectedDate + " " + startTime;
                                Toast.makeText(QuickAddReminderActivity.this, 
                                    getString(R.string.reminder_time_past_detail, reminderDateTime), 
                                    Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        // Notification is disabled or no start time
                        if (!enableNotification) {
                            android.util.Log.d(TAG, "Notification is disabled for quick add reminder");
                        }
                        if (startTime.isEmpty()) {
                            android.util.Log.d(TAG, "Start time is empty for quick add reminder");
                        }
                    }
                    
                    // Update Widget
                    updateWidgets();
                    
                    hideKeyboard(dialogView);
                    // Toast is already shown when alarm is set successfully, only show if notification is disabled or start time is empty
                    if (!enableNotification || startTime.isEmpty()) {
                        Toast.makeText(QuickAddReminderActivity.this, 
                            getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                    finish();
                });
            }
            });

            dialog.setOnDismissListener(dialogInterface -> {
                if (!isFinishing() && !isDestroyed()) {
                    hideKeyboard(dialogView);
                }
                finish();
            });
            
            dialog.show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to show quick add dialog", e);
            Toast.makeText(this, "Failed to show add reminder dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showDatePicker(TextInputEditText dateEdit) {
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (Exception e) {
            // Use current date
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long minDate = today.getTimeInMillis();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year1, month1, dayOfMonth, 0, 0, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);
            
            // Get today's date at midnight for comparison
            Calendar todayForComparison = Calendar.getInstance();
            todayForComparison.set(Calendar.HOUR_OF_DAY, 0);
            todayForComparison.set(Calendar.MINUTE, 0);
            todayForComparison.set(Calendar.SECOND, 0);
            todayForComparison.set(Calendar.MILLISECOND, 0);
            
            // Check if selected date is in the past
            if (selectedCalendar.before(todayForComparison)) {
                // Show error message and reset to today
                Toast.makeText(QuickAddReminderActivity.this, 
                    getString(R.string.cannot_select_past_dates), Toast.LENGTH_SHORT).show();
                selectedDate = dateFormat.format(todayForComparison.getTime());
                dateEdit.setText(displayDateFormat.format(todayForComparison.getTime()));
            } else {
                // Valid date selected
                selectedDate = dateFormat.format(selectedCalendar.getTime());
                dateEdit.setText(displayDateFormat.format(selectedCalendar.getTime()));
            }
        }, year, month, day);
        
        // Set minimum date to today (prevents selecting past dates)
        datePickerDialog.getDatePicker().setMinDate(minDate);
        // Also disable past dates by setting max date to prevent any workarounds
        datePickerDialog.getDatePicker().setMaxDate(Long.MAX_VALUE);
        datePickerDialog.show();
    }

    private void showTimePicker(TextInputEditText timeEdit) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        String currentTime = timeEdit.getText() != null ? timeEdit.getText().toString() : "";
        if (!currentTime.isEmpty()) {
            try {
                String[] parts = currentTime.split(":");
                if (parts.length == 2) {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                }
            } catch (Exception e) {
                // Use default values
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            // Validate if selected date is today, the time must not be in the past
            try {
                Date selectedDateObj = dateFormat.parse(selectedDate);
                if (selectedDateObj != null) {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.setTime(selectedDateObj);
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minuteOfHour);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                    
                    Calendar now = Calendar.getInstance();
                    // Reset seconds and milliseconds to 0 for accurate comparison
                    now.set(Calendar.SECOND, 0);
                    now.set(Calendar.MILLISECOND, 0);
                    
                    // Check if selected date+time is in the past (use <= to allow exact current time)
                    if (selectedCalendar.getTimeInMillis() <= now.getTimeInMillis()) {
                        Toast.makeText(QuickAddReminderActivity.this, 
                            getString(R.string.reminder_time_past_detail, 
                                selectedDate + " " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour)), 
                            Toast.LENGTH_SHORT).show();
                        // Reset to current time + 1 hour
                        Calendar futureTime = Calendar.getInstance();
                        futureTime.add(Calendar.HOUR_OF_DAY, 1);
                        String validTime = String.format(Locale.getDefault(), "%02d:%02d", 
                            futureTime.get(Calendar.HOUR_OF_DAY), futureTime.get(Calendar.MINUTE));
                        timeEdit.setText(validTime);
                        return;
                    }
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to validate time", e);
            }
            
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            timeEdit.setText(time);
        }, hour, minute, true);

        timePickerDialog.show();
    }
    private void hideKeyboard(View view) {
        if (view == null || isFinishing() || isDestroyed()) {
            return;
        }
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                android.view.Window window = getWindow();
                if (window != null) {
                    View currentFocus = window.getCurrentFocus();
                    if (currentFocus != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    } else if (view.getWindowToken() != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "Failed to hide keyboard", e);
        }
    }

    private void updateWidgets() {
        Intent intent = new Intent(this, ReminderWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
            new ComponentName(this, ReminderWidgetProvider.class)
        );
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }

    private void showExactAlarmPermissionDialog() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                    .setTitle(getString(R.string.exact_alarm_permission_required))
                    .setMessage(getString(R.string.exact_alarm_permission_required))
                    .setPositiveButton(getString(R.string.open_settings), (dialog, which) -> {
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Failed to open exact alarm settings", e);
                            Toast.makeText(this, getString(R.string.alarm_set_failed), Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }
}


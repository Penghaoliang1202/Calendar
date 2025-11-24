package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
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
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;
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
            reminderManager = new ReminderManager(this);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            displayDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
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

            // Default notification off
            notificationSwitch.setChecked(false);

            // Auto focus on title edit and show keyboard
            titleEdit.requestFocus();
            titleEdit.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(titleEdit, InputMethodManager.SHOW_IMPLICIT);
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
                        if (selectedDateObj != null) {
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
                                    }
                                } catch (Exception e) {
                                    // Invalid time format, just validate date
                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                    selectedCalendar.set(Calendar.MINUTE, 0);
                                    selectedCalendar.set(Calendar.SECOND, 0);
                                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                                }
                            } else {
                                // No time provided, validate date only
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                selectedCalendar.set(Calendar.MINUTE, 0);
                                selectedCalendar.set(Calendar.SECOND, 0);
                                selectedCalendar.set(Calendar.MILLISECOND, 0);
                            }
                            
                            Calendar now = Calendar.getInstance();
                            
                            // Check if selected date+time is in the past
                            if (selectedCalendar.before(now)) {
                                Toast.makeText(QuickAddReminderActivity.this, 
                                    getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                                // Reset to today
                                Calendar today = Calendar.getInstance();
                                today.set(Calendar.HOUR_OF_DAY, 0);
                                today.set(Calendar.MINUTE, 0);
                                today.set(Calendar.SECOND, 0);
                                today.set(Calendar.MILLISECOND, 0);
                                selectedDate = dateFormat.format(today.getTime());
                                dateEdit.setText(displayDateFormat.format(today.getTime()));
                                return;
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Failed to validate date", e);
                        Toast.makeText(QuickAddReminderActivity.this, 
                            getString(R.string.cannot_add_reminders_past_dates), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get notification settings (default 5 minutes before)
                    boolean enableNotification = notificationSwitch != null && notificationSwitch.isChecked();
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
                        boolean alarmSet = AlarmHelper.setAlarm(QuickAddReminderActivity.this, reminder);
                        if (!alarmSet) {
                            String reminderDateTime = selectedDate + " " + startTime;
                            Toast.makeText(QuickAddReminderActivity.this, 
                                getString(R.string.reminder_time_past_detail, reminderDateTime), 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    // Update Widget
                    updateWidgets();
                    
                    hideKeyboard(dialogView);
                    Toast.makeText(QuickAddReminderActivity.this, 
                        getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    finish();
                });
            }
            });

            dialog.setOnDismissListener(dialogInterface -> {
                hideKeyboard(dialogView);
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
                    
                    // Check if selected date+time is in the past
                    if (selectedCalendar.before(now)) {
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
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
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
}


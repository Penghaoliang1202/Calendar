package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private CalendarView calendarView;
    private TextView selectedDateText;
    private TextView currentDateTimeDisplay;
    private TextView reminderIndicatorText;
    private TextView yearText;
    private TextView monthText;
    private MaterialButton addReminderButton;
    private MaterialButton viewRemindersButton;
    private ReminderManager reminderManager;
    private String selectedDate;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat indicatorDateFormat;
    private SimpleDateFormat currentDateTimeFormat;
    private android.os.Handler timeHandler;
    private Runnable timeRunnable;
    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);
            
            View mainView = findViewById(R.id.main);
            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
            }

            initViews();
            initData();
            setupCalendar();
            setupListeners();
            updateReminderIndicator();
            
            // Request notification permission (Android 13+)
            requestNotificationPermission();
            
            // Create notification channel for Android O+
            NotificationHelper.createNotificationChannel(this);
            
            // Restore all alarms for active reminders with notifications enabled
            restoreAllAlarms();
            
            // Debug: Print all reminders info
            AlarmDebugHelper.debugAllReminders(this);
        } catch (Exception e) {
            Log.e(TAG, "App initialization failed", e);
            Toast.makeText(this, "App initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        currentDateTimeDisplay = findViewById(R.id.currentDateTimeDisplay);
        reminderIndicatorText = findViewById(R.id.reminderIndicatorText);
        yearText = findViewById(R.id.yearText);
        monthText = findViewById(R.id.monthText);
        addReminderButton = findViewById(R.id.addReminderButton);
        viewRemindersButton = findViewById(R.id.viewRemindersButton);
    }


    private void initData() {
        reminderManager = new ReminderManager(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()); // Display seconds
        displayDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        indicatorDateFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
        currentDateTimeFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        Date today = new Date();
        selectedDate = dateFormat.format(today);
        todayDate = selectedDate;
        updateSelectedDateText();
        
        // Highlight today's date
        if (calendarView != null) {
            highlightToday();
        }
        
        updateYearText();
        updateMonthText();
        startTimeUpdates();
    }


    private void setupCalendar() {
        if (calendarView != null) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            // Set current date as selected date (will be highlighted)
            calendarView.setDate(today.getTimeInMillis(), false, true);
            
            Calendar oneYearAgo = (Calendar) today.clone();
            oneYearAgo.add(Calendar.YEAR, -1);
            calendarView.setMinDate(oneYearAgo.getTimeInMillis());
            
            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                String selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                
                if (isDateInPast(selectedDateStr)) {
                    Toast.makeText(MainActivity.this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
                    calendarView.setDate(today.getTimeInMillis(), false, true);
                    selectedDate = dateFormat.format(today.getTime());
                } else {
                    selectedDate = selectedDateStr;
                }
                
                updateSelectedDateText();
                updateYearText();
                updateMonthText();
                updateReminderIndicator();
            });
            
            updateReminderIndicator();
        }
        
        if (yearText != null) {
            yearText.setOnClickListener(v -> showYearMonthPickerDialog());
        }
        if (monthText != null) {
            monthText.setOnClickListener(v -> showYearMonthPickerDialog());
        }
    }

    private void setupListeners() {
        if (addReminderButton != null) {
            addReminderButton.setOnClickListener(v -> {
                if (isDateInPast(selectedDate)) {
                    Toast.makeText(MainActivity.this, "Cannot add reminders for past dates", Toast.LENGTH_SHORT).show();
                } else {
                    showReminderDialog();
                }
            });
        }
        
        if (viewRemindersButton != null) {
            viewRemindersButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RemindersListActivity.class);
                startActivity(intent);
            });
        }
    }

    private boolean isDateInPast(String date) {
        if (date == null || todayDate == null) {
            return false;
        }
        return date.compareTo(todayDate) < 0;
    }

    private void updateSelectedDateText() {
        if (selectedDateText == null || selectedDate == null || displayDateFormat == null) {
            return;
        }
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                selectedDateText.setText(displayDateFormat.format(date));
            } else {
                selectedDateText.setText(selectedDate);
            }
        } catch (Exception e) {
            selectedDateText.setText(selectedDate);
        }
    }

    private void showReminderDialog() {
        showReminderDialogInternal(null);
    }

    private void showReminderDialogInternal(Reminder reminder) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText titleEdit = dialogView.findViewById(R.id.reminderTitleEdit);
        TextInputEditText contentEdit = dialogView.findViewById(R.id.reminderContentEdit);
        TextInputEditText startTimeEdit = dialogView.findViewById(R.id.startTimeEdit);
        TextInputEditText endTimeEdit = dialogView.findViewById(R.id.endTimeEdit);
        SwitchMaterial notificationSwitch = dialogView.findViewById(R.id.notificationSwitch);
        LinearLayout notificationTimeLayout = dialogView.findViewById(R.id.notificationTimeLayout);
        TextInputEditText notificationTimeEdit = dialogView.findViewById(R.id.notificationTimeEdit);

        final Reminder finalReminder = reminder;
        boolean isEdit = finalReminder != null;
        
        if (dialogTitle != null) {
            dialogTitle.setText(isEdit ? getString(R.string.edit_reminder) : getString(R.string.new_reminder));
        }
        
        if (isEdit) {
            titleEdit.setText(finalReminder.getTitle());
            contentEdit.setText(finalReminder.getContent());
            startTimeEdit.setText(finalReminder.getStartTime());
            endTimeEdit.setText(finalReminder.getEndTime());
            if (notificationSwitch != null) {
                notificationSwitch.setChecked(finalReminder.isEnableNotification());
            }
            if (notificationTimeEdit != null) {
                notificationTimeEdit.setText(String.valueOf(finalReminder.getNotificationMinutesBefore()));
            }
        } else {
            // Default values for new reminder
            if (notificationSwitch != null) {
                notificationSwitch.setChecked(false);
            }
            if (notificationTimeEdit != null) {
                notificationTimeEdit.setText("5");
            }
        }

        // Show/hide notification time layout based on switch state
        if (notificationSwitch != null && notificationTimeLayout != null) {
            notificationTimeLayout.setVisibility(notificationSwitch.isChecked() ? View.VISIBLE : View.GONE);
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                notificationTimeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        // Set up time picker
        startTimeEdit.setOnClickListener(v -> showTimePicker(startTimeEdit));
        endTimeEdit.setOnClickListener(v -> showTimePicker(endTimeEdit));

        // Set up notification time picker (show dialog with predefined options)
        if (notificationTimeEdit != null) {
            notificationTimeEdit.setOnClickListener(v -> showNotificationTimePicker(notificationTimeEdit));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), null)
                .setNegativeButton(getString(R.string.cancel), null);

        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    String title = titleEdit.getText() != null ? titleEdit.getText().toString().trim() : "";
                    String content = contentEdit.getText() != null ? contentEdit.getText().toString().trim() : "";
                    String startTime = startTimeEdit.getText() != null ? startTimeEdit.getText().toString().trim() : "";
                    String endTime = endTimeEdit.getText() != null ? endTimeEdit.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
                        Toast.makeText(MainActivity.this, getString(R.string.please_enter_title_or_content), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get notification settings
                    boolean enableNotification = notificationSwitch != null && notificationSwitch.isChecked();
                    int notificationMinutesBefore = 5; // Default
                    if (enableNotification && notificationTimeEdit != null) {
                        String minutesStr = notificationTimeEdit.getText() != null ? notificationTimeEdit.getText().toString().trim() : "";
                        if (!minutesStr.isEmpty()) {
                            try {
                                notificationMinutesBefore = Integer.parseInt(minutesStr);
                                if (notificationMinutesBefore < 0) {
                                    notificationMinutesBefore = 0;
                                } else if (notificationMinutesBefore > 1440) { // Max 24 hours
                                    notificationMinutesBefore = 1440;
                                }
                            } catch (NumberFormatException e) {
                                Log.d(TAG, "Invalid notification minutes, using default", e);
                                notificationMinutesBefore = 5;
                            }
                        }
                    }

                    Reminder reminderToSave;
                    if (isEdit) {
                        // Cancel old alarm if exists
                        if (finalReminder.isEnableNotification()) {
                            AlarmHelper.cancelAlarm(MainActivity.this, finalReminder);
                        }
                        
                        finalReminder.setTitle(title);
                        finalReminder.setContent(content);
                        finalReminder.setStartTime(startTime);
                        finalReminder.setEndTime(endTime);
                        finalReminder.setTimestamp(System.currentTimeMillis());
                        finalReminder.setEnableNotification(enableNotification);
                        finalReminder.setNotificationMinutesBefore(notificationMinutesBefore);
                        reminderToSave = finalReminder;
                    } else {
                        reminderToSave = new Reminder(UUID.randomUUID().toString(), selectedDate, title, content, startTime, endTime, System.currentTimeMillis());
                        reminderToSave.setEnableNotification(enableNotification);
                        reminderToSave.setNotificationMinutesBefore(notificationMinutesBefore);
                    }
                    
                    reminderManager.saveReminder(reminderToSave);
                    
                    // Set alarm if notification is enabled
                    if (enableNotification && !startTime.isEmpty()) {
                        AlarmHelper.setAlarm(MainActivity.this, reminderToSave);
                    }
                    
                    Toast.makeText(MainActivity.this, getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
                    updateReminderIndicator();
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
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
                Log.d(TAG, "Failed to parse time, using default values", e);
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            timeEdit.setText(time);
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void showNotificationTimePicker(TextInputEditText timeEdit) {
        String[] options = {"0", "5", "10", "15", "30", "60"};
        String[] displayOptions = {
            getString(R.string.at_time),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "5"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "10"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "15"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "30"),
            String.format(Locale.getDefault(), getString(R.string.minutes_before_hint), "60")
        };

        String currentValue = timeEdit.getText() != null ? timeEdit.getText().toString().trim() : "5";
        int selectedIndex = 1; // Default to 5 minutes
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.select_reminder_time))
                .setSingleChoiceItems(displayOptions, selectedIndex, (dialog, which) -> {
                    timeEdit.setText(options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void updateYearText() {
        if (yearText == null || selectedDate == null) {
            return;
        }
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                yearText.setText(String.valueOf(cal.get(Calendar.YEAR)));
            } else {
                if (selectedDate.length() >= 4) {
                    yearText.setText(selectedDate.substring(0, 4));
                } else {
                    Calendar cal = Calendar.getInstance();
                    yearText.setText(String.valueOf(cal.get(Calendar.YEAR)));
                }
            }
        } catch (Exception e) {
            Calendar cal = Calendar.getInstance();
            yearText.setText(String.valueOf(cal.get(Calendar.YEAR)));
        }
    }

    private void updateMonthText() {
        if (monthText == null || selectedDate == null) {
            return;
        }
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
                monthText.setText(monthFormat.format(cal.getTime()));
            }
        } catch (Exception e) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
            monthText.setText(monthFormat.format(cal.getTime()));
        }
    }

    private void showYearMonthPickerDialog() {
        if (calendarView == null || selectedDate == null) {
            return;
        }
        
        try {
            int currentYear;
            int currentMonth;
            Calendar cal = Calendar.getInstance();
            try {
                Date date = dateFormat.parse(selectedDate);
                if (date != null) {
                    cal.setTime(date);
                    currentYear = cal.get(Calendar.YEAR);
                    currentMonth = cal.get(Calendar.MONTH);
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                cal = Calendar.getInstance();
                currentYear = cal.get(Calendar.YEAR);
                currentMonth = cal.get(Calendar.MONTH);
            }
            
            Calendar today = Calendar.getInstance();
            int minYear = today.get(Calendar.YEAR);
            
            int maxYear = minYear + 100;
            String[] years = new String[maxYear - minYear + 1];
            for (int i = 0; i <= maxYear - minYear; i++) {
                years[i] = String.valueOf(minYear + i);
            }
            
            String[] months = new String[]{"January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};
            
            int selectedYearIndex = currentYear - minYear;
            if (selectedYearIndex < 0) selectedYearIndex = 0;
            if (selectedYearIndex >= years.length) selectedYearIndex = years.length - 1;
            
            int selectedMonthIndex = currentMonth;
            if (selectedMonthIndex == months.length) selectedMonthIndex = months.length - 1;
            
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_year_month_picker, null);
            
            android.widget.ListView yearListView = dialogView.findViewById(R.id.yearListView);
            android.widget.ListView monthListView = dialogView.findViewById(R.id.monthListView);
            
            android.widget.ArrayAdapter<String> yearAdapter = new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_single_choice, years);
            yearListView.setAdapter(yearAdapter);
            yearListView.setChoiceMode(android.widget.AbsListView.CHOICE_MODE_SINGLE);
            yearListView.setItemChecked(selectedYearIndex, true);
            yearListView.setSelection(selectedYearIndex);
            
            android.widget.ArrayAdapter<String> monthAdapter = new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_single_choice, months);
            monthListView.setAdapter(monthAdapter);
            monthListView.setChoiceMode(android.widget.AbsListView.CHOICE_MODE_SINGLE);
            monthListView.setItemChecked(selectedMonthIndex, true);
            monthListView.setSelection(selectedMonthIndex);
            
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme);
            builder.setTitle(getString(R.string.select_year_month));
            builder.setView(dialogView);
            builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                int selectedYearPos = yearListView.getCheckedItemPosition();
                int selectedMonthPos = monthListView.getCheckedItemPosition();
                
                if (selectedYearPos >= 0 && selectedMonthPos >= 0) {
                    int selectedYear = Integer.parseInt(years[selectedYearPos]);
                    
                    Calendar newCal = Calendar.getInstance();
                    try {
                        Date date = dateFormat.parse(selectedDate);
                        if (date != null) {
                            newCal.setTime(date);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Failed to parse selected date, using current date", e);
                    }
                    
                    newCal.set(Calendar.YEAR, selectedYear);
                    newCal.set(Calendar.MONTH, selectedMonthPos);
                    
                    int maxDay = newCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    int currentDay = newCal.get(Calendar.DAY_OF_MONTH);
                    if (currentDay > maxDay) {
                        newCal.set(Calendar.DAY_OF_MONTH, maxDay);
                    }
                    
                    Calendar todayCal = Calendar.getInstance();
                    todayCal.set(Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, 0);
                    todayCal.set(Calendar.MILLISECOND, 0);
                    
                    if (newCal.before(todayCal)) {
                        newCal.setTime(todayCal.getTime());
                    }
                    
                    calendarView.setDate(newCal.getTimeInMillis(), false, true);
                    selectedDate = dateFormat.format(newCal.getTime());
                    updateSelectedDateText();
                    updateYearText();
                    updateMonthText();
                    updateReminderIndicator();
                }
            });
            builder.setNegativeButton("Cancel", null);
            
            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to show year/month picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReminderIndicator() {
        if (reminderIndicatorText == null || reminderManager == null || indicatorDateFormat == null) {
            return;
        }
        
        try {
            List<Reminder> allReminders = reminderManager.getAllReminders();
            Set<String> datesWithReminders = new HashSet<>();
            
            for (Reminder reminder : allReminders) {
                if (reminder != null && !reminder.isCompleted() && !reminder.isDeleted()) {
                    String date = reminder.getDate();
                    if (date != null) {
                        datesWithReminders.add(date);
                    }
                }
            }
            
            // Update calendar reminder marks
            updateCalendarReminderMarks(datesWithReminders);
            
            if (datesWithReminders.isEmpty()) {
                reminderIndicatorText.setVisibility(View.GONE);
                return;
            }
            
            List<String> displayDates = new ArrayList<>();
            for (String dateStr : datesWithReminders) {
                try {
                    Date date = dateFormat.parse(dateStr);
                    if (date != null) {
                        displayDates.add(indicatorDateFormat.format(date));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Failed to format date: " + dateStr, e);
                }
            }
            
            if (!displayDates.isEmpty()) {
                displayDates.sort(String::compareTo);
                String datesList = String.join(", ", displayDates);
                reminderIndicatorText.setText(getString(R.string.dates_with_reminders, datesList));
                reminderIndicatorText.setVisibility(View.VISIBLE);
            } else {
                reminderIndicatorText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            reminderIndicatorText.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update calendar reminder marks
     * Note: CalendarView has limited customization, so we rely on text indicator below calendar
     */
    private void updateCalendarReminderMarks(Set<String> datesWithReminders) {
        // CalendarView doesn't support direct date marking
        // The reminder indicator text below the calendar shows dates with reminders
    }
    
    /**
     * Highlight today's date
     */
    private void highlightToday() {
        if (calendarView == null) {
            return;
        }
        
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            // Set current date as selected date
            // CalendarView will automatically highlight selected date
            calendarView.setDate(today.getTimeInMillis(), false, true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to highlight today", e);
        }
    }

    private void startTimeUpdates() {
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        
        timeHandler = new android.os.Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                if (currentDateTimeDisplay != null && currentDateTimeFormat != null && timeFormat != null) {
                    String dateStr = currentDateTimeFormat.format(now);
                    String timeStr = timeFormat.format(now);
                    currentDateTimeDisplay.setText(getString(R.string.date_time_format, dateStr, timeStr));
                }
                if (timeHandler != null) {
                    timeHandler.postDelayed(this, 1000);
                }
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                Toast.makeText(this, getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Notification permission denied");
                Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void restoreAllAlarms() {
        if (reminderManager == null) {
            return;
        }
        try {
            List<Reminder> allReminders = reminderManager.getAllReminders();
            int restoredCount = 0;
            for (Reminder reminder : allReminders) {
                if (reminder != null && !reminder.isCompleted() && !reminder.isDeleted() 
                        && reminder.isEnableNotification() && reminder.getStartTime() != null 
                        && !reminder.getStartTime().isEmpty()) {
                    AlarmHelper.setAlarm(this, reminder);
                    restoredCount++;
                }
            }
            Log.d(TAG, "Restored " + restoredCount + " alarms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore alarms", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}

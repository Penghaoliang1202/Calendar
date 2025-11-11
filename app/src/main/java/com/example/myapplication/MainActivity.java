package com.example.myapplication;

import android.app.TimePickerDialog;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        indicatorDateFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
        currentDateTimeFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
        Date today = new Date();
        selectedDate = dateFormat.format(today);
        todayDate = selectedDate;
        updateSelectedDateText();
        
        if (calendarView != null) {
            calendarView.setDate(today.getTime(), false, true);
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
        }

        // Set up time picker
        startTimeEdit.setOnClickListener(v -> showTimePicker(startTimeEdit));
        endTimeEdit.setOnClickListener(v -> showTimePicker(endTimeEdit));

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

                    Reminder reminderToSave;
                    if (isEdit) {
                        finalReminder.setTitle(title);
                        finalReminder.setContent(content);
                        finalReminder.setStartTime(startTime);
                        finalReminder.setEndTime(endTime);
                        finalReminder.setTimestamp(System.currentTimeMillis());
                        reminderToSave = finalReminder;
                    } else {
                        reminderToSave = new Reminder(UUID.randomUUID().toString(), selectedDate, title, content, startTime, endTime, System.currentTimeMillis());
                    }
                    reminderManager.saveReminder(reminderToSave);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}

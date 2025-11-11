package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RemindersListActivity extends AppCompatActivity {
    private static final String TAG = "RemindersListActivity";
    private RecyclerView remindersRecyclerView;
    private TextView emptyRemindersText;
    private MaterialButton historyButton;
    private MaterialButton clearHistoryButton;
    private ReminderManager reminderManager;
    private ReminderAdapter reminderAdapter;
    private boolean showingHistory = false;
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.all_reminders));
        }

        remindersRecyclerView = findViewById(R.id.remindersRecyclerView);
        emptyRemindersText = findViewById(R.id.emptyRemindersText);
        historyButton = findViewById(R.id.historyButton);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);

        reminderManager = new ReminderManager(this);
        reminderAdapter = new ReminderAdapter(new ArrayList<>(), new ReminderAdapter.OnReminderClickListener() {
            @Override
            public void onEditClick(Reminder reminder) {
                if (!reminder.isCompleted()) {
                    showReminderDialog(reminder);
                }
            }

            @Override
            public void onDeleteClick(Reminder reminder) {
                showDeleteConfirmDialog(reminder);
            }

            @Override
            public void onCompleteClick(Reminder reminder) {
                reminderManager.markAsCompleted(reminder.getId());
                loadReminders();
            }

            @Override
            public void onRestoreClick(Reminder reminder) {
                reminderManager.restoreReminder(reminder.getId());
                Toast.makeText(RemindersListActivity.this, getString(R.string.reminder_restored), Toast.LENGTH_SHORT).show();
                loadReminders();
            }
        });

        remindersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        remindersRecyclerView.setAdapter(reminderAdapter);

        if (historyButton != null) {
            historyButton.setOnClickListener(v -> toggleHistoryView());
        }

        if (clearHistoryButton != null) {
            clearHistoryButton.setOnClickListener(v -> showClearHistoryConfirmDialog());
        }

        loadReminders();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void loadReminders() {
        List<Reminder> reminders;
        if (showingHistory) {
            reminders = reminderManager.getCompletedReminders();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.history));
            }
            if (historyButton != null) {
                historyButton.setText(getString(R.string.view_all_reminders));
            }
            if (clearHistoryButton != null) {
                clearHistoryButton.setVisibility(reminders.isEmpty() ? View.GONE : View.VISIBLE);
            }
        } else {
            reminders = reminderManager.getActiveReminders();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.all_reminders));
            }
            if (historyButton != null) {
                historyButton.setText(getString(R.string.view_history));
            }
            if (clearHistoryButton != null) {
                clearHistoryButton.setVisibility(View.GONE);
            }
        }
        
        reminderAdapter.updateReminders(reminders);
        if (reminders.isEmpty()) {
            emptyRemindersText.setVisibility(View.VISIBLE);
            remindersRecyclerView.setVisibility(View.GONE);
            emptyRemindersText.setText(showingHistory ? getString(R.string.no_history_reminders) : getString(R.string.no_reminders));
        } else {
            emptyRemindersText.setVisibility(View.GONE);
            remindersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void toggleHistoryView() {
        showingHistory = !showingHistory;
        loadReminders();
    }

    private void showReminderDialog(Reminder reminder) {
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
                        Toast.makeText(RemindersListActivity.this, getString(R.string.please_enter_title_or_content), Toast.LENGTH_SHORT).show();
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
                        String date = DATE_FORMAT.format(new java.util.Date());
                        reminderToSave = new Reminder(UUID.randomUUID().toString(), date, title, content, startTime, endTime, System.currentTimeMillis());
                    }
                    reminderManager.saveReminder(reminderToSave);
                    loadReminders();
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

    private void showDeleteConfirmDialog(Reminder reminder) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    reminderManager.deleteReminder(reminder.getId());
                    loadReminders();
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showClearHistoryConfirmDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.clear_all_history))
                .setMessage(getString(R.string.clear_all_history_message))
                .setPositiveButton(getString(R.string.clear_all), (dialog, which) -> {
                    reminderManager.clearAllCompletedReminders();
                    loadReminders();
                })
                .setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }
}


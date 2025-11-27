package com.example.myapplication;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {
    private List<Reminder> reminders;
    private final OnReminderClickListener listener;
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);

    public interface OnReminderClickListener {
        void onEditClick(Reminder reminder);
        void onDeleteClick(Reminder reminder);
        void onCompleteClick(Reminder reminder);
        void onRestoreClick(Reminder reminder);
    }

    public ReminderAdapter(List<Reminder> reminders, OnReminderClickListener listener) {
        this.reminders = reminders != null ? reminders : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        if (reminders == null || position < 0 || position >= reminders.size()) {
            return;
        }
        Reminder reminder = reminders.get(position);
        holder.bind(reminder, listener);
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateReminders(List<Reminder> newReminders) {
        this.reminders = newReminders != null ? newReminders : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView reminderTitle;
        private final TextView reminderDate;
        private final TextView reminderTime;
        private final TextView reminderContent;
        private final TextView reminderStatus;
        private final MaterialButton editButton;
        private final MaterialButton deleteButton;
        private final MaterialButton completeButton;
        private final MaterialButton restoreButton;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            reminderTitle = itemView.findViewById(R.id.reminderTitle);
            reminderDate = itemView.findViewById(R.id.reminderDate);
            reminderTime = itemView.findViewById(R.id.reminderTime);
            reminderContent = itemView.findViewById(R.id.reminderContent);
            reminderStatus = itemView.findViewById(R.id.reminderStatus);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            completeButton = itemView.findViewById(R.id.completeButton);
            restoreButton = itemView.findViewById(R.id.restoreButton);
        }

        @SuppressLint("SetTextI18n")
        public void bind(Reminder reminder, OnReminderClickListener listener) {
            if (reminder == null) return;

            String title = reminder.getTitle();
            String content = reminder.getContent();
            String displayTitle = title == null || title.isEmpty() ? "No Title" : title;
            String displayContent = content == null || content.isEmpty() ? "No Content" : content;
            
            reminderTitle.setText(displayTitle);
            reminderContent.setText(displayContent);
            
            // Set status badge
            if (reminderStatus != null) {
                if (reminder.isDeleted()) {
                    reminderStatus.setVisibility(View.VISIBLE);
                    reminderStatus.setText("DELETED");
                    reminderStatus.setBackgroundColor(0xFFE53935); // Red
                } else if (reminder.isCompleted()) {
                    reminderStatus.setVisibility(View.VISIBLE);
                    reminderStatus.setText("COMPLETED");
                    reminderStatus.setBackgroundColor(0xFF43A047); // Green
                } else {
                    reminderStatus.setVisibility(View.GONE);
                }
            }

            // Format date display
            String dateStr = reminder.getDate();
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    Date date = INPUT_DATE_FORMAT.parse(dateStr);
                    reminderDate.setText(date != null ? OUTPUT_DATE_FORMAT.format(date) : dateStr);
                } catch (Exception e) {
                    reminderDate.setText(dateStr);
                }
            } else {
                reminderDate.setText("");
            }

            // Display time range
            String startTime = reminder.getStartTime();
            String endTime = reminder.getEndTime();
            if (startTime != null && !startTime.isEmpty()) {
                if (endTime != null && !endTime.isEmpty()) {
                    reminderTime.setText(itemView.getContext().getString(R.string.time_range, startTime, endTime));
                } else {
                    reminderTime.setText(startTime);
                }
            } else {
                reminderTime.setText("");
            }

            if (listener != null) {
                // If completed or deleted, hide complete and edit buttons, show restore button
                if (reminder.isCompleted() || reminder.isDeleted()) {
                    if (completeButton != null) {
                        completeButton.setVisibility(View.GONE);
                    }
                    if (editButton != null) {
                        editButton.setVisibility(View.GONE);
                    }
                    if (restoreButton != null) {
                        restoreButton.setVisibility(View.VISIBLE);
                        restoreButton.setOnClickListener(v -> listener.onRestoreClick(reminder));
                    }
                } else {
                    if (restoreButton != null) {
                        restoreButton.setVisibility(View.GONE);
                    }
                    if (completeButton != null) {
                        completeButton.setVisibility(View.VISIBLE);
                        completeButton.setOnClickListener(v -> listener.onCompleteClick(reminder));
                    }
                    if (editButton != null) {
                        editButton.setVisibility(View.VISIBLE);
                        editButton.setOnClickListener(v -> listener.onEditClick(reminder));
                    }
                }
                
                if (deleteButton != null) {
                    deleteButton.setOnClickListener(v -> listener.onDeleteClick(reminder));
                }
            }
        }
    }
}


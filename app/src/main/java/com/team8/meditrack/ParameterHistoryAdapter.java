package com.team8.meditrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParameterHistoryAdapter extends RecyclerView.Adapter<ParameterHistoryAdapter.HistoryViewHolder> {

    private List<HealthParameter> parameters;
    private String parameterType; // "temperature", "heart_rate", "spo2"
    private SimpleDateFormat timeFormat;

    public ParameterHistoryAdapter(List<HealthParameter> parameters, String parameterType) {
        this.parameters = parameters;
        this.parameterType = parameterType;
        this.timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parameter_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HealthParameter parameter = parameters.get(position);

        // Format timestamp to readable time
        String formattedTime = timeFormat.format(new Date(parameter.getTimestamp()));
        holder.textViewHistoryTimestamp.setText(formattedTime);

        // Format value based on parameter type
        String formattedValue;
        switch (parameterType) {
            case "temperature":
                formattedValue = String.format(Locale.getDefault(), "%.1f°C", parameter.getValue());
                break;
            case "heart_rate":
                formattedValue = String.format(Locale.getDefault(), "%d BPM", (int) parameter.getValue());
                break;
            case "spo2":
                formattedValue = String.format(Locale.getDefault(), "%d%%", (int) parameter.getValue());
                break;
            default:
                formattedValue = String.valueOf(parameter.getValue());
                break;
        }

        holder.textViewHistoryValue.setText(formattedValue);
    }

    @Override
    public int getItemCount() {
        return parameters != null ? parameters.size() : 0;
    }

    public void updateData(List<HealthParameter> newParameters) {
        this.parameters = newParameters;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHistoryTimestamp;
        TextView textViewHistoryValue;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHistoryTimestamp = itemView.findViewById(R.id.textViewHistoryTimestamp);
            textViewHistoryValue = itemView.findViewById(R.id.textViewHistoryValue);
        }
    }
}
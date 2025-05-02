package com.team8.meditrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

    private List<Medication> medications;
    private OnMedicationClickListener listener;

    public interface OnMedicationClickListener {
        void onDeleteClick(Medication medication);
    }

    public MedicationAdapter(List<Medication> medications, OnMedicationClickListener listener) {
        this.medications = medications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = medications.get(position);
        holder.textViewMedicationName.setText(medication.getName());
        holder.textViewMedicationTime.setText(medication.getTime());

        holder.buttonDeleteMedication.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(medication);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medications != null ? medications.size() : 0;
    }

    public void updateData(List<Medication> newMedications) {
        this.medications = newMedications;
        notifyDataSetChanged();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMedicationName;
        TextView textViewMedicationTime;
        ImageButton buttonDeleteMedication;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMedicationName = itemView.findViewById(R.id.textViewMedicationName);
            textViewMedicationTime = itemView.findViewById(R.id.textViewMedicationTime);
            buttonDeleteMedication = itemView.findViewById(R.id.buttonDeleteMedication);
        }
    }
}
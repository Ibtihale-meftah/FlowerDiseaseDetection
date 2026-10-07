package org.tensorflow.lite.examples.imageclassification.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.tensorflow.lite.examples.imageclassification.R;
import org.tensorflow.lite.examples.imageclassification.firebase.FirebaseRepository;
import org.tensorflow.lite.examples.imageclassification.firebase.FlowerDetection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<FlowerDetection> detectionList;
    private final FirebaseRepository firebaseRepository;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public HistoryAdapter(List<FlowerDetection> detectionList, FirebaseRepository firebaseRepository) {
        this.detectionList = detectionList;
        this.firebaseRepository = firebaseRepository;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historique, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        FlowerDetection detection = detectionList.get(position);

        String flowerClass = detection.flowerClass != null ? detection.flowerClass : "Inconnu";

        holder.tvLabel.setText(capitalize(flowerClass));
        holder.tvScore.setText(
                String.format(Locale.getDefault(), "Confiance : %.1f%%", detection.confidence * 100)
        );
        holder.tvInference.setText("Inférence : " + detection.inferenceTime + " ms");

        if (detection.createdAt > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(detection.createdAt)));
        } else {
            holder.tvDate.setText("");
        }

        if (detection.note != null && !detection.note.isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText(detection.note);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        if (detection.imageUrl != null && !detection.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(detection.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(holder.imgDetection);
        } else {
            holder.imgDetection.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnFavorite.setImageResource(
                detection.isFavorite
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off
        );

        holder.btnFavorite.setOnClickListener(v -> {
            if (detection.id != null && !detection.id.isEmpty()) {
                firebaseRepository.updateDetectionFavorite(detection.id, !detection.isFavorite);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (detection.id != null && !detection.id.isEmpty()) {
                firebaseRepository.deleteDetection(detection.id);
            }
        });
    }
    public void updateData(List<FlowerDetection> newList) {
        detectionList.clear();
        detectionList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return detectionList.size();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "Inconnu";
        return value.substring(0, 1).toUpperCase(Locale.ROOT)
                + value.substring(1).toLowerCase(Locale.ROOT);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        ImageView imgDetection;
        TextView tvLabel;
        TextView tvScore;
        TextView tvInference;
        TextView tvDate;
        TextView tvNote;
        ImageButton btnDelete;
        ImageButton btnFavorite;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            imgDetection = itemView.findViewById(R.id.img_detection);
            tvLabel = itemView.findViewById(R.id.tv_hist_label);
            tvScore = itemView.findViewById(R.id.tv_hist_score);
            tvInference = itemView.findViewById(R.id.tv_hist_time);
            tvDate = itemView.findViewById(R.id.tv_hist_date);
            tvNote = itemView.findViewById(R.id.tv_hist_note);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}
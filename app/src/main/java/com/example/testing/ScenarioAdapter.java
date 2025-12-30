package com.example.testing;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class ScenarioAdapter extends RecyclerView.Adapter<ScenarioAdapter.ScenarioViewHolder> {

    private List<Scenario> scenarios = new ArrayList<>();
    private OnScenarioActionListener listener;

    public interface OnScenarioActionListener {
        void onEdit(Scenario scenario);
        void onDelete(Scenario scenario);
    }

    public void setOnScenarioActionListener(OnScenarioActionListener listener) {
        this.listener = listener;
    }

    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScenarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scenario, parent, false);
        return new ScenarioViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScenarioViewHolder holder, int position) {
        Scenario scenario = scenarios.get(position);
        holder.bind(scenario);
    }

    @Override
    public int getItemCount() {
        return scenarios.size();
    }

    class ScenarioViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumb;
        TextView textViewName;
        TextView textViewDescription;
        TextView textViewIsDefault;
        ImageButton buttonEdit;
        ImageButton buttonDelete;

        public ScenarioViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewThumb = itemView.findViewById(R.id.image_view_scenario_thumb);
            textViewName = itemView.findViewById(R.id.text_view_scenario_name);
            textViewDescription = itemView.findViewById(R.id.text_view_scenario_description);
            textViewIsDefault = itemView.findViewById(R.id.text_view_is_default);
            buttonEdit = itemView.findViewById(R.id.button_edit_scenario);
            buttonDelete = itemView.findViewById(R.id.button_delete_scenario);

            buttonEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onEdit(scenarios.get(position));
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onDelete(scenarios.get(position));
                }
            });
        }

        public void bind(Scenario scenario) {
            textViewName.setText(scenario.getName());

            String desc = scenario.getDescription();
            if (desc != null && desc.length() > 50) {
                desc = desc.substring(0, 50) + "...";
            }
            textViewDescription.setText(desc);

            if (scenario.isDefault()) {
                textViewIsDefault.setVisibility(View.VISIBLE);
                textViewIsDefault.setText("Default");
            } else {
                textViewIsDefault.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(scenario.getImagePath())) {
                Glide.with(itemView.getContext())
                        .load(scenario.getImagePath())
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imageViewThumb);
            } else {
                imageViewThumb.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }
}
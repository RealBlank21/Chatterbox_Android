package com.example.testing;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin; // Correct Import

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_CHARACTER = 2;

    // We need two renderers: one for User (White Text) and one for Character (Black Text)
    private Markwon markwonUser;
    private Markwon markwonCharacter;

    private OnMessageLongClickListener longClickListener;
    private int editingPosition = -1;
    private OnMessageEditListener editListener;

    public interface OnMessageEditListener {
        void onMessageEdited(Message message);
    }

    public void setOnMessageEditListener(OnMessageEditListener listener) {
        this.editListener = listener;
    }

    public void setEditingPosition(int position) {
        int oldPosition = this.editingPosition;
        this.editingPosition = position;

        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (this.editingPosition != -1) {
            notifyItemChanged(this.editingPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if ("user".equals(message.getRole())) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_CHARACTER;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Initialize Markwon instances if null
        if (markwonUser == null) {
            float textSize = parent.getContext().getResources().getDisplayMetrics().scaledDensity * 16;

            // 1. User Markwon (Purple BG -> White Text)
            markwonUser = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    // Enable Inline Parsing (e.g. $...$)
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    // Configure Latex: Enable inlines & Set text color to WHITE
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(Color.WHITE);
                    }))
                    .build();

            // 2. Character Markwon (Gray BG -> Black Text)
            markwonCharacter = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    // Configure Latex: Enable inlines & Set text color to BLACK
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(Color.BLACK);
                    }))
                    .build();
        }

        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item_character, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        if (position == editingPosition) {
            holder.showEditMode(message);

            holder.buttonSaveEdit.setOnClickListener(v -> {
                String newContent = holder.editTextMessageContent.getText().toString().trim();
                if (!newContent.isEmpty()) {
                    message.setContent(newContent);
                    if (editListener != null) {
                        editListener.onMessageEdited(message);
                    }
                    setEditingPosition(-1);
                } else {
                    setEditingPosition(-1);
                }
            });

        } else {
            // Select the correct Markwon instance based on the role
            Markwon activeMarkwon = "user".equals(message.getRole()) ? markwonUser : markwonCharacter;
            holder.showDisplayMode(message, activeMarkwon);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        View layoutEditMessage;
        EditText editTextMessageContent;
        Button buttonSaveEdit;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_message);
            layoutEditMessage = itemView.findViewById(R.id.layout_edit_message);
            editTextMessageContent = itemView.findViewById(R.id.edit_text_message_content);
            buttonSaveEdit = itemView.findViewById(R.id.button_save_edit);

            OnMessageLongClickListener listener = longClickListener;
            View.OnLongClickListener longClick = v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onMessageLongClick(messages.get(position), v, position);
                }
                return true;
            };

            itemView.setOnLongClickListener(longClick);
            textViewMessage.setOnLongClickListener(longClick);
        }

        public void showDisplayMode(Message message, Markwon markwon) {
            // Use the passed 'markwon' instance (User or Character) to render
            markwon.setMarkdown(textViewMessage, message.getContent());
            textViewMessage.setVisibility(View.VISIBLE);
            layoutEditMessage.setVisibility(View.GONE);
        }

        public void showEditMode(Message message) {
            textViewMessage.setVisibility(View.GONE);
            layoutEditMessage.setVisibility(View.VISIBLE);
            editTextMessageContent.setText(message.getContent());
            editTextMessageContent.requestFocus();
        }
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, View anchorView, int position);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
}
package com.example.testing;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_CHARACTER = 2;

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
        if (markwonUser == null) {
            float textSize = parent.getContext().getResources().getDisplayMetrics().scaledDensity * 16;

            markwonUser = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(Color.WHITE);
                    }))
                    .build();

            markwonCharacter = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .usePlugin(MarkwonInlineParserPlugin.create())
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
        ImageView imageViewMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_message);
            layoutEditMessage = itemView.findViewById(R.id.layout_edit_message);
            editTextMessageContent = itemView.findViewById(R.id.edit_text_message_content);
            buttonSaveEdit = itemView.findViewById(R.id.button_save_edit);
            imageViewMessage = itemView.findViewById(R.id.image_view_message_image);

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
            if(imageViewMessage != null) imageViewMessage.setOnLongClickListener(longClick);
        }

        public void showDisplayMode(Message message, Markwon markwon) {
            String content = message.getContent();

            if (content != null) {
                content = content.replace("\\[", "$$").replace("\\]", "$$");
                content = content.replace("\\(", "$").replace("\\)", "$");
            }

            markwon.setMarkdown(textViewMessage, content != null ? content : "");
            textViewMessage.setVisibility(View.VISIBLE);
            layoutEditMessage.setVisibility(View.GONE);

            if (imageViewMessage != null) {
                if (!TextUtils.isEmpty(message.getImagePath())) {
                    imageViewMessage.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(message.getImagePath())
                            .transform(new RoundedCorners(16))
                            .into(imageViewMessage);
                } else {
                    imageViewMessage.setVisibility(View.GONE);
                }
            }
        }

        public void showEditMode(Message message) {
            textViewMessage.setVisibility(View.GONE);
            if (imageViewMessage != null) imageViewMessage.setVisibility(View.GONE); // Hide image in edit mode? Or keep it. Hiding for simplicity.
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
package com.example.testing.ui.conversation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testing.R;
import com.example.testing.data.local.entity.Message;
import com.example.testing.data.local.entity.User;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import org.commonmark.node.Emphasis;

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

    private int narrativeColor = Color.GRAY;
    private int dialogueColor = Color.WHITE;
    private float bubbleWidth = 0.9f;
    private float lineSpacing = 1.0f;

    public interface OnMessageEditListener {
        void onMessageEdited(Message message);
    }

    public MessageAdapter() {
    }

    public void updateSettings(User user) {
        if (user == null) return;
        this.narrativeColor = user.getNarrativeTextColor();
        this.dialogueColor = user.getDialogueTextColor();
        this.bubbleWidth = user.getChatBubbleWidth();
        this.lineSpacing = user.getChatLineSpacing();

        markwonUser = null;
        markwonCharacter = null;
        notifyDataSetChanged();
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
        Context context = parent.getContext();

        if (markwonUser == null) {
            float textSize = context.getResources().getDisplayMetrics().scaledDensity * 16;

            AbstractMarkwonPlugin stylePlugin = new AbstractMarkwonPlugin() {
                @Override
                public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                    builder.setFactory(Emphasis.class, (configuration, props) -> new Object[]{
                            new StyleSpan(Typeface.ITALIC),
                            new ForegroundColorSpan(narrativeColor)
                    });
                }
            };

            markwonUser = Markwon.builder(context)
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(stylePlugin)
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(dialogueColor);
                    }))
                    .build();

            markwonCharacter = Markwon.builder(context)
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(stylePlugin)
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(dialogueColor);
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

        holder.applyAppearance(bubbleWidth, lineSpacing, dialogueColor);

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

    public void setMessages(List<Message> newMessages) {
        if (this.messages == null) {
            this.messages = newMessages;
            notifyDataSetChanged();
            return;
        }

        if (this.messages.size() == newMessages.size() && !newMessages.isEmpty()) {
            int lastIndex = newMessages.size() - 1;
            Message oldLast = this.messages.get(lastIndex);
            Message newLast = newMessages.get(lastIndex);

            if (!oldLast.getContent().equals(newLast.getContent())) {
                this.messages = newMessages;
                notifyItemChanged(lastIndex);
                return;
            }
        }

        this.messages = newMessages;
        notifyDataSetChanged();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        View layoutEditMessage;
        EditText editTextMessageContent;
        Button buttonSaveEdit;
        View layoutMessageContainer;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_message);
            layoutEditMessage = itemView.findViewById(R.id.layout_edit_message);
            editTextMessageContent = itemView.findViewById(R.id.edit_text_message_content);
            buttonSaveEdit = itemView.findViewById(R.id.button_save_edit);
            layoutMessageContainer = itemView.findViewById(R.id.layout_message_container);

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

        public void applyAppearance(float widthPercent, float spacingMult, int baseColor) {
            ViewGroup.LayoutParams params = layoutMessageContainer.getLayoutParams();
            if (params instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams constraintParams = (ConstraintLayout.LayoutParams) params;
                constraintParams.matchConstraintPercentWidth = widthPercent;
                layoutMessageContainer.setLayoutParams(constraintParams);
            }

            textViewMessage.setLineSpacing(0f, spacingMult);
            textViewMessage.setTextColor(baseColor);
        }

        public void showDisplayMode(Message message, Markwon markwon) {
            String displayText = message.getContent();

            if (displayText != null) {
                displayText = displayText.replace("\\[", "$$").replace("\\]", "$$");
                displayText = displayText.replace("\\(", "$").replace("\\)", "$");
            }

            markwon.setMarkdown(textViewMessage, displayText != null ? displayText : "");

            if (TextUtils.isEmpty(displayText)) {
                textViewMessage.setVisibility(View.GONE);
            } else {
                textViewMessage.setVisibility(View.VISIBLE);
            }
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
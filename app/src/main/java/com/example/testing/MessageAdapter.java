package com.example.testing;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_CHARACTER = 2;

    private Markwon markwonUser;
    private Markwon markwonCharacter;

    private OnMessageLongClickListener longClickListener;
    private int editingPosition = -1;
    private OnMessageEditListener editListener;
    private OnImageClickListener imageClickListener;

    // Image Handling
    private final GalleryImageDao galleryImageDao;
    private final ExecutorService imageQueryExecutor;
    private final Handler mainHandler;
    private final Map<String, String> uuidToPathCache = new HashMap<>(); // Simple cache
    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile("\\$image:([a-f0-9\\-]+)\\$");

    public interface OnMessageEditListener {
        void onMessageEdited(Message message);
    }

    public interface OnImageClickListener {
        void onImageClick(String imagePath);
    }

    // Constructor updated to require DAO
    public MessageAdapter(GalleryImageDao galleryImageDao) {
        this.galleryImageDao = galleryImageDao;
        this.imageQueryExecutor = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setOnMessageEditListener(OnMessageEditListener listener) {
        this.editListener = listener;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
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

            // Re-using context for Markwon builder
            Context context = parent.getContext();

            markwonUser = Markwon.builder(context)
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(Color.WHITE);
                    }))
                    .build();

            markwonCharacter = Markwon.builder(context)
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(MarkwonInlineParserPlugin.create())
                    .usePlugin(JLatexMathPlugin.create(textSize, builder -> {
                        builder.inlinesEnabled(true);
                        builder.theme().textColor(Color.BLACK); // Ensure black text for character
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
            String rawContent = message.getContent();
            String displayText = rawContent;
            String imageUuid = null;

            // 1. Check for Image Tag
            if (rawContent != null) {
                Matcher matcher = IMAGE_TAG_PATTERN.matcher(rawContent);
                if (matcher.find()) {
                    imageUuid = matcher.group(1);
                    // Remove the tag from the text displayed to the user
                    displayText = rawContent.replace(matcher.group(0), "").trim();
                }
            }

            // 2. Render Text
            if (displayText != null) {
                displayText = displayText.replace("\\[", "$$").replace("\\]", "$$");
                displayText = displayText.replace("\\(", "$").replace("\\)", "$");
            }

            markwon.setMarkdown(textViewMessage, displayText != null ? displayText : "");

            // Hide text view if empty (e.g. message was ONLY an image)
            if (TextUtils.isEmpty(displayText)) {
                textViewMessage.setVisibility(View.GONE);
            } else {
                textViewMessage.setVisibility(View.VISIBLE);
            }
            layoutEditMessage.setVisibility(View.GONE);

            // 3. Render Image
            if (imageViewMessage != null) {
                // Priority 1: User uploaded image (existing logic)
                if (!TextUtils.isEmpty(message.getImagePath())) {
                    imageViewMessage.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(message.getImagePath())
                            .transform(new RoundedCorners(16))
                            .into(imageViewMessage);
                    imageViewMessage.setOnClickListener(v -> {
                        if (imageClickListener != null) {
                            imageClickListener.onImageClick(message.getImagePath());
                        }
                    });
                }
                // Priority 2: AI sent Gallery Image (new logic)
                else if (imageUuid != null) {
                    imageViewMessage.setVisibility(View.VISIBLE);
                    loadGalleryImage(imageUuid, imageViewMessage);
                }
                else {
                    imageViewMessage.setVisibility(View.GONE);
                }
            }
        }

        private void loadGalleryImage(String uuid, ImageView target) {
            // Check cache first
            if (uuidToPathCache.containsKey(uuid)) {
                String path = uuidToPathCache.get(uuid);
                Glide.with(itemView.getContext())
                        .load(path)
                        .transform(new RoundedCorners(16))
                        .into(target);
                target.setOnClickListener(v -> {
                    if (imageClickListener != null) {
                        imageClickListener.onImageClick(path);
                    }
                });
                return;
            }

            // Set placeholder or clear while loading
            target.setImageDrawable(null);
            target.setOnClickListener(null);

            // Fetch from DB
            imageQueryExecutor.execute(() -> {
                GalleryImage img = galleryImageDao.getImageByUuid(uuid);
                mainHandler.post(() -> {
                    if (img != null && img.getImagePath() != null) {
                        uuidToPathCache.put(uuid, img.getImagePath());
                        // Verify the view holder still needs this image (basic check)
                        if (target.getWindowToken() != null) {
                            Glide.with(itemView.getContext())
                                    .load(img.getImagePath())
                                    .transform(new RoundedCorners(16))
                                    .into(target);
                            target.setOnClickListener(v -> {
                                if (imageClickListener != null) {
                                    imageClickListener.onImageClick(img.getImagePath());
                                }
                            });
                        }
                    }
                });
            });
        }

        public void showEditMode(Message message) {
            textViewMessage.setVisibility(View.GONE);
            if (imageViewMessage != null) imageViewMessage.setVisibility(View.GONE);
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
package com.connectycube.messenger.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.ConnectycubeChatService
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.messenger.R
import timber.log.Timber


typealias ClickListener = (ConnectycubeChatMessage) -> Unit

class ChatMessageAdapter(
    val context: Context,
    private val clickListener: ClickListener
) : PagedListAdapter<ConnectycubeChatMessage, ChatMessageAdapter.ChatMessageViewHolderBase>(diffCallback) {
    val TEXT_OUTCOMING = 1
    val TEXT_INCOMING = 2
    val ATTACH_IMAGE_OUTCOMING = 3
    val ATTACH_IMAGE_INCOMING = 4

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolderBase {
        Timber.d("onCreateViewHolder viewType= " + viewType)
        return when (viewType) {
            TEXT_OUTCOMING -> ChatMessageViewHolder(parent, R.layout.chat_outcoming_item)
            TEXT_INCOMING -> ChatMessageViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_OUTCOMING -> ChatAttachViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_INCOMING -> ChatAttachViewHolder(parent, R.layout.chat_incoming_item)
            else -> throw IllegalArgumentException("Wrong type of viewType= $viewType")
        }
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolderBase, position: Int) {
        Timber.d("Binding view holder at position $position")
        when (this.getItemViewType(position)) {
            TEXT_OUTCOMING, TEXT_INCOMING -> onBindTextViewHolder(holder as ChatMessageViewHolder, position)
            ATTACH_IMAGE_OUTCOMING, ATTACH_IMAGE_INCOMING -> onBindAttachViewHolder(holder as ChatAttachViewHolder, position)
        }

    }

    fun onBindTextViewHolder(holder: ChatMessageViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
            message.let {
                itemView.setOnClickListener {
                    clickListener(message)
                }
            }
        }
    }

    fun onBindAttachViewHolder(holder: ChatAttachViewHolder, position: Int) {
        val message = getItem(position)
        with(holder) {
            bindTo(message!!)
            message.let {
                itemView.setOnClickListener {
                    clickListener(message)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = this.getItem(position)!!
        val isReceived = isIncoming(chatMessage)
        return if (withAttachment(chatMessage)) {
            if (isReceived) {
                ATTACH_IMAGE_INCOMING
            } else ATTACH_IMAGE_OUTCOMING
        } else if (isReceived) {
            TEXT_INCOMING
        } else TEXT_OUTCOMING
    }

    fun isIncoming(chatMessage: ConnectycubeChatMessage): Boolean {
        val localUser = ConnectycubeChatService.getInstance().user
        return chatMessage.senderId != localUser.id
    }

    fun withAttachment(chatMessage: ConnectycubeChatMessage): Boolean {
        val attachments = chatMessage.attachments
        return attachments != null && !attachments.isEmpty()
    }

    fun formatDate(seconds: Long): String {
        return DateUtils.formatDateTime(context, seconds * 1000L, DateUtils.FORMAT_NUMERIC_DATE)
    }

    companion object {
        /**
         * This diff callback informs the PagedListAdapter how to compute list differences when new
         * PagedLists arrive.
         */
        private val diffCallback = object : DiffUtil.ItemCallback<ConnectycubeChatMessage>() {
            override fun areItemsTheSame(oldItem: ConnectycubeChatMessage, newItem: ConnectycubeChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ConnectycubeChatMessage,
                newItem: ConnectycubeChatMessage
            ): Boolean =
                oldItem == newItem
        }
    }

    open inner class ChatMessageViewHolderBase(@NonNull itemView: View) : RecyclerView.ViewHolder(
        itemView
    ) {

        /**
         * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
         * ViewHolder when Item is loaded.
         */
        open fun bindTo(message: ConnectycubeChatMessage) {

        }
    }

    inner class ChatMessageViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : ChatMessageViewHolderBase(
        LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
    ) {
        private val bodyView = itemView.findViewById<TextView>(R.id.text_message_body)
        private val dateView = itemView.findViewById<TextView>(R.id.text_message_date)
        private var message: ConnectycubeChatMessage? = null

        /**
         * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
         * ViewHolder when Item is loaded.
         */
        override fun bindTo(message: ConnectycubeChatMessage) {
            this.message = message
            bodyView.text = message.body
            dateView.text = formatDate(message.dateSent)
        }
    }

    inner class ChatAttachViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) : ChatMessageViewHolderBase(
        LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
    ) {
        private val bodyView = itemView.findViewById<TextView>(R.id.text_message_body)
        private val dateView = itemView.findViewById<TextView>(R.id.text_message_date)
        private var message: ConnectycubeChatMessage? = null

        /**
         * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
         * ViewHolder when Item is loaded.
         */
        override fun bindTo(message: ConnectycubeChatMessage) {
            this.message = message
            dateView.text = formatDate(message.dateSent)
        }
    }
}
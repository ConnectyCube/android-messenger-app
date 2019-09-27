package com.connectycube.messenger.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.auth.session.ConnectycubeSessionManager
import com.connectycube.chat.model.ConnectycubeAttachment
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeChatMessage
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.core.helper.CollectionsUtil
import com.connectycube.messenger.R
import com.connectycube.messenger.paging.NetworkState
import com.connectycube.messenger.utilities.*
import com.connectycube.users.model.ConnectycubeUser
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import timber.log.Timber


typealias AttachmentClickListener = (ConnectycubeAttachment) -> Unit


class ChatMessageAdapter(
    val context: Context,
    var chatDialog: ConnectycubeChatDialog,
    private val attachmentClickListener: AttachmentClickListener
) : PagedListAdapter<ConnectycubeChatMessage, RecyclerView.ViewHolder>(diffCallback),
    StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    val IN_PROGRESS = -1
    val TEXT_OUTCOMING = 1
    val TEXT_INCOMING = 2
    val ATTACH_IMAGE_OUTCOMING = 3
    val ATTACH_IMAGE_INCOMING = 4

    val localUserId = ConnectycubeSessionManager.getInstance().sessionParameters.userId
    val occupantsIds: ArrayList<Int> =
        ArrayList<Int>(chatDialog.occupants).apply { remove(localUserId) }
    val occupants: MutableMap<Int, ConnectycubeUser> = mutableMapOf()
    private var networkState: NetworkState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Timber.d("onCreateViewHolder viewType= $viewType")
        return when (viewType) {
            TEXT_OUTCOMING -> ChatMessageOutcomingViewHolder(parent, R.layout.chat_outcoming_item)
            TEXT_INCOMING -> ChatMessageIncomingViewHolder(parent, R.layout.chat_incoming_item)
            ATTACH_IMAGE_OUTCOMING -> ChatImageAttachOutcomingViewHolder(
                parent,
                R.layout.chat_outcoming_attachimage_item
            )
            ATTACH_IMAGE_INCOMING -> ChatImageAttachIncomingViewHolder(
                parent,
                R.layout.chat_incoming_attachimage_item
            )
            IN_PROGRESS -> NetworkStateItemViewHolder.create(parent)
            else -> throw IllegalArgumentException("Wrong type of viewType= $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder,
                                  position: Int,
                                  payloads: MutableList<Any>
    ) {
        Timber.d("Binding view holder at position $position, payloads= ${payloads.isNotEmpty()}")
        if (payloads.isNotEmpty()) {
            val message = getItem(position)
            message?.let {
                val imgStatus =
                    holder.itemView.findViewById<ImageView>(R.id.message_status_image_view)
                setStatus(imgStatus, message)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Timber.d("Binding view holder at position $position")
        val chatMessage = getItem(position)
        chatMessage?.let {
            if (isIncoming(chatMessage) && !isRead(chatMessage)) {
                markAsReadMessage(chatMessage)
            }
        }

        when (this.getItemViewType(position)) {
            TEXT_OUTCOMING -> onBindTextViewHolderOutComing(
                holder as ChatMessageOutcomingViewHolder,
                position
            )
            TEXT_INCOMING -> onBindTextViewHolderInComing(
                holder as ChatMessageIncomingViewHolder,
                position
            )
            ATTACH_IMAGE_OUTCOMING -> onBindAttachViewHolderOutComing(
                holder as BaseChatMessageViewHolder,
                position
            )
            ATTACH_IMAGE_INCOMING -> onBindAttachViewHolderInComing(
                holder as ChatImageAttachIncomingViewHolder,
                position
            )
            IN_PROGRESS -> (holder as NetworkStateItemViewHolder).bindTo(
                networkState
            )
        }
    }

    fun onBindTextViewHolderOutComing(holder: ChatMessageOutcomingViewHolder, position: Int) {
        val message = getItem(position)
        message?.let {
            with(holder) {
                bindTo(it)
            }
        }
    }

    fun onBindTextViewHolderInComing(holder: ChatMessageIncomingViewHolder, position: Int) {
        val message = getItem(position)
        message?.let {
            with(holder) {
                bindTo(it, showAvatar(position, message), showName(position, message))
            }
        }
    }

    fun onBindAttachViewHolderOutComing(holder: BaseChatMessageViewHolder,
                                        position: Int
    ) {
        val message = getItem(position)
        message?.let {
            with(holder) {
                bindTo(it)
                message.let {
                    itemView.setOnClickListener {
                        attachmentClickListener(message.attachments.first())
                    }
                }
            }
        }
    }

    fun onBindAttachViewHolderInComing(holder: ChatImageAttachIncomingViewHolder, position: Int) {
        val message = getItem(position)
        message?.let {
            with(holder) {
                bindTo(it, showAvatar(position, message), showName(position, message))
                itemView.setOnClickListener {
                    attachmentClickListener(message.attachments.first())
                }
            }
        }
    }

    private fun showAvatar(position: Int, currentMsg: ConnectycubeChatMessage): Boolean {
        return chatDialog.isPrivate || isNeedShowExtraData(position, currentMsg)
    }

    private fun showName(position: Int, currentMsg: ConnectycubeChatMessage): Boolean {
        return !chatDialog.isPrivate && isNeedShowExtraData(position, currentMsg)
    }

    private fun isNeedShowExtraData(position: Int, currentMsg: ConnectycubeChatMessage): Boolean {
        fun isPreviousTheSameSender(position: Int,
                                    currentMsg: ConnectycubeChatMessage
        ): Boolean {
            val previousPosition = position + 1
            if (previousPosition >= itemCount) {
                return false
            }
            val previousMsg = getItem(previousPosition)
            previousMsg?.let {
                return currentMsg.senderId == previousMsg.senderId
            }
            return false
        }

        fun isPreviousHeader(position: Int): Boolean {
            val previousPosition = position + 1
            if (previousPosition >= itemCount) {
                return true
            }
            return isHeaderView(previousPosition)
        }

        return !isPreviousTheSameSender(position, currentMsg) || isPreviousHeader(position)
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = this.getItem(position)
        chatMessage?.let {
            val isReceived = isIncoming(chatMessage)
            return if (withAttachment(chatMessage)) {
                if (isReceived) {
                    ATTACH_IMAGE_INCOMING
                } else ATTACH_IMAGE_OUTCOMING
            } else if (isReceived) {
                TEXT_INCOMING
            } else TEXT_OUTCOMING
        }
        return IN_PROGRESS
    }

    fun isHeaderView(position: Int): Boolean {
        if (position >= itemCount) {
            return false
        }
        val msgCurrent = getItem(position)
        val msgNext = getItem(position - 1)
        if (msgCurrent != null && msgNext != null) {
            val dateMsgCurrent: Long? = getDateAsHeaderId(msgCurrent.dateSent * 1000)
            val dateMsgNext: Long? = getDateAsHeaderId(msgNext.dateSent * 1000)
            return dateMsgCurrent != dateMsgNext
        }
        return false
    }

    override fun getHeaderId(position: Int): Long {
        val chatMessage = getItem(position)
        var date = 0L
        chatMessage?.let {
            date = getDateAsHeaderId(chatMessage.dateSent * 1000)
        }
        return date
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(
            R.layout.chat_message_header,
            parent,
            false
        )
        return object : RecyclerView.ViewHolder(view) {
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView
        val dateView = view.findViewById<TextView>(R.id.header_text_view)

        val chatMessage = getItem(position)
        chatMessage?.let {
            dateView.text = getPrettyMessageDate(context, chatMessage.dateSent * 1000)
        }
    }

    fun setStatus(imgStatus: ImageView?, msg: ConnectycubeChatMessage) {
        when {
            messageIsRead(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_double_color_16)
            messageIsDelivered(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_double_16)
            messageIsSent(msg) -> imgStatus?.setImageResource(R.drawable.ic_check_black_16dp)
            else -> imgStatus?.setImageResource(android.R.color.transparent)
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    fun setOccupants(newOccupants: Map<Int, ConnectycubeUser>) {
        occupants.clear()
        occupants.putAll(newOccupants)
        occupantsIds.clear()
        occupantsIds.addAll(newOccupants.keys.toList())
    }

    fun isIncoming(chatMessage: ConnectycubeChatMessage): Boolean {
        return chatMessage.senderId != null && chatMessage.senderId != localUserId
    }

    fun withAttachment(chatMessage: ConnectycubeChatMessage): Boolean {
        val attachments = chatMessage.attachments
        return attachments != null && !attachments.isEmpty()
    }

    fun formatDate(seconds: Long): String {
        return DateUtils.formatDateTime(context, seconds * 1000L, DateUtils.FORMAT_SHOW_TIME)
    }

    private fun markAsReadMessage(chatMessage: ConnectycubeChatMessage) {
        try {
            chatDialog.readMessage(chatMessage)
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    private fun isRead(chatMessage: ConnectycubeChatMessage): Boolean {
        return !CollectionsUtil.isEmpty(chatMessage.readIds) && chatMessage.readIds.contains(
            localUserId
        )
    }

    companion object {
        /**
         * This diff callback informs the PagedListAdapter how to compute list differences when new
         * PagedLists arrive.
         */
        private val PAYLOAD_STATUS = Any()
        private val diffCallback = object : DiffUtil.ItemCallback<ConnectycubeChatMessage>() {
            override fun areItemsTheSame(oldItem: ConnectycubeChatMessage,
                                         newItem: ConnectycubeChatMessage
            ): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ConnectycubeChatMessage,
                newItem: ConnectycubeChatMessage
            ): Boolean =
                oldItem.id == newItem.id && oldItem.readIds == newItem.readIds && oldItem.deliveredIds == newItem.deliveredIds

            override fun getChangePayload(oldItem: ConnectycubeChatMessage,
                                          newItem: ConnectycubeChatMessage
            ): Any? {
                return if (sameExceptStatus(oldItem, newItem)) {
                    PAYLOAD_STATUS
                } else null
            }

            fun sameExceptStatus(oldItem: ConnectycubeChatMessage,
                                 newItem: ConnectycubeChatMessage
            ): Boolean {
                return newItem.readIds != oldItem.readIds || newItem.deliveredIds != oldItem.deliveredIds
            }
        }
    }

    private fun messageIsSent(message: ConnectycubeChatMessage): Boolean {
        return message.deliveredIds?.contains(localUserId) ?: false
    }

    private fun messageIsRead(message: ConnectycubeChatMessage): Boolean {
        if (chatDialog.isPrivate) return message.readIds != null &&
                (message.recipientId == null || message.readIds.contains(message.recipientId))
        return message.readIds != null && message.readIds.any { it in occupantsIds }
    }

    private fun messageIsDelivered(message: ConnectycubeChatMessage): Boolean {
        if (chatDialog.isPrivate) return message.deliveredIds?.contains(message.recipientId)
            ?: false
        return message.deliveredIds != null && message.deliveredIds.any { it in occupantsIds }
    }

    fun getAttachImageUrl(attachment: ConnectycubeAttachment): String {
        return attachment.url
    }

    open inner class BaseChatMessageViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(
        itemView
    ) {
        val dateView = itemView.findViewById<TextView>(R.id.text_message_date)
        /**
         * Items might be null if they are not paged in yet. PagedListAdapter will re-bind the
         * ViewHolder when Item is loaded.
         */
        open fun bindTo(message: ConnectycubeChatMessage) {
            dateView.text = formatDate(message.dateSent)
        }
    }

    open inner class BaseChatMessageTextViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageViewHolder(
            LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
        ) {
        private val bodyView = itemView.findViewById<TextView>(R.id.text_message_body)
        private var message: ConnectycubeChatMessage? = null

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            this.message = message
            bodyView.text = message.body
        }
    }

    inner class ChatMessageIncomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageTextViewHolder(parent, chatItem) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val senderName: TextView = itemView.findViewById(R.id.text_message_sender)

        fun bindTo(message: ConnectycubeChatMessage, showAvatar: Boolean, showName: Boolean) {
            super.bindTo(message)
            if (showAvatar || showName) {
                val sender = occupants[message.senderId]
                if (showAvatar) {
                    imgAvatar.visibility = View.VISIBLE

                    loadChatMessagePhoto(
                        chatDialog.type == ConnectycubeDialogType.PRIVATE,
                        sender?.avatar,
                        imgAvatar,
                        context
                    )
                } else {
                    imgAvatar.visibility = View.INVISIBLE
                }
                if (showName) {
                    senderName.visibility = View.VISIBLE
                    sender?.let {
                        senderName.text = sender.fullName ?: sender.login
                    }
                } else {
                    senderName.visibility = View.GONE
                }
            }
        }
    }

    inner class ChatMessageOutcomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageTextViewHolder(parent, chatItem) {
        val imgStatus: ImageView = itemView.findViewById(R.id.message_status_image_view)
        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            setStatus(imgStatus, message)
        }
    }

    open inner class BaseChatImageAttachViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatMessageViewHolder(
            LayoutInflater.from(parent.context).inflate(chatItem, parent, false)
        ) {
        private val attachmentView: ImageView = itemView.findViewById(R.id.attachment_image_view)

        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            showImageAttachment(message)
        }

        private fun showImageAttachment(message: ConnectycubeChatMessage) {
            val validUrl = getAttachImageUrl(message.attachments.iterator().next())
            loadAttachImage(validUrl, attachmentView, context)
        }
    }

    inner class ChatImageAttachIncomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatImageAttachViewHolder(parent, chatItem) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val senderName: TextView = itemView.findViewById(R.id.text_message_sender)

        fun bindTo(message: ConnectycubeChatMessage, showAvatar: Boolean, showName: Boolean) {
            super.bindTo(message)
            if (showAvatar || showName) {
                val sender = occupants[message.senderId]
                if (showAvatar) {
                    imgAvatar.visibility = View.VISIBLE
                    loadChatMessagePhoto(
                        chatDialog.type == ConnectycubeDialogType.PRIVATE,
                        sender?.avatar,
                        imgAvatar,
                        context
                    )
                } else {
                    imgAvatar.visibility = View.INVISIBLE
                }
                if (showName) {
                    senderName.visibility = View.VISIBLE
                    sender?.let {
                        senderName.text = sender.fullName ?: sender.login
                    }
                } else {
                    senderName.visibility = View.GONE
                }
            }
        }
    }

    inner class ChatImageAttachOutcomingViewHolder(parent: ViewGroup, @LayoutRes chatItem: Int) :
        BaseChatImageAttachViewHolder(parent, chatItem) {
        private val imgStatus: ImageView = itemView.findViewById(R.id.message_status_image_view)
        override fun bindTo(message: ConnectycubeChatMessage) {
            super.bindTo(message)
            setStatus(imgStatus, message)
        }
    }
}
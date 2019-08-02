package com.connectycube.messenger.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.chat.model.ConnectycubeChatDialog
import com.connectycube.chat.model.ConnectycubeDialogType
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.getPrettyDate
import com.connectycube.messenger.utilities.loadChatDialogPhoto

class ChatDialogAdapter(private val context: Context) :
    ListAdapter<ConnectycubeChatDialog, ChatDialogAdapter.ChatDialogViewHolder>(ChatDialogDiffCallback()) {

    var callback: ChatDialogAdapterCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatDialogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_chat_dialog, parent, false)
        return ChatDialogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatDialogViewHolder, position: Int) {
        val chatDialog = getItem(position)
        holder.bind(context, chatDialog, View.OnClickListener { onChatDialogSelected(chatDialog) })
    }

    private fun onChatDialogSelected(chatDialog: ConnectycubeChatDialog) {
        callback!!.onChatDialogSelected(chatDialog)
    }


    override fun onCurrentListChanged(previousList: List<ConnectycubeChatDialog>, currentList: List<ConnectycubeChatDialog>) {
        callback!!.onChatDialogsListUpdated(currentList)
    }

    inner class ChatDialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val txtName: TextView = itemView.findViewById(R.id.name_text_viw)
        private val txtLastMessage: TextView = itemView.findViewById(R.id.last_message_text_view)
        private val txtUnreadMessagesCount: TextView = itemView.findViewById(R.id.unread_message_count_text_view)
        private val txtLastMessageDate: TextView = itemView.findViewById(R.id.last_masage_date_text_view)

        fun bind(activityContext: Context, chatDialog: ConnectycubeChatDialog, clickListener: View.OnClickListener) {
            loadChatDialogPhoto(activityContext,
                chatDialog.type == ConnectycubeDialogType.PRIVATE,
                chatDialog.photo,
                imgAvatar)

            txtName.text = chatDialog.name
            txtLastMessage.text = chatDialog.lastMessage

            setLastMessageDate(activityContext, txtLastMessageDate, chatDialog)

            if (chatDialog.unreadMessageCount != null && chatDialog.unreadMessageCount > 0) {
                txtUnreadMessagesCount.visibility = View.VISIBLE
                txtUnreadMessagesCount.text = chatDialog.unreadMessageCount.toString()
                setTextColor(activityContext, txtLastMessageDate, R.color.unread_messages_date)
            } else {
                txtUnreadMessagesCount.visibility = View.GONE
                setTextColor(activityContext, txtLastMessageDate, R.color.dark_grey)
            }

            itemView.setOnClickListener(clickListener)
        }

        private fun setLastMessageDate(
            activityContext: Context,
            textView: TextView,
            chatDialog: ConnectycubeChatDialog
        ) {
            var lastMessageDateSent: Long = chatDialog.lastMessageDateSent * 1000

            if (lastMessageDateSent == 0L) lastMessageDateSent = chatDialog.createdAt.time

            textView.text = getPrettyDate(activityContext, lastMessageDateSent)
        }

        private fun setTextColor(context: Context, textView: TextView, @ColorRes color: Int){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextColor(context.resources.getColor(color, context.theme))
            } else{
                textView.setTextColor(context.resources.getColor(color))
            }
        }
    }

    private class ChatDialogDiffCallback : DiffUtil.ItemCallback<ConnectycubeChatDialog>() {
        override fun areItemsTheSame(oldItem: ConnectycubeChatDialog, newItem: ConnectycubeChatDialog): Boolean {
            return oldItem.dialogId == newItem.dialogId
        }

        override fun areContentsTheSame(oldItem: ConnectycubeChatDialog, newItem: ConnectycubeChatDialog): Boolean {
            return oldItem.lastMessageDateSent == newItem.lastMessageDateSent
                    && oldItem.lastMessage == newItem.lastMessage
                    && oldItem.unreadMessageCount == newItem.unreadMessageCount
        }
    }

    interface ChatDialogAdapterCallback {
        fun onChatDialogSelected(chatDialog: ConnectycubeChatDialog)
        fun onChatDialogsListUpdated(currentList: List<ConnectycubeChatDialog>)
    }
}
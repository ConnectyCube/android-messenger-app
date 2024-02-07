package com.connectycube.messenger.adapters

import android.content.Context
import android.os.Build
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.getPrettyDate
import com.connectycube.messenger.utilities.loadChatDialogPhoto
import com.connectycube.messenger.utilities.setSingleOnClickListener
import com.connectycube.chat.models.ConnectycubeDialog
import com.connectycube.chat.models.ConnectycubeDialogType
import com.connectycube.messenger.utilities.CREATED_AT_SIMPLE_DATE_FORMAT
import java.text.SimpleDateFormat
import java.util.*

const val MENU_ITEM_DELETE: Int = 1

internal class ChatDialogAdapter(private val context: Context) :
    ListAdapter<ConnectycubeDialog, ChatDialogAdapter.ChatDialogViewHolder>(ChatDialogDiffCallback()) {

    var callback: ChatDialogAdapterCallback? = null

    override fun onViewRecycled(holder: ChatDialogViewHolder) {
        holder.unbind()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatDialogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_chat_dialog, parent, false)
        return ChatDialogViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatDialogViewHolder, position: Int) {
        val chatDialog = getItem(position)
        holder.bind(context,
            chatDialog, object : ChatDialogViewHolder.ChatDialogViewHolderCallback {
                override fun onItemDelete(chatDialog: ConnectycubeDialog) {
                    onChatDialogDelete(chatDialog)
                }

                override fun onItemSelected(chatDialog: ConnectycubeDialog) {
                    onChatDialogSelected(chatDialog)
                }
            })
    }

    private fun onChatDialogSelected(chatDialog: ConnectycubeDialog) {
        callback?.onChatDialogSelected(chatDialog)
    }

    private fun onChatDialogDelete(chatDialog: ConnectycubeDialog) {
        callback?.onChatDialogDelete(chatDialog)
    }

    override fun onCurrentListChanged(
        previousList: List<ConnectycubeDialog>,
        currentList: List<ConnectycubeDialog>
    ) {
        callback?.onChatDialogsListUpdated(currentList)
    }

    internal class ChatDialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {
        private var model: ConnectycubeDialog? = null
        private var callback: ChatDialogViewHolderCallback? = null
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val txtName: TextView = itemView.findViewById(R.id.name_text_viw)
        private val txtLastMessage: TextView = itemView.findViewById(R.id.last_message_text_view)
        private val txtUnreadMessagesCount: TextView = itemView.findViewById(R.id.unread_message_count_text_view)
        private val txtLastMessageDate: TextView = itemView.findViewById(R.id.last_masage_date_text_view)

        fun bind(activityContext: Context, chatDialog: ConnectycubeDialog, callback: ChatDialogViewHolderCallback) {
            this.model = chatDialog
            this.callback = callback

            loadChatDialogPhoto(
                activityContext,
                chatDialog.type == ConnectycubeDialogType.Companion.PRIVATE,
                chatDialog.photo,
                imgAvatar
            )

            txtName.text = chatDialog.name
            txtLastMessage.text = chatDialog.lastMessage

            setLastMessageDate(activityContext, txtLastMessageDate, chatDialog)

            if (chatDialog.unreadMessageCount != null && chatDialog.unreadMessageCount!! > 0) {
                txtUnreadMessagesCount.visibility = View.VISIBLE
                txtUnreadMessagesCount.text = chatDialog.unreadMessageCount.toString()
                setTextColor(activityContext, txtLastMessageDate, R.color.unread_messages_date)
            } else {
                txtUnreadMessagesCount.visibility = View.GONE
                setTextColor(activityContext, txtLastMessageDate, R.color.dark_grey)
            }

            itemView.setSingleOnClickListener { notifyItemSelected() }
            itemView.setOnCreateContextMenuListener(this)
        }

        fun unbind() {
            model = null
            callback = null
        }

        private fun setLastMessageDate(
            activityContext: Context,
            textView: TextView,
            chatDialog: ConnectycubeDialog
        ) {
            var lastMessageDateSent: Long = (chatDialog.lastMessageDateSent ?: 0) * 1000

            if (lastMessageDateSent == 0L) lastMessageDateSent = SimpleDateFormat(CREATED_AT_SIMPLE_DATE_FORMAT, Locale.getDefault()).parse(chatDialog.createdAt).time

            textView.text = getPrettyDate(activityContext, lastMessageDateSent)
        }

        private fun setTextColor(context: Context, textView: TextView, @ColorRes color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setTextColor(context.resources.getColor(color, context.theme))
            } else {
                textView.setTextColor(context.resources.getColor(color))
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete)?.setOnMenuItemClickListener(this)
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when (item?.itemId) {
                MENU_ITEM_DELETE -> notifyItemDelete()
            }
            return true
        }

        private fun notifyItemDelete() {
            model?.let { callback?.onItemDelete(it) }
        }

        private fun notifyItemSelected() {
            model?.let { callback?.onItemSelected(it) }
        }

        internal interface ChatDialogViewHolderCallback {
            fun onItemSelected(chatDialog: ConnectycubeDialog)
            fun onItemDelete(chatDialog: ConnectycubeDialog)
        }
    }

    private class ChatDialogDiffCallback : DiffUtil.ItemCallback<ConnectycubeDialog>() {
        override fun areItemsTheSame(oldItem: ConnectycubeDialog, newItem: ConnectycubeDialog): Boolean {
            return oldItem.dialogId == newItem.dialogId
        }

        override fun areContentsTheSame(oldItem: ConnectycubeDialog, newItem: ConnectycubeDialog): Boolean {
            return oldItem.lastMessageDateSent == newItem.lastMessageDateSent
                    && oldItem.lastMessage == newItem.lastMessage
                    && oldItem.unreadMessageCount == newItem.unreadMessageCount
                    && oldItem.name == newItem.name
                    && oldItem.updatedAt == newItem.updatedAt
        }
    }

    interface ChatDialogAdapterCallback {
        fun onChatDialogSelected(chatDialog: ConnectycubeDialog)
        fun onChatDialogsListUpdated(currentList: List<ConnectycubeDialog>)
        fun onChatDialogDelete(chatDialog: ConnectycubeDialog)
    }
}
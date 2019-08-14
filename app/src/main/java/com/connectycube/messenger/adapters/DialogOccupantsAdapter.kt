package com.connectycube.messenger.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.getPrettyLastActivityDate
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.users.model.ConnectycubeUser

class DialogOccupantsAdapter(
    private val context: Context,
    private val callback: DialogOccupantsAdapterCallback
) : RecyclerView.Adapter<DialogOccupantsAdapter.DialogOccupantViewHolder>() {

    private var items: List<ConnectycubeUser> = mutableListOf()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogOccupantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_chat_dialog_occupant, parent, false)
        return DialogOccupantViewHolder(view)
    }

    override fun onBindViewHolder(holder: DialogOccupantViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(
            context,
            user,
            isCurrentUser(user),
            isCreator(user),
            isAdmin(user)
        )
    }

    private fun isCurrentUser(user: ConnectycubeUser): Boolean {
        return callback.isCurrentUser(user)
    }

    private fun isCreator(user: ConnectycubeUser): Boolean {
        return callback.isUserCreator(user)
    }

    private fun isAdmin(user: ConnectycubeUser): Boolean {
        return callback.isUserAdmin(user)
    }

    private fun getItem(position: Int): ConnectycubeUser {
        return items[position]
    }

    fun setItems(users: List<ConnectycubeUser>) {
        items = users
        notifyDataSetChanged()
    }

    inner class DialogOccupantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val txtName: TextView = itemView.findViewById(R.id.name_text_viw)
        private val txtLastActivityTitle: TextView = itemView.findViewById(R.id.last_activity_title_txt)
        private val txtLastActivity: TextView = itemView.findViewById(R.id.last_activity_text_view)
        private val txtRole: TextView = itemView.findViewById(R.id.role_txt)

        fun bind(
            activityContext: Context,
            connectycubeUser: ConnectycubeUser,
            isCurrentUser: Boolean,
            isCreator: Boolean,
            isAdministrator: Boolean
        ) {
            loadUserAvatar(
                activityContext,
                connectycubeUser,
                imgAvatar
            )

            if (isCurrentUser) {
                txtName.text = activityContext.getText(R.string.you)
                txtLastActivity.visibility = View.GONE
                txtLastActivityTitle.visibility = View.GONE
            } else {
                txtName.text = connectycubeUser.fullName
                txtLastActivityTitle.visibility = View.VISIBLE
                txtLastActivity.visibility = View.VISIBLE
                txtLastActivity.text = getPrettyLastActivityDate(activityContext, connectycubeUser.lastRequestAt)
            }

            when {
                isCreator -> {
                    txtRole.visibility = View.VISIBLE
                    txtRole.text = activityContext.getText(R.string.creator)
                }
                isAdministrator -> {
                    txtRole.visibility = View.VISIBLE
                    txtRole.text = activityContext.getText(R.string.admin)
                }
                else -> txtRole.visibility = View.GONE
            }
        }
    }

    interface DialogOccupantsAdapterCallback {
        fun isUserCreator(user: ConnectycubeUser): Boolean
        fun isUserAdmin(user: ConnectycubeUser): Boolean
        fun isCurrentUser(user: ConnectycubeUser): Boolean
    }
}
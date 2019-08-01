package com.connectycube.messenger.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.getPrettyLastActivityDate
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.users.model.ConnectycubeUser

class CheckableUsersAdapter(
    private val context: Context,
    private val callback: CheckableUsersAdapterCallback
) : RecyclerView.Adapter<CheckableUsersAdapter.CheckableUserViewHolder>() {

    private var items: List<ConnectycubeUser> = mutableListOf()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_checkable_user, parent, false)
        return CheckableUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckableUserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(context, user, isUserChecked(user), CompoundButton.OnCheckedChangeListener { _, checked -> setUserChecked(user, checked) })
    }

    private fun isUserChecked(user: ConnectycubeUser): Boolean {
        return callback.isUserSelected(user)
    }

    private fun setUserChecked(user: ConnectycubeUser, checked: Boolean) {
        callback.onUserSelected(user, checked)
    }

    private fun getItem(position: Int): ConnectycubeUser {
        return items[position]
    }

    fun setItems(users: List<ConnectycubeUser>){
        items = users
        notifyDataSetChanged()
    }

    inner class CheckableUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val txtName: TextView = itemView.findViewById(R.id.name_text_viw)
        private val txtLastActivity: TextView = itemView.findViewById(R.id.last_activity_text_view)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

        fun bind(activityContext: Context, connectycubeUser: ConnectycubeUser, isSelected: Boolean, onCheckedChangeListener: CompoundButton.OnCheckedChangeListener) {
            loadUserAvatar(activityContext,
                connectycubeUser,
                imgAvatar)

            txtName.text = connectycubeUser.fullName
            txtLastActivity.text = getPrettyLastActivityDate(activityContext, connectycubeUser.lastRequestAt)

            checkBox.isChecked = isSelected
            checkBox.setOnCheckedChangeListener(onCheckedChangeListener)
        }
    }

    interface CheckableUsersAdapterCallback {
        fun onUserSelected(user: ConnectycubeUser, checked: Boolean)
        fun isUserSelected(user: ConnectycubeUser): Boolean
    }
}
package com.connectycube.messenger.adapters

import android.content.Context
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.R
import com.connectycube.messenger.utilities.CREATED_AT_SIMPLE_DATE_FORMAT
import com.connectycube.messenger.utilities.getPrettyLastActivityDate
import com.connectycube.messenger.utilities.loadUserAvatar
import com.connectycube.users.models.ConnectycubeUser
import java.text.SimpleDateFormat
import java.util.*

const val MENU_ITEM_ADMIN_ADD: Int = 0
const val MENU_ITEM_ADMIN_REMOVE: Int = 1
const val MENU_ITEM_OCCUPANT_REMOVE: Int = 2

typealias ClickListener = (ConnectycubeUser) -> Unit

internal class DialogOccupantsAdapter(
    private val context: Context,
    private val callback: DialogOccupantsAdapterCallback,
    private val listener: ClickListener
) : RecyclerView.Adapter<DialogOccupantsAdapter.DialogOccupantViewHolder>() {

    private var items: List<ConnectycubeUser> = mutableListOf()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogOccupantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_chat_dialog_occupant, parent, false)
        return DialogOccupantViewHolder(view)
    }

    override fun onViewRecycled(holder: DialogOccupantViewHolder) {
        holder.unbind()
    }

    override fun onBindViewHolder(holder: DialogOccupantViewHolder, position: Int) {
        val user = getItem(position)
        with(holder) {
            bind(
                context,
                user,
                object : DialogOccupantViewHolder.DialogOccupantViewHolderCallback {
                    override fun isUserCreator(connectycubeUser: ConnectycubeUser): Boolean {
                        return isCreator(connectycubeUser)
                    }

                    override fun isUserAdmin(connectycubeUser: ConnectycubeUser): Boolean {
                        return isAdmin(connectycubeUser)
                    }

                    override fun isCurrentUser(connectycubeUser: ConnectycubeUser): Boolean {
                        return isUserCurrentUser(connectycubeUser)
                    }

                    override fun onItemAddAdmin(connectycubeUser: ConnectycubeUser) {
                        onAddUserToAdmins(connectycubeUser.id)
                    }

                    override fun onItemRemoveAdmin(connectycubeUser: ConnectycubeUser) {
                        onRemoveUserFromAdmins(connectycubeUser.id)
                    }

                    override fun onItemRemoveOccupant(connectycubeUser: ConnectycubeUser) {
                        onRemoveUserFromOccupants(connectycubeUser.id)
                    }
                },
                getCurrentUser()
            )
            itemView.setOnClickListener { listener(user) }
        }
    }

    private fun getCurrentUser(): ConnectycubeUser {
        return callback.getCurrentUser()
    }

    private fun isUserCurrentUser(user: ConnectycubeUser): Boolean {
        return callback.isCurrentUser(user)
    }

    private fun isCreator(user: ConnectycubeUser): Boolean {
        return callback.isUserCreator(user)
    }

    private fun isAdmin(user: ConnectycubeUser): Boolean {
        return callback.isUserAdmin(user)
    }

    private fun onAddUserToAdmins(userId: Int) {
        callback.onAddUserToAdmins(userId)
    }

    private fun onRemoveUserFromAdmins(userId: Int) {
        callback.onRemoveUserFromAdmins(userId)
    }

    private fun onRemoveUserFromOccupants(userId: Int) {
        callback.onRemoveUserFromOccupants(userId)
    }

    private fun getItem(position: Int): ConnectycubeUser {
        return items[position]
    }

    fun setItems(users: List<ConnectycubeUser>) {
        items = users
        notifyDataSetChanged()
    }

    internal class DialogOccupantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {
        private var model: ConnectycubeUser? = null
        private var callback: DialogOccupantViewHolderCallback? = null
        private var currentUser: ConnectycubeUser? = null
        private val imgAvatar: ImageView = itemView.findViewById(R.id.avatar_image_view)
        private val txtName: TextView = itemView.findViewById(R.id.name_text_viw)
        private val txtLastActivityTitle: TextView = itemView.findViewById(R.id.last_activity_title_txt)
        private val txtLastActivity: TextView = itemView.findViewById(R.id.last_activity_text_view)
        private val txtRole: TextView = itemView.findViewById(R.id.role_txt)
        private val btnContextMenu: ImageView = itemView.findViewById(R.id.context_menu_img)

        fun bind(
            activityContext: Context,
            connectycubeUser: ConnectycubeUser,
            callback: DialogOccupantViewHolderCallback,
            currentUser: ConnectycubeUser
        ) {
            this.model = connectycubeUser
            this.callback = callback
            this.currentUser = currentUser

            loadUserAvatar(
                activityContext,
                connectycubeUser,
                imgAvatar
            )

            if (isCurrentUser()) {
                txtName.text = activityContext.getText(R.string.you)
                txtLastActivity.visibility = View.GONE
                txtLastActivityTitle.visibility = View.GONE
            } else {
                txtName.text = connectycubeUser.fullName
                txtLastActivityTitle.visibility = View.VISIBLE
                txtLastActivity.visibility = View.VISIBLE
                txtLastActivity.text =
                    getPrettyLastActivityDate(activityContext, if(connectycubeUser.lastRequestAt != null ) SimpleDateFormat(
                        CREATED_AT_SIMPLE_DATE_FORMAT, Locale.getDefault()).parse(connectycubeUser.lastRequestAt) else null ?: Date())
            }

            when {
                isCreator() -> {
                    txtRole.visibility = View.VISIBLE
                    txtRole.text = activityContext.getText(R.string.creator)
                }
                isAdministrator() -> {
                    txtRole.visibility = View.VISIBLE
                    txtRole.text = activityContext.getText(R.string.admin)
                }
                else -> txtRole.visibility = View.GONE
            }

            if (isCurrentUserCreatorOrAdministrator() && !isCreator() && !isCurrentUser()) {
                itemView.setOnCreateContextMenuListener(this)
                btnContextMenu.setOnClickListener { itemView.showContextMenu() }
                itemView.setOnLongClickListener { itemView.showContextMenu() }
                btnContextMenu.visibility = View.VISIBLE
            } else {
                btnContextMenu.visibility = View.GONE
            }
        }

        private fun isAdministrator(): Boolean {
            if (model == null) return false

            return callback?.isUserAdmin(model!!)!!
        }

        private fun isCurrentUser(): Boolean {
            if (model == null) return false

            return callback?.isCurrentUser(model!!)!!
        }

        private fun isCreator(): Boolean {
            if (model == null) return false

            return callback?.isUserCreator(model!!)!!
        }

        private fun isCurrentUserCreator(): Boolean {
            return currentUser?.let { callback?.isUserCreator(it) }!!
        }

        private fun isCurrentUserCreatorOrAdministrator(): Boolean {
            return currentUser?.let { callback?.isUserCreator(it)!! || callback?.isUserAdmin(it)!! }!!
        }

        fun unbind() {
            model = null
            callback = null
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            if (isCurrentUserCreator()) {
                when {
                    isAdministrator() ->
                        menu?.add(Menu.NONE, MENU_ITEM_ADMIN_REMOVE, Menu.NONE, R.string.remove_from_admins)
                            ?.setOnMenuItemClickListener(this)
                    else ->
                        menu?.add(Menu.NONE, MENU_ITEM_ADMIN_ADD, Menu.NONE, R.string.add_to_admins)
                            ?.setOnMenuItemClickListener(this)
                }
            }

            if (isCurrentUserCreatorOrAdministrator()) {
                menu?.add(Menu.NONE, MENU_ITEM_OCCUPANT_REMOVE, Menu.NONE, R.string.remove_from_occupants)
                    ?.setOnMenuItemClickListener(this)
            }
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                MENU_ITEM_ADMIN_ADD -> notifyAddAdmin()
                MENU_ITEM_ADMIN_REMOVE -> notifyRemoveAdmin()
                MENU_ITEM_OCCUPANT_REMOVE -> notifyRemoveOccupant()
            }
            return true
        }

        private fun notifyRemoveOccupant() {
            model?.let { callback?.onItemRemoveOccupant(it) }

        }

        private fun notifyRemoveAdmin() {
            model?.let { callback?.onItemRemoveAdmin(it) }
        }

        private fun notifyAddAdmin() {
            model?.let { callback?.onItemAddAdmin(it) }
        }

        internal interface DialogOccupantViewHolderCallback {
            fun isUserCreator(connectycubeUser: ConnectycubeUser): Boolean
            fun isUserAdmin(connectycubeUser: ConnectycubeUser): Boolean
            fun isCurrentUser(connectycubeUser: ConnectycubeUser): Boolean
            fun onItemAddAdmin(connectycubeUser: ConnectycubeUser)
            fun onItemRemoveAdmin(connectycubeUser: ConnectycubeUser)
            fun onItemRemoveOccupant(connectycubeUser: ConnectycubeUser)
        }
    }

    interface DialogOccupantsAdapterCallback {
        fun isUserCreator(user: ConnectycubeUser): Boolean
        fun isUserAdmin(user: ConnectycubeUser): Boolean
        fun isCurrentUser(user: ConnectycubeUser): Boolean
        fun getCurrentUser(): ConnectycubeUser
        fun onAddUserToAdmins(userId: Int)
        fun onRemoveUserFromAdmins(userId: Int)
        fun onRemoveUserFromOccupants(userId: Int)
    }
}
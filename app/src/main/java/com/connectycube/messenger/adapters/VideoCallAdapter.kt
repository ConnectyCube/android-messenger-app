package com.connectycube.messenger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.connectycube.messenger.R
import com.connectycube.users.model.ConnectycubeUser
import com.connectycube.videochat.view.RTCSurfaceView

class VideoCallAdapter(private val users: ArrayList<ConnectycubeUser>, var itemHeight: Int) :
    RecyclerView.Adapter<VideoCallAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.list_item_call_opponent,
            parent,
            false
        )
        val viewHolder = ViewHolder(view)
        initViewHeight(viewHolder, itemHeight)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    fun initViewHeight(holder: ViewHolder, height: Int) {
        val params = holder.itemView.layoutParams
        params.height = height
        holder.itemView.layoutParams = params
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun getUsers(): List<ConnectycubeUser> {
        return users
    }

    fun add(item: ConnectycubeUser) {
        users.add(item)
        notifyItemRangeChanged(users.size - 1, users.size)
    }

    fun addAll(items: List<ConnectycubeUser>) {
        users.clear()
        users.addAll(items)
        notifyDataSetChanged()
    }

    fun removeItem(index: Int) {
        users.removeAt(index)
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, users.size)
    }


    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.name)
        var connectionStatus: TextView = itemView.findViewById(R.id.connection_status)
        var rtcView: RTCSurfaceView = itemView.findViewById(R.id.rtc_view)
        var userId: Int = 0

        fun bind(user: ConnectycubeUser) {
            userId = user.id
            name.text = user.fullName?: user.login
        }
    }
}
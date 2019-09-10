package com.connectycube.messenger

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.connectycube.messenger.adapters.VideoCallAdapter
import com.connectycube.users.model.ConnectycubeUser
import com.connectycube.videochat.BaseSession
import com.connectycube.videochat.RTCSession
import com.connectycube.videochat.callbacks.RTCClientVideoTracksCallback
import com.connectycube.videochat.view.RTCSurfaceView
import com.connectycube.videochat.view.RTCVideoTrack
import kotlinx.android.synthetic.main.fragment_video_call.*
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber
import java.util.*

private const val SPAN_COUNT = 2
private const val VIDEO_TRACK_INITIALIZE_DELAY = 500L

class VideoCallFragment :
    BaseCallFragment(R.layout.fragment_video_call, R.string.title_video_call),
    RTCClientVideoTracksCallback<RTCSession> {
    private lateinit var videoCallAdapter: VideoCallAdapter
    private val userViewHolders: HashMap<Int, VideoCallAdapter.ViewHolder> = HashMap()
    private var users: ArrayList<ConnectycubeUser> = ArrayList()
    private val mainHandler = Handler()
    private var isCameraFront = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    context!!,
                    R.color.black_60
                )
            )
        )
    }

    override fun onStart() {
        super.onStart()
        initVideoTrackListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseUserViewHolders()
        removeVideoTrackListener()
        releaseOpponentsViews()
    }

    private fun releaseUserViewHolders() {
        userViewHolders.clear()
    }

    private fun releaseOpponentsViews() {
        val layoutManager = recycler_view_opponents.layoutManager
        layoutManager?.let { manager ->
            val childCount = manager.childCount
            Timber.d("releaseOpponentsViews for  $childCount views")
            for (i in 0 until childCount) {
                val childView = manager.getChildAt(i)
                Timber.d(" release View for  $i, $childView")
                childView?.let {
                    val childViewHolder =
                        recycler_view_opponents.getChildViewHolder(it) as VideoCallAdapter.ViewHolder
                    childViewHolder.rtcView.release()
                }
            }
        }
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        opponents?.let {
            users.clear()
            users.addAll(opponents)
//            setUsersToAdapter()
        }
    }

    override fun initViews() {
        super.initViews()
        initRecyclerView()
        initAdapter()
    }

    private fun initRecyclerView() {
        recycler_view_opponents.setHasFixedSize(false)
        val layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        layoutManager.reverseLayout = false
        layoutManager.spanSizeLookup = SpanSizeLookupForCall().apply {
            isSpanIndexCacheEnabled = false
        }
        recycler_view_opponents.layoutManager = layoutManager
        recycler_view_opponents.itemAnimator = null

        recycler_view_opponents.viewTreeObserver.addOnGlobalLayoutListener(object :
                                                                               ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val height = recycler_view_opponents.height
                if (height != 0) {
                    updateAllViewHeight(height)
                    recycler_view_opponents.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    private fun updateViewSizeIfNeed() {
        val height = recycler_view_opponents.getHeight() / 2
            initCurrentUserViewHeight(height)
            videoCallAdapter.itemHeight = height

    }

    private fun initCurrentUserViewHeight(height: Int) {
        val holder = recycler_view_opponents.findViewHolderForAdapterPosition(0)
        if (holder is VideoCallAdapter.ViewHolder) {
            videoCallAdapter.initViewHeight(holder, height)
        }
    }

    private fun updateAllViewHeight(height: Int) {
        for (user in videoCallAdapter.getUsers()) {
            val holder = getViewHolderForUser(user.id)
            if (holder != null) videoCallAdapter.initViewHeight(holder, height)
        }
    }

    private fun getViewHolderForUser(userId: Int): VideoCallAdapter.ViewHolder? {
        var holder: VideoCallAdapter.ViewHolder? = userViewHolders[userId]
        if (holder == null) {
            holder = findHolder(userId)
            holder?.let {
                userViewHolders.put(userId, holder)
            }
        }
        return holder
    }

    private fun findHolder(userId: Int): VideoCallAdapter.ViewHolder? {
        val childCount = recycler_view_opponents.getChildCount()
        for (i in 0 until childCount) {
            val childView = recycler_view_opponents.getChildAt(i)
            val childViewHolder =
                recycler_view_opponents.getChildViewHolder(childView) as VideoCallAdapter.ViewHolder
            if (userId == childViewHolder.userId) {
                return childViewHolder
            }
        }
        return null
    }

    private fun initAdapter() {
        val itemHeight = itemHeight()
        val connectycubeUsers = ArrayList<ConnectycubeUser>()
        videoCallAdapter = VideoCallAdapter(connectycubeUsers, itemHeight)
        recycler_view_opponents.adapter = videoCallAdapter
    }

    private fun itemHeight(): Int {
        val displayMetrics = resources.displayMetrics
        return displayMetrics.heightPixels
    }

    private fun initVideoTrackListener() {
        currentSession?.apply {
            addVideoTrackCallbacksListener(this@VideoCallFragment)
        }
    }

    private fun removeVideoTrackListener() {
        currentSession?.apply {
            removeVideoTrackCallbacksListener(this@VideoCallFragment)
        }
    }

    ////////////////////////////  RTCClientVideoTracksCallback callbacks  ///////////////////
    override fun onLocalVideoTrackReceive(rtcSession: RTCSession, videoTrack: RTCVideoTrack) {
        Timber.d("onLocalVideoTrackReceive currentUser= $currentUser")
        setUserToAdapter(currentUser)
        mainHandler.postDelayed(
            Runnable { setViewCall(currentUser.id, videoTrack, false) },
            VIDEO_TRACK_INITIALIZE_DELAY
        )

    }

    override fun onRemoteVideoTrackReceive(session: RTCSession,
                                           videoTrack: RTCVideoTrack,
                                           userId: Int
    ) {
        Timber.d("onRemoteVideoTrackReceive userId= $userId")
        updateViewSizeIfNeed()
        setUserToAdapter(userId)
        mainHandler.postDelayed(
            { setViewCall(userId, videoTrack, true) },
            VIDEO_TRACK_INITIALIZE_DELAY
        )
    }

    private fun setUserToAdapter(user: ConnectycubeUser) {
        videoCallAdapter.add(user)
        recycler_view_opponents.requestLayout()
    }

    private fun setUserToAdapter(userId: Int) {
        val user = users.first { it.id == userId }
        setUserToAdapter(user)
    }

    private fun setViewCall(userId: Int, videoTrack: RTCVideoTrack, remoteRenderer: Boolean) {
        Timber.d("setViewCall userId= $userId")
        if (isCurrentSessionClosed()) {
            return
        }
        val itemHolder = getViewHolderForUser(userId)
        if (itemHolder != null) {
            val videoView = itemHolder.rtcView
            Timber.d("setViewCall initVideoView")
            initVideoView(videoView, videoTrack, remoteRenderer)
        }
    }

    private fun initVideoView(videoView: RTCSurfaceView, videoTrack: RTCVideoTrack,
                              remoteRenderer: Boolean
    ) {
        videoTrack.removeRenderer(videoTrack.renderer)
        videoTrack.addRenderer(videoView)
        if (!remoteRenderer) {
            updateVideoView(videoView, isCameraFront)
        }
        Timber.d(if (remoteRenderer) "remote" else "local", " track is rendering")
    }

    private fun updateVideoView(surfaceViewRenderer: SurfaceViewRenderer, mirror: Boolean) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL)
    }

    private fun updateVideoView(surfaceViewRenderer: SurfaceViewRenderer,
                                mirror: Boolean,
                                scalingType: RendererCommon.ScalingType
    ) {
        surfaceViewRenderer.setScalingType(scalingType)
        surfaceViewRenderer.setMirror(mirror)
        surfaceViewRenderer.requestLayout()
    }

    private fun isCurrentSessionClosed(): Boolean {
        currentSession?.let {
            return it.state == BaseSession.RTCSessionState.RTC_SESSION_CLOSED
        }
        return true
    }

    private inner class SpanSizeLookupForCall : GridLayoutManager.SpanSizeLookup() {

        override fun getSpanSize(position: Int): Int {
            val itemCount = videoCallAdapter.itemCount
            if (itemCount == 4) {
                return 1
            }
            if (itemCount == 3) {
                if (position % 3 > 0) {
                    return 1
                }
            }
            return 2
        }
    }
}
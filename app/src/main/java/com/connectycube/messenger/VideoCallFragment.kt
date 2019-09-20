package com.connectycube.messenger

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.connectycube.messenger.adapters.VideoCallAdapter
import com.connectycube.users.model.ConnectycubeUser
import com.connectycube.videochat.BaseSession
import com.connectycube.videochat.RTCCameraVideoCapturer
import com.connectycube.videochat.RTCClient
import com.connectycube.videochat.RTCSession
import com.connectycube.videochat.callbacks.RTCClientVideoTracksCallback
import com.connectycube.videochat.callbacks.RTCSessionEventsCallback
import com.connectycube.videochat.callbacks.RTCSessionStateCallback
import com.connectycube.videochat.view.RTCSurfaceView
import com.connectycube.videochat.view.RTCVideoTrack
import kotlinx.android.synthetic.main.fragment_video_call.*
import org.webrtc.CameraVideoCapturer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber
import java.util.*

private const val SPAN_COUNT = 2
private const val VIDEO_TRACK_INITIALIZE_DELAY = 500L

class VideoCallFragment :
    BaseCallFragment(R.layout.fragment_video_call, R.string.title_video_call),
    RTCClientVideoTracksCallback<RTCSession>, RTCSessionStateCallback<RTCSession>,
    RTCSessionEventsCallback {
    private lateinit var videoCallAdapter: VideoCallAdapter
    private val userViewHolders: HashMap<Int, VideoCallAdapter.ViewHolder> = HashMap()
    private var users: ArrayList<ConnectycubeUser> = ArrayList()
    private val cameraSwitchHandler = CameraSwitchHandler()
    private val mainHandler = Handler()
    private var isCameraFront = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    context!!,
                    R.color.grey_30
                )
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    override fun onStart() {
        super.onStart()
        initVideoTrackListener()
        initSessionListener()
        initRTCClientListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseUserViewHolders()
        removeVideoTrackListener()
        removeSessionListener()
        removeRTCClientListener()
        releaseUsersViews()
    }

    private fun releaseUserViewHolders() {
        userViewHolders.clear()
    }

    private fun releaseUsersViews() {
        val layoutManager = recycler_view_opponents.layoutManager
        layoutManager?.let { manager ->
            val childCount = manager.childCount
            Timber.d("releaseUsersViews for  $childCount views")
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

    private fun initButtons() {
        toggle_camera.setOnClickListener { switchCamera() }
        toggle_mute_camera.setOnClickListener { setMuteCamera(toggle_mute_camera.isChecked) }
        toggle_mute_mic.setOnClickListener { setAudioMute(toggle_mute_mic.isChecked) }
    }

    private fun switchCamera() {
        toggle_camera.isEnabled = false
        (currentSession?.mediaStreamManager?.videoCapturer as RTCCameraVideoCapturer)
            .switchCamera(cameraSwitchHandler)
    }

    private fun setMuteCamera(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localVideoTrack?.setEnabled(isEnabled)
        }
    }

    private fun setAudioMute(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localAudioTrack?.setEnabled(isEnabled)
        }
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        opponents?.let {
            users.clear()
            users.addAll(opponents)
        }
    }

    override fun initViews() {
        super.initViews()
        chronometerInCall = chronometer
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
        val height = if (videoCallAdapter.itemCount < 2) {
            recycler_view_opponents.height
        } else {
            recycler_view_opponents.height / 2
        }
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
        videoCallAdapter.itemHeight = height
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

    private fun initSessionListener() {
        currentSession?.apply {
            addSessionCallbacksListener(this@VideoCallFragment)
        }
    }

    private fun removeSessionListener() {
        currentSession?.apply {
            removeSessionCallbacksListener(this@VideoCallFragment)
        }
    }

    private fun initRTCClientListener() {
        RTCClient.getInstance(context).addSessionCallbacksListener(this@VideoCallFragment)
    }

    private fun removeRTCClientListener() {
        RTCClient.getInstance(context).removeSessionsCallbacksListener(this@VideoCallFragment)
    }

    //RTCClientVideoTracksCallback callbacks
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
        setUserToAdapter(userId)
        updateViewSizeIfNeed()
        mainHandler.postDelayed(
            { setViewCall(userId, videoTrack, true) },
            VIDEO_TRACK_INITIALIZE_DELAY
        )
    }

    //    RTCSessionConnectionCallbacks
    override fun onStateChanged(rtcSession: RTCSession, state: BaseSession.RTCSessionState) {

    }

    override fun onConnectedToUser(rtcSession: RTCSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_connected))
    }

    override fun onConnectionClosedForUser(rtcSession: RTCSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_closed))
        removeUserFromAdapter(userId)
    }

    override fun onDisconnectedFromUser(rtcSession: RTCSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_disconnected))
    }

    //RTCClientSessionUserCallbacks
    override fun onUserNotAnswer(session: RTCSession, userId: Int) {
        Timber.d("onUserNotAnswer= $userId")
    }

    override fun onCallRejectByUser(session: RTCSession,
                                    userId: Int,
                                    userInfo: Map<String, String>?
    ) {
        Timber.d("onCallRejectByUser= $userId")
    }

    override fun onCallAcceptByUser(session: RTCSession,
                                    userId: Int,
                                    userInfo: Map<String, String>?
    ) {
        Timber.d("onCallAcceptByUser= $userId")
    }

    override fun onReceiveHangUpFromUser(session: RTCSession,
                                         userId: Int,
                                         userInfo: Map<String, String>?
    ) {
        Timber.d("onReceiveHangUpFromUser user= $userId")
        val user = users.first { it.id == userId }
        Toast.makeText(
            context, "${user.fullName
                ?: user.login} " + getString(R.string.call_status_hang_up),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onSessionClosed(session: RTCSession) {
    }

    private fun setUserToAdapter(user: ConnectycubeUser) {
        videoCallAdapter.add(user)
        recycler_view_opponents.requestLayout()
    }

    private fun setUserToAdapter(userId: Int) {
        val user = users.first { it.id == userId }
        setUserToAdapter(user)
    }

    private fun removeUserFromAdapter(userId: Int) {
        val itemHolder = getViewHolderForUser(userId)
        if (itemHolder != null) {
            Timber.d("removeUserFromAdapter opponentsAdapter.removeItem")
            videoCallAdapter.removeItem(itemHolder.adapterPosition)
            userViewHolders.remove(userId)
        }
        updateViewSizeIfNeed()
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

    private fun setStatusUser(userId: Int, status: String) {
        val holder = findHolder(userId) ?: return
        holder.connectionStatus.text = status
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

    private inner class CameraSwitchHandler : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(isFront: Boolean) {
            toggle_camera.isEnabled = true
            isCameraFront = !isCameraFront

            val localView = getViewHolderForUser(currentUser.id)?.rtcView
            localView?.let {
                updateVideoView(it, isCameraFront)
            }
        }

        override fun onCameraSwitchError(err: String?) {
            Toast.makeText(
                context, getString(R.string.camera_switch_error),
                Toast.LENGTH_SHORT
            ).show()
            toggle_camera.isEnabled = true
        }
    }
}
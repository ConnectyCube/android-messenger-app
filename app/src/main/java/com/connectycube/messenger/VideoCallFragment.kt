package com.connectycube.messenger

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.connectycube.messenger.adapters.VideoCallAdapter
import com.connectycube.messenger.databinding.FragmentVideoCallBinding
import com.connectycube.users.models.ConnectycubeUser
import com.connectycube.webrtc.*
import com.connectycube.webrtc.callbacks.RTCSessionEventsCallback
import com.connectycube.webrtc.callbacks.RTCSessionStateCallback
import com.connectycube.webrtc.callbacks.VideoTracksCallback
import org.webrtc.CameraVideoCapturer
import org.webrtc.RendererCommon
import timber.log.Timber
import java.util.*

private const val SPAN_COUNT = 2
private const val VIDEO_TRACK_INITIALIZE_DELAY = 500L

class VideoCallFragment :
    BaseCallFragment(R.string.title_video_call),
    VideoTracksCallback<P2PSession>, RTCSessionStateCallback<P2PSession>,
    RTCSessionEventsCallback {

    val binding by lazy {
        _binding as FragmentVideoCallBinding
    }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)
        return _binding?.root
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
        val layoutManager = binding.recyclerViewOpponents.layoutManager
        layoutManager?.let { manager ->
            val childCount = manager.childCount
            Timber.d("releaseUsersViews for  $childCount views")
            for (i in 0 until childCount) {
                val childView = manager.getChildAt(i)
                Timber.d(" release View for  $i, $childView")
                childView?.let {
                    val childViewHolder =
                        binding.recyclerViewOpponents.getChildViewHolder(it) as VideoCallAdapter.ViewHolder
                    childViewHolder.rtcView.release()
                }
            }
        }
    }

    private fun initButtons() {
        binding.toggleCamera.setOnClickListener { switchCamera() }
        binding.toggleCamera.setOnClickListener { setMuteCamera(binding.toggleCamera.isChecked) }
        binding.toggleMuteMic.setOnClickListener { setMuteAudio(binding.toggleMuteMic.isChecked) }
    }

    private fun switchCamera() {
        binding.toggleCamera.isEnabled = false
        currentSession?.mediaStreamManager?.videoCapturer?.switchCamera(cameraSwitchHandler)
    }

    private fun setMuteCamera(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localVideoTrack?.enabled = isEnabled
        }
    }

    private fun setMuteAudio(isEnabled: Boolean) {
        currentSession?.apply {
            mediaStreamManager?.localAudioTrack?.enabled = isEnabled
        }
    }

    override fun initWithOpponents(opponents: List<ConnectycubeUser>?) {
        opponents?.let {
            users.clear()
            users.addAll(opponents)
        }
    }

    override fun textOpponentsNames(): TextView {
        return binding.outgoingView.textOpponentsNames
    }

    override fun layoutOutgoingView(): View {
        return binding.outgoingView.root
    }

    override fun buttonHangup(): View {
        return binding.callHangUp.buttonHangup
    }

    override fun initViews() {
        super.initViews()
        chronometerInCall = binding.chronometer
        initRecyclerView()
        initAdapter()
    }

    private fun initRecyclerView() {
        binding.recyclerViewOpponents.setHasFixedSize(false)
        val layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        layoutManager.reverseLayout = false
        layoutManager.spanSizeLookup = SpanSizeLookupForCall().apply {
            isSpanIndexCacheEnabled = false
        }
        binding.recyclerViewOpponents.layoutManager = layoutManager
        binding.recyclerViewOpponents.itemAnimator = null

        binding.recyclerViewOpponents.viewTreeObserver.addOnGlobalLayoutListener(object :
                                                                               ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val height = binding.recyclerViewOpponents.height
                if (height != 0) {
                    updateAllViewHeight(height)
                    binding.recyclerViewOpponents.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    private fun updateViewSizeIfNeed() {
        val height = if (videoCallAdapter.itemCount < 2) {
            binding.recyclerViewOpponents.height
        } else {
            binding.recyclerViewOpponents.height / 2
        }
        initCurrentUserViewHeight(height)
        videoCallAdapter.itemHeight = height
    }

    private fun initCurrentUserViewHeight(height: Int) {
        val holder = binding.recyclerViewOpponents.findViewHolderForAdapterPosition(0)
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
        val childCount = binding.recyclerViewOpponents.getChildCount()
        for (i in 0 until childCount) {
            val childView = binding.recyclerViewOpponents.getChildAt(i)
            val childViewHolder =
                binding.recyclerViewOpponents.getChildViewHolder(childView) as VideoCallAdapter.ViewHolder
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
        binding.recyclerViewOpponents.adapter = videoCallAdapter
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
            addSessionStateCallbacksListener(this@VideoCallFragment)
        }
    }

    private fun removeSessionListener() {
        currentSession?.apply {
            removeSessionStateCallbacksListener(this@VideoCallFragment)
        }
    }

    private fun initRTCClientListener() {
        P2PCalls.addSessionCallbacksListener(this@VideoCallFragment)
    }

    private fun removeRTCClientListener() {
        P2PCalls.removeSessionCallbacksListener(this@VideoCallFragment)
    }

    //RTCClientVideoTracksCallback callbacks
    override fun onLocalVideoTrackReceive(session: P2PSession, videoTrack: ConnectycubeVideoTrack) {
        Timber.d("onLocalVideoTrackReceive currentUser= $currentUser")
        setUserToAdapter(currentUser)
        mainHandler.postDelayed(
            Runnable { setViewCall(currentUser.id, videoTrack, false) },
            VIDEO_TRACK_INITIALIZE_DELAY
        )

    }

    override fun onRemoteVideoTrackReceive(session: P2PSession,
                                           videoTrack: ConnectycubeVideoTrack,
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

    override fun onStateChanged(rtcSession: P2PSession, state: BaseSession.RTCSessionState) {

    }

    override fun onConnectedToUser(session: P2PSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_connected))
    }

    override fun onConnectionClosedForUser(session: P2PSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_closed))
        removeUserFromAdapter(userId)
    }

    override fun onDisconnectedFromUser(session: P2PSession, userId: Int) {
        setStatusUser(userId, getString(R.string.status_disconnected))
    }

    //RTCClientSessionUserCallbacks
    override fun onUserNotAnswer(session: P2PSession, userId: Int) {
        Timber.d("onUserNotAnswer= $userId")
    }

    override fun onCallRejectByUser(session: P2PSession,
                                    opponentId: Int,
                                    userInfo: Map<String, String?>?
    ) {
        Timber.d("onCallRejectByUser= $opponentId")
    }

    override fun onCallAcceptByUser(session: P2PSession,
                                    opponentId: Int,
                                    userInfo: Map<String, String?>?
    ) {
        Timber.d("onCallAcceptByUser= $opponentId")
    }

    override fun onReceiveHangUpFromUser(session: P2PSession,
                                         opponentId: Int,
                                         userInfo: Map<String, String?>?
    ) {
        Timber.d("onReceiveHangUpFromUser user= $opponentId")
        val user = users.first { it.id == opponentId }
        Toast.makeText(
            context, "${user.fullName
                ?: user.login} " + getString(R.string.call_status_hang_up),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onSessionClosed(session: P2PSession) {
    }

    private fun setUserToAdapter(user: ConnectycubeUser) {
        videoCallAdapter.add(user)
        binding.recyclerViewOpponents.requestLayout()
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

    private fun setViewCall(userId: Int, videoTrack: ConnectycubeVideoTrack, remoteRenderer: Boolean) {
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

    private fun initVideoView(videoView: RTCSurfaceView, videoTrack: ConnectycubeVideoTrack,
                              remoteRenderer: Boolean
    ) {
        videoTrack.videoSink?.let { videoTrack.removeSink(it) }
        videoTrack.addSink(videoView.videoSink)
        if (!remoteRenderer) {
            updateVideoView(videoView, isCameraFront)
        }
        Timber.d(if (remoteRenderer) "remote" else "local", " track is rendering")
    }

    private fun updateVideoView(surfaceViewRenderer: RTCSurfaceView, mirror: Boolean) {
        updateVideoView(surfaceViewRenderer, mirror, RendererCommon.ScalingType.SCALE_ASPECT_FILL)
    }

    private fun updateVideoView(surfaceViewRenderer: RTCSurfaceView,
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
            binding.toggleCamera.isEnabled = true
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
            binding.toggleCamera.isEnabled = true
        }
    }
}
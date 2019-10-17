package com.connectycube.messenger.utilities

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.connectycube.messenger.R
import com.github.chrisbanes.photoview.PhotoView

class CircleRectPhotoView : PhotoView {

    private var firstAfterAnimation: Boolean = false
    private var transitionStarted: Boolean = false
    private val TAG = "CircleRectPhotoView"
    private var circleRadius: Int = 0
    private var cornerRadius: Float = 0.toFloat()

    private var bitmapRect: RectF? = null
    private var clipPath: Path? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.CircleRectPhotoView, 0, 0)
        init(a)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.CircleRectPhotoView, defStyleAttr, 0)
        init(a)
    }

    private fun init(a: TypedArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        if (a.hasValue(R.styleable.CircleRectPhotoView_circleRadius)) {
            circleRadius = a.getDimensionPixelSize(R.styleable.CircleRectPhotoView_circleRadius, 0)
            cornerRadius = circleRadius.toFloat()
        }

        clipPath = Path()

        a.recycle()
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //This event-method provides the real dimensions of this custom view.

        Log.d(TAG, "w = $w h = $h")

        bitmapRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        drawable ?: return

        if (width == 0 || height == 0) {
            return
        }

        clipPath!!.reset()
        if (transitionStarted || width == height) {
            clipPath!!.addRoundRect(bitmapRect, (height/2).toFloat(), (width/2).toFloat(), Path.Direction.CW)
        } else if (firstAfterAnimation){
            clipPath!!.addRoundRect(bitmapRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }

        canvas.clipPath(clipPath!!)
        super.onDraw(canvas)
    }

    fun setAnimationStarted(started: Boolean) {
        if (transitionStarted != started) {
            transitionStarted = started

            if (!transitionStarted){
                firstAfterAnimation = true
            }
        }
    }
}
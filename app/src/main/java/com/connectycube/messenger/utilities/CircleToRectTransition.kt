package com.connectycube.messenger.utilities

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.transition.Transition
import android.transition.TransitionValues
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.annotation.RequiresApi


@TargetApi(Build.VERSION_CODES.KITKAT)
class CircleToRectTransition : Transition() {

    companion object {
        private val TAG = CircleToRectTransition::class.java.simpleName
        private val BOUNDS = "viewBounds"
        private val PROPS = arrayOf(BOUNDS)
    }

    override fun getTransitionProperties(): Array<String> {
        return PROPS
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        val bounds = Rect()
        bounds.left = view.left
        bounds.right = view.right
        bounds.top = view.top
        bounds.bottom = view.bottom
        transitionValues.values[BOUNDS] = bounds
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun createAnimator(sceneRoot: ViewGroup,
                                startValues: TransitionValues?,
                                endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        if (startValues.view !is CircleRectPhotoView) {
            Log.w(
                CircleToRectTransition::class.java.simpleName,
                "transition view should be CircleRectPhotoView"
            )
            return null
        }

        val view = startValues.view as CircleRectPhotoView

        val startRect = startValues.values[BOUNDS] as Rect?
        val endRect = endValues.values[BOUNDS] as Rect?

        val animator: Animator

        //scale animator
        animator = animator(view,
            startRect!!.height(),
            startRect.width(),
            endRect!!.height(),
            endRect.width()
        )

        Log.i(TAG, "--------- start animation ------")
        Log.d(TAG, "start rect left = " + startRect!!.left)
        Log.d(TAG, "start rect top = " + startRect.top)
        Log.d(TAG, "end rect left = " + endRect!!.left)
        Log.d(TAG, "end rect top = " + endRect.top)

        //movement animators below
        //if some translation not performed fully, use it instead of start coordinate
        val startX = startRect.left + view.translationX
        val startY = startRect.top + view.translationY

        Log.w(TAG, "xFrom $startX")
        Log.w(TAG, "yFrom $startY")

        //somehow end rect returns needed value minus translation in case not finished transition available
        val moveXTo = (endRect.left + Math.round(view.translationX)).toFloat()
        val moveYTo = (endRect.top + Math.round(view.translationY)).toFloat()

        Log.w(TAG, "moveXTo $moveXTo")
        Log.w(TAG, "moveYTo $moveYTo")

        val moveXAnimator = ObjectAnimator.ofFloat(view, "x", startX, moveXTo)
        val moveYAnimator = ObjectAnimator.ofFloat(view, "y", startY, moveYTo)

        val animatorSet = AnimatorSet()
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
                view.setAnimationStarted(true)
            }

            override fun onAnimationEnd(animation: Animator?) {
                view.setAnimationStarted(false)
            }

            override fun onAnimationCancel(animation: Animator?) {
                view.setAnimationStarted(false)
            }

            override fun onAnimationStart(animation: Animator?) {
                view.setAnimationStarted(true)
            }

        })
        animatorSet.playTogether(animator, moveXAnimator, moveYAnimator)

        //prevent blinking when interrupt animation
        return NoPauseAnimator(animatorSet)
    }

    private fun animator(view : View, startHeight: Int, startWidth: Int, endHeight: Int, endWidth: Int): Animator {
        val circleRadius: Float = (view.width/2).toFloat()
        var cornerRadius: Float = circleRadius

        val animatorSet = AnimatorSet()

        val heightAnimator = ValueAnimator.ofInt(startHeight, endHeight)
        val widthAnimator = ValueAnimator.ofInt(startWidth, endWidth)

        heightAnimator.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = view.getLayoutParams()
            layoutParams.height = `val`

            view.setLayoutParams(layoutParams)
            requestLayoutSupport(view)
        }

        widthAnimator.addUpdateListener { valueAnimator ->
            val `val` = valueAnimator.animatedValue as Int
            val layoutParams = view.getLayoutParams()
            layoutParams.width = `val`

            view.setLayoutParams(layoutParams)
            requestLayoutSupport(view)
        }

        val radiusAnimator: ValueAnimator
        if (startWidth < endWidth) {
            radiusAnimator = ValueAnimator.ofFloat(circleRadius, 0f)
        } else {
            radiusAnimator = ValueAnimator.ofFloat(cornerRadius, circleRadius)
        }

        radiusAnimator.interpolator = AccelerateInterpolator()
        radiusAnimator.addUpdateListener { animator ->
            cornerRadius = animator.animatedValue as Float
        }

        animatorSet.playTogether(heightAnimator, widthAnimator, radiusAnimator)

        return animatorSet
    }

    private fun requestLayoutSupport(view: View) {
        val parent = view.getParent() as View
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.EXACTLY)
        parent.measure(widthSpec, heightSpec)
        parent.layout(parent.left, parent.top, parent.right, parent.bottom)
    }
}
package com.yuliyang.well_design.nested_scroll

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.OverScroller
import android.widget.Scroller
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.yly.nestedscroll.R
import kotlin.math.abs

/**
 * 带嵌套滑动的ViewGroup
 */
class NestedScrollParentLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), NestedScrollingParent2 {

    private var mTop: View? = null
    private var mTopViewHeight: Int = 0
    private var mScroller: OverScroller
    private var mVelocityTracker: VelocityTracker? = null
    private var mTouchSlop: Int = 0
    private var mMaximumVelocity: Int = 0
    private var mMinimumVelocity: Int = 0
    private var mLastY: Float = 0F

    init {
        orientation = VERTICAL
        mScroller = OverScroller(context)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mMaximumVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
        mMinimumVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mTop = findViewById(R.id.id_stickynavlayout_topview)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        mTopViewHeight = mTop?.measuredHeight ?: 0
        setMeasuredDimension(measuredWidth, mTopViewHeight + measuredHeight)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return true
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return true
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
    }

    override fun onStopNestedScroll(target: View, type: Int) {
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!mScroller.isFinished) {
            mScroller.abortAnimation()
        }
        //向上滑动且头部没有完全移除屏幕
        val hiddenTop = dy > 0 && scrollY < mTopViewHeight
        //向下滑动且头部没有完全移除屏幕
        val showTop = dy < 0 && scrollY >= 0 && !target.canScrollVertically(-1)
        if (hiddenTop || showTop) {
            //头部移动
            scrollBy(0, dy)
            consumed[1] = dy
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val scrollerFiled = NestedScrollView::class.java.getDeclaredField("mScroller")
                scrollerFiled.isAccessible = true
                (scrollerFiled.get(findViewById<NestedScrollView>(R.id.testScroll)) as OverScroller).abortAnimation()
                initVelocityTrackerIfNotExists()
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mLastY = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                mVelocityTracker!!.addMovement(event)
                val dy = y - mLastY
                scrollBy(0, (-dy).toInt())
                mLastY = y
            }
            MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
                mScroller.abortAnimation()
            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker?.apply {
                    computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                    fling(yVelocity.toInt())
                }
                recycleVelocityTracker()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun fling(velocityY: Int) {
        mScroller.fling(0, scrollY, 0, -velocityY, 0, 0, Int.MIN_VALUE, Int.MAX_VALUE)
    }

    override fun scrollTo(x: Int, y: Int) {
        var innerY = y
        if (innerY < 0) {
            innerY = 0
        }
        if (innerY > mTopViewHeight) {
            innerY = mTopViewHeight
        }
        super.scrollTo(x, innerY)
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.currY)
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }
}

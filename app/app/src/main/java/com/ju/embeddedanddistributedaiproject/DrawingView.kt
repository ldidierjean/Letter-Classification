package com.ju.embeddedanddistributedaiproject

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.util.Log

// View that lets the user draw onto it
class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPath: Path = Path()
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private val circlePaint: Paint = Paint()
    private val circlePath: Path = Path()

    init {
        circlePaint.isAntiAlias = true
        circlePaint.color = Color.BLACK
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeJoin = Paint.Join.ROUND
        circlePaint.strokeWidth = 60f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mBitmap?.eraseColor(Color.WHITE)
        mCanvas = Canvas(mBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint)
        canvas.drawPath(mPath, circlePaint!!)
        canvas.drawPath(circlePath, circlePaint)
    }

    private var mX = 0f
    private var mY = 0f

    private fun touchStart(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= 4f || dy >= 4f) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
            circlePath.reset()
            circlePath.addCircle(mX, mY, 30f, Path.Direction.CW)
        }
    }

    private fun touchUp() {
        mPath.lineTo(mX, mY)
        circlePath.reset()
        // commit the path to our offscreen
        mCanvas!!.drawPath(mPath, circlePaint!!)
        // kill this so we don't double draw
        mPath.reset()
    }

    // Handles touch input
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    // Wipes the drawing
    fun resetDrawing() {
        onSizeChanged(width, height, width, height)
        invalidate()
    }

    // Returns the drawing
    fun getBitmap() : Bitmap {return mBitmap!!}
}
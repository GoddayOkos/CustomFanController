package dev.decagon.godday.customfancontroller

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.StringRes
import androidx.core.content.withStyledAttributes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import dev.decagon.godday.customfancontroller.FanSpeed.*
import kotlin.math.cos
import kotlin.math.sin

private enum class FanSpeed(@StringRes val label: Int) {
    OFF(R.string.fan_off),
    LOW(R.string.fan_low),
    MEDIUM(R.string.fan_medium),
    HIGH(R.string.fan_high);

    fun next() = when (this) {
        OFF -> LOW
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> OFF
    }
}

private const val RADIUS_OFFSET_LABEL = 30
private const val RADIUS_OFFSET_INDICATOR = -35

class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0.0f               // Radius of the circle
    private var fanSpeed = OFF     // The active selection
    // Position variable which will be used to draw label and
    // indicator circle position
    private val pointPosition: PointF = PointF(0.0f, 0.0f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    // Properties to retrieve and cache the View attributes
    private var fanSpeedLowColor = 0
    private var fanSpeedMediumColor = 0
    private var fanSpeedMaxColor = 0

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.DialView) {
            fanSpeedLowColor = getColor(R.styleable.DialView_fanColor1, 0)
            fanSpeedMediumColor = getColor(R.styleable.DialView_fanColor2, 0)
            fanSpeedMaxColor = getColor(R.styleable.DialView_fanColor3, 0)
        }

        updateContentDescription()

        ViewCompat.setAccessibilityDelegate(this,
            object : AccessibilityDelegateCompat() {

                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val customClick = AccessibilityNodeInfoCompat
                        .AccessibilityActionCompat(AccessibilityNodeInfo.ACTION_CLICK,
                        context.getString(if (fanSpeed != HIGH) R.string.change
                        else R.string.reset))

                    info.addAction(customClick)
                }
        })
    }

    override fun performClick(): Boolean {
        if (super.performClick()) return true

        fanSpeed = fanSpeed.next()
        updateContentDescription()

        invalidate()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = (minOf(w, h) / 2.0 * 0.8).toFloat()
    }

    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
        // Angles are in radians
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Set dial background color to green if selection not off
        paint.color = when (fanSpeed) {
            OFF -> Color.GRAY
            LOW -> fanSpeedLowColor
            MEDIUM -> fanSpeedMediumColor
            HIGH -> fanSpeedMaxColor
        }

        // Draw the dial
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(),
        radius, paint)

        // Draw the indicator circle
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius)
        paint.color = Color.BLACK
        canvas.drawCircle(pointPosition.x, pointPosition.y,
            radius/12, paint)

        // Draw the text label
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in values()) {
            pointPosition.computeXYForSpeed(i, labelRadius)
            val label = resources.getString(i.label)
            canvas.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }

    private fun updateContentDescription() {
        contentDescription = resources.getString(fanSpeed.label)
    }
}
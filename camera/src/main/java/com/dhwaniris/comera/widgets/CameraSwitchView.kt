package com.dhwaniris.comera.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageButton
import com.dhwaniris.comera.R


class CameraSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs) {

    private var frontCameraDrawable: Drawable? = null
    private var rearCameraDrawable: Drawable? = null
    private var padding = 5

    init {
        initializeView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs)

    private fun initializeView() {
        val context = context
        frontCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_front_white_24dp)
        frontCameraDrawable = DrawableCompat.wrap(frontCameraDrawable!!)
        DrawableCompat.setTintList(
            frontCameraDrawable!!.mutate(),
            ContextCompat.getColorStateList(context, R.color.switch_camera_mode_selector)
        )

        rearCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_rear_white_24dp)
        rearCameraDrawable = DrawableCompat.wrap(rearCameraDrawable!!)
        DrawableCompat.setTintList(
            rearCameraDrawable!!.mutate(),
            ContextCompat.getColorStateList(context, R.color.switch_camera_mode_selector)
        )

        setBackgroundResource(R.drawable.circle_frame_background_dark)
        displayBackCamera()

        padding = padding.dipToPixels(context)
        setPadding(padding, padding, padding, padding)

        displayBackCamera()
    }

    fun displayFrontCamera() {
        setImageDrawable(frontCameraDrawable)
    }

    fun displayBackCamera() {
        setImageDrawable(rearCameraDrawable)
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (Build.VERSION.SDK_INT > 10) {
            alpha = if (enabled) {
                1f
            } else {
                0.5f
            }
        }
    }
}

fun Int.dipToPixels(context: Context): Int {
    val resources = context.resources
    val px = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
        resources.displayMetrics
    )
    return px.toInt()
}
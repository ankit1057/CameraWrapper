package com.dhwaniris.comera.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.TypedValue
import com.dhwaniris.comera.R


class CameraSwitchView @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {

  private var frontCameraDrawable: Drawable? = null
  private var rearCameraDrawable: Drawable? = null
  private var padding = 5

  init {
    initializeView()
  }

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : this(context, attrs) {}

  private fun initializeView() {
    val context = context
    frontCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_front_white_24dp)

    rearCameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera_rear_white_24dp)

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
  val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
      resources.displayMetrics)
  return px.toInt()
}
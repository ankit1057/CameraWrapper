package com.dhwaniris.comera.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.dhwaniris.comera.R


class FlashSwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs) {

    private val flashOnDrawable: Drawable?
    private val flashOffDrawable: Drawable?
    private val flashAutoDrawable: Drawable?

    init {
        flashOnDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_on_white_24dp)
        flashOffDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_off_white_24dp)
        flashAutoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_auto_white_24dp)
        init()
    }

    private fun init() {
        setBackgroundColor(Color.TRANSPARENT)
        displayFlashOff()
    }

    fun displayFlashOff() {
        setImageDrawable(flashOffDrawable)
    }

    fun displayFlashOn() {
        setImageDrawable(flashOnDrawable)
    }

    fun displayFlashAuto() {
        setImageDrawable(flashAutoDrawable)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
    }

}
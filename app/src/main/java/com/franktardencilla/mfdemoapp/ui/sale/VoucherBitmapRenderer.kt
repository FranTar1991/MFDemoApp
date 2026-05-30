package com.franktardencilla.mfdemoapp.ui.sale

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View

object VoucherBitmapRenderer {
    fun render(view: View): Bitmap {
        val width = view.width.takeIf { it > 0 } ?: DEFAULT_PRINT_WIDTH
        val height = view.height.takeIf { it > 0 } ?: view.measuredHeight.takeIf { it > 0 } ?: DEFAULT_PRINT_HEIGHT
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return bitmap
    }

    fun renderForPrint(view: View): Bitmap {
        val receiptBitmap = render(view)
        val printBitmap = Bitmap.createBitmap(
            receiptBitmap.width,
            receiptBitmap.height + CUT_SPACE_HEIGHT,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(printBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(receiptBitmap, 0f, 0f, null)
        return printBitmap
    }

    private const val DEFAULT_PRINT_WIDTH = 384
    private const val DEFAULT_PRINT_HEIGHT = 640
    private const val CUT_SPACE_HEIGHT = 120
}

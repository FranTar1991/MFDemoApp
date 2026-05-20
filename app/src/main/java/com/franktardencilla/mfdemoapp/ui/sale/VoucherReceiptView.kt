package com.franktardencilla.mfdemoapp.ui.sale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.franktardencilla.mfdemoapp.R

class VoucherReceiptView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val receiptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface_border)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val tearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.app_background)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = sp(13f)
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val boldTextPaint = Paint(textPaint).apply {
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.MONOSPACE,
            android.graphics.Typeface.BOLD
        )
    }
    private val mutedTextPaint = Paint(textPaint).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = sp(11f)
    }
    private val statusPaint = Paint(boldTextPaint).apply {
        textSize = sp(18f)
        textAlign = Paint.Align.CENTER
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface_border)
        strokeWidth = 2f
    }

    private var voucher = VoucherUiModel.empty()
    private val contentPadding = dp(20f)
    private val topPadding = dp(32f)
    private val bottomPadding = dp(32f)
    private val lineHeight = dp(26f)
    private val tearRadius = dp(3f)

    fun setVoucher(newVoucher: VoucherUiModel) {
        voucher = newVoucher
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec).takeIf { it > 0 } ?: dp(320f).toInt()
        val desiredHeight = topPadding +
            lineHeight * calculateLineCount(measuredWidth) +
            bottomPadding
        val measuredHeight = resolveSize(desiredHeight.toInt(), heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = 4f
        val top = 4f
        val right = width - 4f
        val bottom = height - 4f
        val receipt = RectF(left, top, right, bottom)

        canvas.drawRoundRect(receipt, 10f, 10f, receiptPaint)
        canvas.drawRoundRect(receipt, 10f, 10f, borderPaint)
        drawTearEdge(canvas, left, right, top)
        drawTearEdge(canvas, left, right, bottom)

        var y = topPadding
        drawCentered(canvas, voucher.merchantName, y, boldTextPaint)
        y += lineHeight
        drawCentered(canvas, "TERMINAL ${voucher.terminalId}", y, mutedTextPaint)
        y += lineHeight
        drawCentered(canvas, "MERCHANT ${voucher.merchantId}", y, mutedTextPaint)
        y += lineHeight
        drawDivider(canvas, y)
        y += lineHeight

        drawCentered(canvas, voucher.transactionName, y, boldTextPaint)
        y += lineHeight
        drawCentered(canvas, voucher.status, y, statusPaint)
        y += lineHeight
        drawDivider(canvas, y)
        y += lineHeight

        y = drawWrappedSingleLine(canvas, voucher.cardLine, y, textPaint)
        y = drawLeftRight(canvas, voucher.authorizationLine, voucher.invoiceLine, y, textPaint)
        y = drawLeftRight(canvas, voucher.referenceLine, voucher.dateLine, y, textPaint)
        drawDivider(canvas, y)
        y += lineHeight

        voucher.amountRows.forEach { row ->
            y = drawLeftRight(
                canvas = canvas,
                leftText = row.label,
                rightText = row.value,
                y = y,
                paint = if (row.isTotal) boldTextPaint else textPaint
            )
        }

        drawDivider(canvas, y)
        y += lineHeight
        y = drawWrappedSingleLine(canvas, voucher.responseLine, y, mutedTextPaint)
        if (voucher.verificationLine.isNotBlank()) {
            y = drawWrappedCentered(canvas, voucher.verificationLine, y, boldTextPaint)
        }
        drawCentered(canvas, voucher.copyLine, y, boldTextPaint)
    }

    private fun drawTearEdge(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float
    ) {
        var x = left + tearRadius
        while (x < right) {
            canvas.drawCircle(x, y, tearRadius, tearPaint)
            x += tearRadius * 2
        }
    }

    private fun drawDivider(canvas: Canvas, y: Float) {
        canvas.drawLine(contentPadding, y - dp(10f), width - contentPadding, y - dp(10f), dividerPaint)
    }

    private fun drawWrappedSingleLine(
        canvas: Canvas,
        text: String,
        y: Float,
        paint: Paint
    ): Float {
        var nextY = y
        wrapText(text, paint, width - contentPadding * 2).forEach { line ->
            canvas.drawText(line, contentPadding, nextY, paint)
            nextY += lineHeight
        }
        return nextY
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        y: Float,
        paint: Paint
    ) {
        val originalAlign = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            fitText(text, paint, width - contentPadding * 2),
            width / 2f,
            y,
            paint
        )
        paint.textAlign = originalAlign
    }

    private fun drawWrappedCentered(
        canvas: Canvas,
        text: String,
        y: Float,
        paint: Paint
    ): Float {
        val originalAlign = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        var nextY = y
        wrapText(text, paint, width - contentPadding * 2).forEach { line ->
            canvas.drawText(line, width / 2f, nextY, paint)
            nextY += lineHeight
        }
        paint.textAlign = originalAlign
        return nextY
    }

    private fun drawLeftRight(
        canvas: Canvas,
        leftText: String,
        rightText: String,
        y: Float,
        paint: Paint
    ): Float {
        val availableWidth = width - contentPadding * 2
        val gap = dp(12f)
        val fittedRight = fitText(rightText, paint, availableWidth)
        val fittedRightWidth = paint.measureText(fittedRight)
        val combinedWidth = paint.measureText(leftText) + gap + fittedRightWidth

        return if (combinedWidth <= availableWidth) {
            canvas.drawText(leftText, contentPadding, y, paint)
            canvas.drawText(
                fittedRight,
                width - contentPadding - fittedRightWidth,
                y,
                paint
            )
            y + lineHeight
        } else {
            canvas.drawText(fitText(leftText, paint, availableWidth), contentPadding, y, paint)
            val rightY = y + lineHeight
            canvas.drawText(
                fittedRight,
                width - contentPadding - fittedRightWidth,
                rightY,
                paint
            )
            rightY + lineHeight
        }
    }

    private fun fitText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        if (maxWidth <= 0f || paint.measureText(text) <= maxWidth) {
            return text
        }
        var fitted = text
        while (fitted.length > 1 && paint.measureText("$fitted...") > maxWidth) {
            fitted = fitted.dropLast(1)
        }
        return "$fitted..."
    }

    private fun calculateLineCount(measuredWidth: Int): Int {
        val availableWidth = measuredWidth - contentPadding * 2
        var count = 0
        count += 7
        count += wrapText(voucher.cardLine, textPaint, availableWidth).size
        count += leftRightLineCount(voucher.authorizationLine, voucher.invoiceLine, textPaint, availableWidth)
        count += leftRightLineCount(voucher.referenceLine, voucher.dateLine, textPaint, availableWidth)
        count += 1
        voucher.amountRows.forEach { row ->
            count += leftRightLineCount(
                row.label,
                row.value,
                if (row.isTotal) boldTextPaint else textPaint,
                availableWidth
            )
        }
        count += 1
        count += wrapText(voucher.responseLine, mutedTextPaint, availableWidth).size
        if (voucher.verificationLine.isNotBlank()) {
            count += wrapText(voucher.verificationLine, boldTextPaint, availableWidth).size
        }
        count += 1
        return count
    }

    private fun leftRightLineCount(
        leftText: String,
        rightText: String,
        paint: Paint,
        availableWidth: Float
    ): Int {
        val fittedRight = fitText(rightText, paint, availableWidth)
        val combinedWidth = paint.measureText(leftText) + dp(12f) + paint.measureText(fittedRight)
        return if (combinedWidth <= availableWidth) 1 else 2
    }

    private fun wrapText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }
        if (paint.measureText(text) <= maxWidth) {
            return listOf(text)
        }

        val lines = mutableListOf<String>()
        var currentLine = ""
        text.split(" ").forEach { word ->
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    lines += currentLine
                }
                currentLine = if (paint.measureText(word) <= maxWidth) {
                    word
                } else {
                    fitText(word, paint, maxWidth)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines += currentLine
        }
        return lines
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}

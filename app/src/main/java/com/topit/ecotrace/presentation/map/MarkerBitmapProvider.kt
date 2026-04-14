package com.topit.ecotrace.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.ColorInt
import com.yandex.runtime.image.ImageProvider

// ─────────────────────────────────────────────────────────────────────────────
//  pinMarkerBitmap — LocationOn-style pin for report markers
//  Draws a teardrop/pin shape: circle head + pointed bottom.
// ─────────────────────────────────────────────────────────────────────────────

fun pinMarkerBitmap(context: Context, @ColorInt fillColor: Int): ImageProvider {
    val d = context.resources.displayMetrics.density
    val w = (30 * d).toInt().coerceAtLeast(30)
    val h = (44 * d).toInt().coerceAtLeast(44)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = w / 2f
    val headR = w * 0.41f         // radius of circular head
    val headCy = headR + d * 2    // vertical center of circle

    // Build LocationOn-style path (circle + teardrop bottom)
    val pinPath = Path().apply {
        // Outer arc sweep + two lines meeting at the bottom tip
        val oval = RectF(cx - headR, headCy - headR, cx + headR, headCy + headR)
        // Start at bottom-left shoulder of circle
        moveTo(cx, h.toFloat() - d)          // bottom tip
        lineTo(cx - headR * 0.62f, headCy + headR * 0.78f)
        arcTo(oval, 145f, 250f, false)       // counter-clockwise around top
        lineTo(cx, h.toFloat() - d)          // back to tip
        close()
    }

    // Shadow (slight translate)
    canvas.save()
    canvas.translate(d * 0.8f, d * 1.5f)
    canvas.drawPath(pinPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44000000
        style = Paint.Style.FILL
    })
    canvas.restore()

    // Fill
    canvas.drawPath(pinPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    })

    // White stroke outline
    canvas.drawPath(pinPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = d * 2.2f
        strokeJoin = Paint.Join.ROUND
    })

    // White inner circle dot
    canvas.drawCircle(cx, headCy, headR * 0.34f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    })

    return ImageProvider.fromBitmap(bitmap)
}

// ─────────────────────────────────────────────────────────────────────────────
//  userLocationBitmap — pulsing green circle for the user's own position
// ─────────────────────────────────────────────────────────────────────────────

fun userLocationBitmap(context: Context): ImageProvider {
    val d = context.resources.displayMetrics.density
    val size = (52 * d).toInt().coerceAtLeast(52)

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f

    // Outer pulse ring (translucent green)
    canvas.drawCircle(cx, cy, cx - d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x3B059669.toInt()  // green, ~23 % opacity
        style = Paint.Style.FILL
    })

    // Middle ring border
    canvas.drawCircle(cx, cy, cx * 0.62f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF059669.toInt()  // solid brand green
        style = Paint.Style.FILL
    })

    // White border
    canvas.drawCircle(cx, cy, cx * 0.62f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = d * 3f
    })

    // White center dot
    canvas.drawCircle(cx, cy, d * 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    })

    return ImageProvider.fromBitmap(bitmap)
}

// ─────────────────────────────────────────────────────────────────────────────
//  clusterBitmap — cluster marker (brand teal circle with count placeholder)
// ─────────────────────────────────────────────────────────────────────────────

fun clusterBitmap(context: Context, @ColorInt fillColor: Int): ImageProvider {
    val d = context.resources.displayMetrics.density
    val px = (44 * d).toInt().coerceAtLeast(44)

    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = px / 2f

    // Shadow
    canvas.drawCircle(cx + d, cx + d * 1.5f, cx - d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    })

    // Fill
    canvas.drawCircle(cx, cx, cx - d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    })

    // White stroke
    canvas.drawCircle(cx, cx, cx - d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = d * 3f
    })

    // White dot
    canvas.drawCircle(cx, cx, d * 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    })

    return ImageProvider.fromBitmap(bitmap)
}

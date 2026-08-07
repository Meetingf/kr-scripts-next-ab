package com.krscripts.core.ui

import android.content.Context
import android.view.View
import coil3.load
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.request.error
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.krscripts.core.R
import com.krscripts.core.config.PathAnalysis

object IconHelper {
    fun applyIcon(
        context: Context,
        view: ShapeableImageView?,
        iconPath: String?,
        configPath: String,
        clip: String
    ) {
        if (view == null) return
        if (iconPath.isNullOrEmpty()) {
            view.visibility = View.GONE
            return
        } else {
            view.visibility = View.VISIBLE
        }

        val icon = if (iconPath.startsWith("http")) {
            iconPath
        } else PathAnalysis(context, configPath).resolveUri(iconPath)

        view.load(icon) {
            crossfade(true)
            error(R.drawable.baseline_broken_image_24)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }
        val shape = ShapeAppearanceModel
            .builder()

        if (clip == "circle") {
            shape.setAllCornerSizes(RelativeCornerSize(0.5f))
        } else {
            shape.setAllCorners(CornerFamily.ROUNDED, clip.toFloat())
        }
        view.shapeAppearanceModel = shape.build()
    }
}
package com.krscripts.core.ui

import android.content.Context
import android.view.View
import coil3.load
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.request.error
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import com.krscripts.core.R
import com.krscripts.core.model.ImageNode

class ListItemImage(
    context: Context,
    layoutId: Int,
    config: ImageNode
) : ListItemClickable(context, layoutId, config) {

    private val imageView = layout.findViewById<ShapeableImageView?>(R.id.image_item)
    private val progressBar = layout.findViewById<CircularProgressIndicator?>(R.id.progressBar)

    init {
        imageView?.load(config.image) {
            crossfade(true)
            error(R.drawable.baseline_broken_image_24)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.DISABLED)
            listener(onStart = {
                progressBar?.visibility = View.VISIBLE
            }, onSuccess = { _, _ ->
                progressBar?.visibility = View.GONE
            }, onError = { _, _ ->
                progressBar?.visibility = View.GONE
            })
        }

        val shape = ShapeAppearanceModel
            .builder()

        if (config.iconClip == "circle") {
            shape.setAllCornerSizes(RelativeCornerSize(0.5f))
        } else {
            shape.setAllCorners(CornerFamily.ROUNDED, config.iconClip.toFloat())
        }
        imageView?.shapeAppearanceModel = shape.build()
        imageView?.visibility = View.VISIBLE
    }
}
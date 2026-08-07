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
import com.krscripts.core.model.ClickableNode

open class ListItemClickable(
    context: Context,
    layoutId: Int,
    config: ClickableNode
) : ListItemView(context, layoutId, config) {
    protected var mOnClickListener: OnClickListener? = null
    protected var mOnLongClickListener: OnLongClickListener? = null
    protected var shortcutIconView: View? = layout.findViewById(R.id.kr_shortcut_icon)
    protected var iconView: ShapeableImageView? = layout.findViewById(R.id.kr_icon)

    fun setOnClickListener(onClickListener: OnClickListener): ListItemClickable {
        this.mOnClickListener = onClickListener

        return this
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener): ListItemClickable {
        this.mOnLongClickListener = onLongClickListener

        return this
    }

    fun triggerAction() {
        this.mOnClickListener?.onClick(this)
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary

        this.layout.setOnClickListener {
            this.mOnClickListener?.onClick(this)
        }
        if (this.key.isNotEmpty() && config.allowShortcut != false) {
            this.layout.setOnLongClickListener {
                this.mOnLongClickListener?.onLongClick(this)
                true
            }
            shortcutIconView?.visibility = View.VISIBLE
        } else {
            shortcutIconView?.visibility = View.GONE
        }
        if (iconView != null) {
            iconView?.visibility = View.GONE
            if (config.iconPath.isNotEmpty()) {
                val icon = if (config.iconPath.startsWith("http")) {
                    config.iconPath
                } else PathAnalysis(context, config.pageConfigDir).resolveUri(config.iconPath)

                iconView?.load(icon) {
                    crossfade(true)
                    error(R.drawable.baseline_broken_image_24)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    diskCachePolicy(CachePolicy.ENABLED)
                }
                val shape = ShapeAppearanceModel
                    .builder()

                if (config.iconClip == "circle") {
                    shape.setAllCornerSizes(RelativeCornerSize(0.5f))
                } else {
                    shape.setAllCorners(CornerFamily.ROUNDED, config.iconClip.toFloat())
                }
                iconView?.shapeAppearanceModel = shape.build()
                iconView?.visibility = View.VISIBLE
            }
        }
    }

    interface OnClickListener {
        fun onClick(listItemView: ListItemClickable)
    }

    interface OnLongClickListener {
        fun onLongClick(listItemView: ListItemClickable)
    }
}

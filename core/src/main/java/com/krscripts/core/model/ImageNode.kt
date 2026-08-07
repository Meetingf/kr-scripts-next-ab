package com.krscripts.core.model

class ImageNode(currentConfigXml: String): RunnableNode(currentConfigXml) {
    var image = ""
    var scale: String? = null
    var width: String? = null
    var height: String? = null
}
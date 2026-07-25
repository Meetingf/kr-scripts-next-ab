package com.krscripts.core.model

class NavNode(currentConfigXml: String) : NodeInfoBase(currentConfigXml) {
    val children: ArrayList<NodeInfoBase> = ArrayList()
}
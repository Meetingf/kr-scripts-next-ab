package com.krscripts.core.model

import java.io.Serializable

class ConfigNode : Serializable {
    var pageMenuOptions = ArrayList<PageMenuOption>()
    var pageMenuOptionsSh: String = ""
    var pageHandlerSh:  String = ""
    val content = ArrayList<NodeInfoBase>()
}
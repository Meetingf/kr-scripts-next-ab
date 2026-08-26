package com.krscripts.core.model

class ActionParamInfo {
    // 参数名：必需保持唯一
    var name: String? = null

    var title: String? = null

    var label: String? = null

    // 描述
    var desc: String? = null

    // 值
    var value: String? = null
    var valueShell: String? = null
    var valueFromShell: String? = null
    var maxLength = -1 // input only
    var type: String? = null
    var max: Int = Int.MAX_VALUE
    var min: Int = Int.MIN_VALUE
    var required: Boolean = false // 是否是必需的
    var readonly: Boolean = false
    var options: ArrayList<SelectItem>? = null
    var optionsFromShell: ArrayList<SelectItem>? = null
    var optionsSh = ""
    // 是否允许多选(options only)
    var multiple: Boolean = false
    // 是否支持
    var supported: Boolean = true
    // 文本框的水印（提示占位符）
    var placeholder: String = ""
    // 文件mime类型（仅限type=file有效）
    var mime: String = ""
    // 文件后缀（仅限type=file有效）
    var suffix: String = ""
    // 是否允许用户手动输入路径
    var editable: Boolean = false
    // 多个值的分隔符（仅限多选下拉）
    var separator: String = "\n"
    // 控制本参数显隐的父参数名：支持多个，用 "|" 连接，如 "mode|cam"（同时依赖，AND）
    var dependOn: String? = null
    // 需匹配的父参数值：按 dependOn 顺序用 "|" 分隔；每个位置内多个可取的值用 "," 分隔
    var dependValue: String? = null
    // 匹配方式：show（默认，匹配即显示）| hide（匹配即隐藏）；可按 dependOn 顺序用 "|" 分开声明
    var dependMode: String = "show"
    // 多条件组合逻辑：and（默认）| or/priority | priority-rtl | xor | nand
    var dependLogic: String = "and"
    // 无任何条件满足时的默认显隐：show（默认）| hide
    var dependDefault: String = "show"
    // 初始显隐状态：auto（默认，按 dependDefault）| show | hide
    var dependInitialState: String = "auto"
    // 取反全部条件（NOT）：true 时 show/hide 互换
    var dependNegate: Boolean = false
    // and 逻辑下的满足阈值百分比：-1(默认)=100%全部满足 | 0-100 满足该百分比
    var dependThreshold: Int = -1
    // 隐藏时是否仍把值传给脚本：true(默认)=隐藏也传值 | false=完全忽略
    var dependIncludeHidden: Boolean = true
    // 父参数隐藏时子参数是否级联隐藏：true(默认) | false=父隐藏仅该父不参与计算
    var dependCascade: Boolean = true
    // 不隐藏而是置灰+禁止交互：false(默认) | true=条件不满足时只读而不消失
    var dependReadonly: Boolean = false
}

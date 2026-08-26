package com.krscripts.core.ui.param

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.krscripts.core.R
import com.krscripts.core.databinding.KrParamRowBinding
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.SelectItem

class ParamLayoutRender(
    private var linearLayout: LinearLayout,
    private val context: FragmentActivity
) {
    private val renderers = mutableListOf<ParamRenderer>()

    // key = actionParamInfo.name
    private val rowViews = HashMap<String, View>()
    private val valueReaders = HashMap<String, () -> String>()
    private var currentParamInfos: ArrayList<ActionParamInfo> = ArrayList()
    private val visibilityState = HashMap<String, Boolean>()

    companion object {

        // Label is hidden in these params
        private val HIDE_LABEL_TYPES = setOf("bool", "checkbox", "switch")

        /**
         * 获取当前选中项索引（单选）
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): Int {
            var selectedIndex = -1

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            // TODO:这里可能有点争议
            if (actionParamInfo.value != null) {
                valList.add(actionParamInfo.value!!)
            }
            if (valList.isNotEmpty()) {
                for (j in valList.indices) {
                    for ((index, option) in options.withIndex()) {
                        if (option.value == valList[j]) {
                            selectedIndex = index
                            break
                        }
                    }
                    if (selectedIndex > -1)
                        break
                }
            }
            return selectedIndex
        }

        /**
         * 获取当前选中项索引（多选）
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): BooleanArray {
            val status = BooleanArray(options.size)
            val values = getParamValues(actionParamInfo)

            options.forEachIndexed { index, item ->
                status[index] = (values != null && values.contains(item.value))
            }
            return status
        }

        /**
         * 设置列表的选中状态
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun setParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): ArrayList<SelectItem> {
            val values = getParamValues(actionParamInfo)

            for (element in options) {
                element.selected = (values != null && values.contains(element.value))
            }
            return options
        }

        // 获取多选下拉的选中值列表
        fun getParamValues(actionParamInfo: ActionParamInfo): List<String>? {
            val value = actionParamInfo.valueFromShell ?: actionParamInfo.value
            val values = value?.split(actionParamInfo.separator)
            return values
        }
    }

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: FileChooserRender.FileChooserInterface?) {
        currentParamInfos = actionParamInfos
        rowViews.clear()
        valueReaders.clear()
        visibilityState.clear()

        for (actionParamInfo in actionParamInfos) {
            val options = actionParamInfo.optionsFromShell
            val render: ParamRenderer =
                if (options != null && actionParamInfo.type !in setOf("app", "packages")) {
                    // Picker
                    if (actionParamInfo.multiple) {
                        MultipleSelectRender(actionParamInfo, context)
                    } else {
                        SingleSelectRender(actionParamInfo, context)
                    }
                } else {
                    when (actionParamInfo.type) {
                        // CheckBox
                        "bool", "checkbox" -> CheckboxRender(actionParamInfo, context)
                        // Switch
                        "switch" -> SwitchRender(actionParamInfo, context)
                        // SeekBar
                        "seekbar" -> SliderRender(actionParamInfo, context)
                        // FileSelector
                        "file", "folder" -> FileChooserRender(actionParamInfo, context, fileChooser)
                        // AppsSelector
                        "app", "packages" -> AppChooserRender(actionParamInfo, context)
                        // ColorPicker
                        "color" -> ColorPickerRender(actionParamInfo, context)

                        else -> {
                            // EditText
                            EditTextRender(actionParamInfo, context)
                        }
                    }
                }

            addRender(actionParamInfo, render)
        }

        // 初始化依赖显隐状态并做首次求值
        initializeDependencyStates()
        evaluateDependencies()
    }

    private fun addRender(
        actionParamInfo: ActionParamInfo,
        render: ParamRenderer
    ) {
        val view = render.render()
        addToLayout(view, actionParamInfo)
        renderers.add(render)

        // 为依赖求值登记当前值的读取方式
        actionParamInfo.name?.let { name ->
            valueReaders[name] = { render.getValue() ?: "" }
        }
        // 控件值变化时联动重新求值依赖
        attachChangeListeners(view)
    }

    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo) {
        val binding = KrParamRowBinding.inflate(LayoutInflater.from(context))
        with(binding) {
            // title
            val title = actionParamInfo.title
            krParamTitle.isVisible = !title.isNullOrEmpty()
            krParamTitle.text = title.orEmpty()

            // label
            val label = actionParamInfo.label
            val showLabel = !label.isNullOrEmpty() && !HIDE_LABEL_TYPES.contains(actionParamInfo.type)
            krParamLabel.isVisible = showLabel
            krParamLabel.text = label.orEmpty()
            krParamLabelDivier.isVisible = showLabel

            // desc
            val desc = actionParamInfo.desc
            krParamDesc.isVisible = !desc.isNullOrEmpty()
            krParamDesc.text = desc.orEmpty()

            krParamInput.addView(inputView)
            linearLayout.addView(root)

            (inputView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL
        }

        // 为依赖求值登记该参数对应的整行（用于显隐/置灰）
        actionParamInfo.name?.let { rowViews[it] = binding.root }
    }

    private fun getFieldTips(actionParamInfo: ActionParamInfo): String {
        return buildString {
            if (!actionParamInfo.title.isNullOrEmpty()) {
                append(actionParamInfo.title)
                append(" ")
            }
            if (!actionParamInfo.label.isNullOrEmpty()) {
                append(actionParamInfo.label)
                append(" ")
            }
            append("(")
            append(actionParamInfo.name)
            append(") ")
        }
    }

    fun readParamsValue(): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (renderer in renderers) {

            var value: String? = null
            try {
                value = renderer.getValue()
            } catch (e: Exception) {
                throw Exception(getFieldTips(renderer.actionParamInfo) + ": " + e.message)
            }

            val paramName = renderer.paramName
            val info = renderer.actionParamInfo

            if (value == null) continue

            info.value = value

            // ===== 依赖隐藏的取值处理 =====
            // depend-include-hidden=true(默认)：隐藏时仍传值（不检查 required，用户看不到无法输入）
            // depend-include-hidden=false：完全忽略隐藏参数
            val isHiddenByDepend = paramName?.let { visibilityState[it] == false } ?: false
            if (isHiddenByDepend) {
                if (info.dependIncludeHidden && value.isNotEmpty()) {
                    params[paramName] = value
                }
                continue
            }

            if (value.isEmpty() && info.required) {
                throw Exception(getFieldTips(info) + context.getString(R.string.do_not_empty))
            } else {
                paramName?.let { name ->
                    params[name] = value
                }
            }
        }
        return params
    }

    private val parenPattern = Regex("\\(([^()]*)\\)")

    // 控件值变化时联动重新求值依赖
    private fun attachChangeListeners(inputView: View) {
        when (inputView) {
            is MaterialSwitch ->
                inputView.setOnCheckedChangeListener { _, _ -> evaluateDependencies() }
            is CheckBox ->
                inputView.setOnCheckedChangeListener { _, _ -> evaluateDependencies() }
            is Slider ->
                inputView.addOnChangeListener { _, _, _ -> evaluateDependencies() }
            is EditText ->
                inputView.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) = evaluateDependencies()
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                })
            is ViewGroup -> {
                for (i in 0 until inputView.childCount) {
                    attachChangeListeners(inputView.getChildAt(i))
                }
            }
            else -> Unit
        }
    }

    // 初始化依赖显隐状态（避免打开对话框时参数闪烁）
    private fun initializeDependencyStates() {
        for (info in currentParamInfos) {
            val name = info.name ?: continue
            val initialState = info.dependInitialState.trim().lowercase()

            val initialVisibility = when (initialState) {
                "hide" -> false
                "show" -> true
                else -> info.dependDefault.trim().lowercase() != "hide"
            }

            val row = rowViews[name] ?: continue
            if (info.dependReadonly) {
                row.visibility = View.VISIBLE
                setRowInteractive(row, initialVisibility && !info.readonly)
            } else {
                row.visibility = if (initialVisibility) View.VISIBLE else View.GONE
            }
        }
    }

    // 多轮迭代求值依赖，支持"父隐藏则子隐藏"（cascade）的多级链
    private fun evaluateDependencies() {
        if (currentParamInfos.isEmpty()) return

        val previousState = HashMap(visibilityState)
        val working = HashMap<String, Boolean>()
        val maxPasses = currentParamInfos.size.coerceAtLeast(1).coerceAtMost(20)

        for (pass in 0 until maxPasses) {
            var changedThisPass = false
            for (info in currentParamInfos) {
                val name = info.name ?: continue
                val shouldShow = computeShouldShow(info, working)
                if (working[name] != shouldShow) {
                    working[name] = shouldShow
                    changedThisPass = true
                }
            }
            if (!changedThisPass) break
        }

        for (info in currentParamInfos) {
            val name = info.name ?: continue
            val shouldShow = working[name] ?: continue
            applyVisibility(name, shouldShow, previousState[name])
        }
    }

    // 根据当前依赖条件，判断某参数是否应显示
    private fun computeShouldShow(info: ActionParamInfo, working: HashMap<String, Boolean>): Boolean {
        val dependOnRaw = info.dependOn?.trim()

        if (dependOnRaw.isNullOrEmpty()) {
            return info.dependDefault.trim().lowercase() != "hide"
        }

        val dependOnList = dependOnRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (dependOnList.isEmpty()) {
            return info.dependDefault.trim().lowercase() != "hide"
        }

        // 父隐藏则子也隐藏（cascade）
        if (info.dependCascade && dependOnList.any { working[it] == false }) {
            return false
        }

        val dependValueList = (info.dependValue ?: "").split("|")
        val dependModeList = info.dependMode.split("|")

        fun evalCondition(i: Int): Pair<Boolean, Boolean>? {
            val parentName = dependOnList[i]

            // cascade=false：父正在隐藏则跳过该父，不参与本次计算
            if (!info.dependCascade && working[parentName] == false) {
                return null
            }

            val controllerInfo = currentParamInfos.find { it.name == parentName }
            val reader = valueReaders[parentName]
            if (controllerInfo == null || reader == null) return null

            val currentValues = reader().split(controllerInfo.separator)
                .map { it.trim() }.filter { it.isNotEmpty() }
            val parentOptions = controllerInfo.optionsFromShell ?: controllerInfo.options

            val currentIdentifiers = HashSet<String>()
            for (v in currentValues) {
                currentIdentifiers.addAll(buildValueIdentifiers(v, parentOptions))
            }

            val wantedRaw = dependValueList.getOrNull(i) ?: dependValueList.lastOrNull() ?: ""
            val wanted = wantedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matched = wanted.isEmpty() || wanted.any { matchesWanted(it, currentIdentifiers) }

            val mode = (dependModeList.getOrNull(i) ?: dependModeList.lastOrNull() ?: "show").trim()
            val wantShow = if (mode == "hide") !matched else matched
            return Pair(matched, wantShow)
        }

        val logic = info.dependLogic.trim().lowercase()
        return when (logic) {
            "priority", "or", "priority-ltr", "or-ltr" -> {
                var result = info.dependDefault.trim().lowercase() != "hide"
                for (i in dependOnList.indices) {
                    val (matched, wantShow) = evalCondition(i) ?: continue
                    if (matched) {
                        result = wantShow
                        break
                    }
                }
                result != info.dependNegate
            }
            "priority-rtl", "or-rtl" -> {
                var result = info.dependDefault.trim().lowercase() != "hide"
                for (i in dependOnList.indices.reversed()) {
                    val (matched, wantShow) = evalCondition(i) ?: continue
                    if (matched) {
                        result = wantShow
                        break
                    }
                }
                result != info.dependNegate
            }
            "xor" -> {
                var matchCount = 0
                for (i in dependOnList.indices) {
                    val (matched, _) = evalCondition(i) ?: continue
                    if (matched) matchCount++
                }
                (matchCount == 1) != info.dependNegate
            }
            "nand" -> {
                var result = true
                for (i in dependOnList.indices) {
                    val (_, wantShow) = evalCondition(i) ?: continue
                    if (!wantShow) {
                        result = false
                        break
                    }
                }
                !result != info.dependNegate
            }
            else -> {
                // "and"（默认）
                var satisfiedCount = 0
                var totalCount = 0
                for (i in dependOnList.indices) {
                    val (_, wantShow) = evalCondition(i) ?: continue
                    totalCount++
                    if (wantShow) satisfiedCount++
                }
                val threshold = if (info.dependThreshold < 0) {
                    totalCount
                } else {
                    (totalCount * info.dependThreshold / 100).coerceAtLeast(1)
                }
                val result = satisfiedCount >= threshold
                result != info.dependNegate
            }
        }
    }

    // 构建一个值及其可能的标识符（值本身、标题、括号内文本）用于模糊匹配
    private fun buildValueIdentifiers(value: String, options: ArrayList<SelectItem>?): Set<String> {
        val identifiers = HashSet<String>()
        identifiers.add(value)

        val title = options?.find { it.value == value }?.title?.trim()
        if (!title.isNullOrEmpty()) {
            identifiers.add(title)
            parenPattern.findAll(title).forEach { m ->
                val inner = m.groupValues[1].trim()
                if (inner.isNotEmpty()) {
                    identifiers.add(inner)
                    identifiers.add("(" + inner + ")")
                }
            }
        }
        return identifiers
    }

    // 匹配 depend-value，支持通配符 *（任意多个字符）和 ?（单个字符）
    private fun matchesWanted(pattern: String, identifiers: Set<String>): Boolean {
        if (!pattern.contains('*') && !pattern.contains('?')) {
            return identifiers.contains(pattern)
        }
        val regex = globToRegex(pattern)
        return identifiers.any { regex.matches(it) }
    }

    private fun globToRegex(pattern: String): Regex {
        val sb = StringBuilder("^")
        for (c in pattern) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                else -> sb.append(Regex.escape(c.toString()))
            }
        }
        sb.append('$')
        return Regex(sb.toString())
    }

    // 把求值结果应用到 UI 上
    private fun applyVisibility(name: String, shouldShow: Boolean, oldState: Boolean?) {
        visibilityState[name] = shouldShow

        val view = rowViews[name]
        val info = currentParamInfos.find { it.name == name }
        val isInitial = oldState == null

        if (info != null && info.dependReadonly) {
            val effectiveEnabled = shouldShow && info.readonly != true
            view?.visibility = View.VISIBLE
            view?.let { setRowInteractive(it, effectiveEnabled, animate = !isInitial) }
        } else if (view != null) {
            view.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    // 置灰 + 禁止整行交互（depend-readonly 用）
    private fun setRowInteractive(row: View, enabled: Boolean, animate: Boolean = false) {
        val targetAlpha = if (enabled) 1f else 0.9f
        if (animate) {
            row.animate().cancel()
            row.animate().alpha(targetAlpha).setDuration(200L).start()
        } else {
            row.animate().cancel()
            row.alpha = targetAlpha
        }
        setEnabledRecursively(row, enabled)
    }

    private fun setEnabledRecursively(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setEnabledRecursively(view.getChildAt(i), enabled)
            }
        }
    }
}
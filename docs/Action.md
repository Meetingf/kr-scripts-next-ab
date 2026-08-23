# Action

此节点在用户点击后执行一段脚本，且允许在执行前要求填写参数

## 用法

在任意页面添加以下代码后，将会创建一个点击后输出 "Hello World!" 的控件

```xml
<action>
	<title>Click This</title>
    <set>echo 'Hello world!'</set>
</action>
```

其中 `<set>` 表示在这个控件被点击后要执行的命令

## 属性



## 要求用户填写参数

添加以下代码任意页面，将会在用户点击此控件时弹出参数选择弹窗，并在用户点击确认时运行一段脚本

```xml
<action>
    <title>Click And Read Input</title>
    <set>echo "$foo"</set>
    <params>
        <param name="foo" type="text" />
    </params>
</action>
```

其中  `<param>` 声明了一个参数输入框，属性 `name` 表示该输入框存储用户输入的结果值在 `<set>` 中作为变量的名称，而 `type` 声明了该参数的类型，可用的类型如下。

| 类型 | 描述 | 取值 |
| - | :- | - |
| int | 整数输入框，可配合`min`、`max`属性使用 | `min`和`max`之间的整数 |
| number | 带小数的数字输入框，可配合`min`、`max`属性使用 | `min`和`max`之间的数字 |
| checkbox | 勾选框 | `1`或`0` |
| switch | 开关 | `1`或`0` |
| seekbar | 滑块，**必需**配合`min`、`max`属性使用 | `min`和`max`之间的整数 |
| file | 文件路径选择器，可配合`suffix`或`mime`属性使用 | 选中文件的绝对路径 |
| folder | 目录选择器 | 选中目录的绝对路径 |
| color | 颜色选择器 | 如`#FFFFFFFF`的色值 |
| app | 应用选择器 | 选取的应用包名 |
| text | 任意文本输入（未定义 type 时默认使用） | 任意自定义输入的文本 |



#### 动态获取参数默认值

```xml
<action>
    <title>Click And Read Input</title>
    <set>echo "$foo"</set>
    <params>
        <param name="foo" type="text" value-sh="echo 'Hi' " />
    </params>
</action>
```



#### 显示下拉菜单

```xml
<action>
    <title>切换状态栏风格</title>
    <desc>选择状态栏布局，[时间居中/默认]</desc>
    <!--可以在script中使用定义的参数-->
    <set>
        echo "mode参数的值：$mode"
        if [ "$mode" = "time_center" ]; then
            echo '刚刚点了 时间居中'
        else
            echo '刚刚点击了 默认布局'
        fi;
    </set>
    <!--params 用于在执行脚本前，先通过用户交互的方式定义变量，参数数量不限于一个，但不建议定义太多-->
    <params>
        <param name="mode" value="default" desc="请选择布局">
            <!--通过option 自定义选项
                [value]=[当前选项的值] 如果不写这个属性，则默认使用显示文字作为值-->
            <option value="default">默认布局</option>
            <option value="time_center">时间居中</option>
        </param>
    </params>
</action>
```



#### param 输入长度限制

```xml
<action>
    <title>自定义DPI</title>
    <desc>允许你自定义手机DPI，1080P屏幕推荐DPI为400~480，设置太高或太低可能导致界面崩溃！</desc>
    <set>
        wm density $dpi;
        wm size ${width}x${height};
    </set>
    <params>
        <param name="dpi" desc="请输入DPI，推荐值：400~480" type="int" value="440" maxlength="3" />
        <param name="width" desc="请输入屏幕横向分辨率" type="int" value="1080" maxlength="4" />
        <param name="height" desc="请输入屏幕纵向向分辨率" type="int" value="1920" maxlength="4" />
    </params>
</action>
```



#### 显示动态列表

- 如果你需要将选项的value（值）和label（显示文字）分开
- 用“|”分隔value和label即可，如：echo '380|很小'

```xml
<action desc-sh="echo '快速调整手机DPI，不需要重启，当前设置：';echo `wm density`;">
    <title>调整DPI</title>
    <desc>快速调整手机DPI，不需要重启</desc>
    <set>
        wm size reset;
        wm density $dpi;
        busybox killall com.android.systemui;
    </set>
    <params>
        <param name="dpi" value="440" options-sh="echo '380|很小';echo '410|较小';echo '440|适中';echo '480|较大';" />
    </params>
</action>
```



#### 显示多选列表

- 设置了`option`或`option-sh`的情况下，在`param`节点添加`multiple="true"`属性
- 即可将原来的单选模式切换为多选模式，例如：

    ```xml
    <action>
        <title>多选下拉</title>
        <param name="test" label="多选下拉" multiple="multiple">
            <option value="Z">测试一下 Z</option>
            <option value="X">测试一下 X</option>
        </param>
        <set>echo '数值为：' $test</set>
    </action>
    ```

- 默认设置下，多选列表的各个值用换行分隔，得到的参数可能是这样的
    ```sh
    value="
    aaa
    bbb
    "
    ```
- 可有时候，你希望得到的值是 `value="aaa,bbb"` 这样的？
- 其实你可以通过`separator`属性自定义分隔符，例如：
    ```xml
    <action>
        <title>多选下拉</title>
        <param name="test" label="多选下拉" multiple="multiple" separator=",">
            <option value="Z">测试一下 Z</option>
            <option value="X">测试一下 X</option>
        </param>
        <set>echo '数值为：' $test</set>
    </action>
    ```



#### 选择应用

- 像下面这个例子，是它最简单的用法：
    ```xml
    <action>
        <title>请选择一个应用</title>
        <param name="package_name" type="app" />
        <set>echo '包名为：' $package_name</set>
    </action>
    ```
- 那如何限制用户只可选择哪些应用呢？其实设置`option`就好了
    > 列表最终呈现的是包含在你的option里且用户已安装的应用

    ```xml
    <action>
        <title>请选择一个应用</title>
        <desc>配合options-sh轻松的限制可被选择的APP</desc>
        <param
            name="package_name"
            type="app"
            options-sh="pm list package -3 | cut -f2 -d ':'" />
        <set>echo '包名为：' $package_name</set>
    </action>
    ```

- 你甚至能设置为可以选择多个应用，以及默认的选中项，就像这样
    ```xml
    <action>
        <title>请选择几个应用</title>
        <desc>也可以设置允许选择多个应用，同时还可以设置默认选中项</desc>
        <param
            name="package_name"
            value="com.krscripts.app,com.android.browser"
            separator=","
            type="app"
            multiple="multiple"
            options-sh="pm list package -3 | cut -f2 -d ':'" />
        <set>echo '包名为：' $package_name</set>
    </action>
    ```

# Picker

在点击后显示一个选择界面

## 用法

```xml
<picker>
    <title>Picker</title>
    <options>
        <option value="1">Item 1</option>
        <option value="2">Item 2</option>
    </options>
    <get>getprop kr.test.picker</get>
    <set>setprop kr.test.picker $state</set>
</picker>
```

## 属性

| 属性 | 作用 | 有效值 | 示例 |
| - | - | - | :- |
| multiple | 是否允许多选(设置了options或type=app时可用) | `true` `false` | `true` |
| separator | 多选模式下多个值的分隔符，默认为换行符 | 任意字符 | `,` |

## 动态选项

- picker也允许使用`options-sh`属性来设置输出下拉选项的脚本
- 用法和action的param一样，如：

```xml
<picker options-sh="echo 'a|选项A'; echo 'b|选项B'">
    <title>测试单选界面</title>
    <desc>测试单选界面</desc>
    <get>getprop xxx.xxx.xxx3</get>
    <set>setprop xxx.xxx.xxx3 "$state"</set>
</picker>
```

## 多选模式

- 在picker节点上增加`multiple="true"`属性来标识允许多选
- 例如：

    ```xml
    <picker options-sh="echo 'a|选项A'; echo 'b|选项B'" value-sh="echo 'a'; echo 'b';">
        <title>测试单选界面</title>
        <get>getprop xxx.xxx.xxx4</get>
        <set>setprop xxx.xxx.xxx4 "$state"</set>
    </picker>
    ```

- 默认设置下，多选列表的各个值用换行分隔，得到的参数可能是这样的
    ```sh
    value="
    wifi
    airplane
    "
    ```
- 可有时候，你希望得到的值是 `value="wifi,airplane"` 这样的？
- 其实你可以通过`separator`属性自定义分隔符，例如：
    ```xml
    <picker multiple="multiple" separator=",">
        <title>隐藏状态栏图标</title>
        <desc>设置隐藏的状态栏图标</desc>
        <options>
            <option value="mobile">手机信号</option>
            <option value="wifi">WIFI</option>
            <option value="airplane">飞行模式</option>
        </options>
        <get>
            settings get secure icon_blacklist
        </get>
        <set>
            settings put secure icon_blacklist "$state"
        </set>
    </picker>
    ```

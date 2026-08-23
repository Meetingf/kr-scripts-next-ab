# 节点通用属性

| 属性          | 作用                                                         | 有效值                  | 示例                      |
| ------------- | ------------------------------------------------------------ | ----------------------- | :------------------------ |
| id            | 如果允许长按添加到桌面快捷，必需设置ID                       | 当前配置文件中必需唯一  | `a0001`                   |
| desc          | 显示在标题下的小字，可以不设置                               | 文本内容                | `这是描述`                |
| desc-sh       | 动态设置desc内容的脚本                                       | `脚本代码`              | `echo '自定义的说明信息'` |
| summary       | 高亮显示的摘要信息                                           | 文本内容                | `这是摘要`                |
| summary-sh    | 动态设置summary内容的脚本                                    | `脚本代码`              | `echo '自定义的摘要信息'` |
| confirm       | 点击时是否弹出确认框，默认`false`                            | `true`、`false`         | `false`                   |
| visible       | 自定义脚本，输出1或0，决定该功能项是否显示                   | 脚本代码                | `echo '1'`                |
| interruptible | 是否允许中断执行，默认`true`                                 | `true`、`false`         | `false`                   |
| auto-off      | 执行完脚本后是否自动关闭日志界面，默认`false`                | `true`、`false`         | `false`                   |
| auto-finish   | 是否在关闭日志界面后关闭当前页面                             | `true`、`false`         | `false`                   |
| logo          | 作为快捷方式添加到桌面时使用的图标                           | 文件路径                |                           |
| icon          | 显示在功能左侧的图标。如果未设置logo属性，它也同时会被作为logo使用 | 文件路径                |                           |
| reload        | 执行完脚本后要执行的刷新操作                                 | `page` 、具体体功能`id` | `page`                    |
| bg-task       | 后台运行而不是显示日志输出界面，默认`false`                  | `true` `false`          | `true`                    |

## visible 属性
- 如果你有一个功能选项，只有在满足特定条件的手机上才显示
- 那你一定用的上`visible`属性
- `visible`可以用于 `功能节点`、`外观节点`、`资源节点`，以及action的`param 节点`
- `visible`需要你定义一段脚本，在脚本中输出 `1` 或 `0` 来决定是否显示

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<group>
    <action visible="echo 1">
        <title>测试功能</title>
        <desc>只有visible定义的脚本输出内容为 1 ，我才会被显示</desc>
    </action>
</group>
```

## 系统版本限制
- 使用（target-sdk、min-sdk、max-sdk）
- 这三个属性可以定义在 `功能节点`和`页面节点`上，即 Action、Switch、Picker、Page 节点上
- 示例

```xml
<group title="系统版本限定">
    <action min-sdk="28" max-sdk="28">
        <title>SDK版本限制-范围</title>
        <desc>通过[min-sdk]和[max-sdk]属性，可以很方便的设置系统版本范围限制</desc>
        <set>
            echo '....'
        </set>
    </action>

    <action target-sdk="28">
        <title>SDK版本限制-指定单个版本</title>
        <desc>通过[target-sdk]属性，可以很方便的设置系统版本限制</desc>
        <set>
            echo '....'
        </set>
    </action>
</group>
```


## 操作确认和警告
- 操作确认 [confirm="true"]，操作警告 [warning="警告内容"]
- 是指点击功能时弹出的二次确认对话框，二者的区别是 `confirm` 默认使用功能标题和描述作为提示内容，`warning`则需要自定义提示内容

- 这两个属性可以定义在 `功能节点`上，即 Action、Switch、Picker 节点上

- 例如
```xml
<action warning="我了个去，你还真点啊，此功能的主要作用是防止用户误触或提示功能危险性，相当于以前的 [confirm=true] 属性优化版本">
    <title>测试操作警告</title>
    <desc>当你点击此功能时，会显示自定义提示：[我勒个去，你还真点啊...]</desc>
</action>
```
> **注意**：如果Action具有一个或多个参数(param)，则`warning`的内容不会单独弹窗提示，而是显示在参数输入界面的顶部


## 功能锁定
- (locked、lock)
- 这三个属性可以定义在 `功能节点`和`页面节点`上，即 Action、Switch、Picker、Page 节点上
- 使用`locked`属性可将一个功能设为不可运行，点击后会弹出提示告知用户
```xml
<switch locked="true">
    <title>静态锁</title>
    <desc>在Page、Switch、Action、Picker节点上添加 locked="true" 将功能锁定，避免被点击</desc>
</switch>
```

- `lock`是一个脚本节点(类似于get、set节点)，它会在用户点击功能或页面时首先被执行
- 如果执行`lock`节点指定的脚本时输出了文本（且内容不是unlocked、unlock、false、0 之一）
- 则认为该功能节点或页面被限制访问，并将输出的文本作为提示内容提示给用户
```xml
<action>
    <title>动态锁</title>
    <desc>在Page、Switch、Action、Picker内添加lock节点来动态判断功能锁定状态</desc>
    <summary>动态锁在脚本里输出提示内容</summary>
    <set>echo '诶嘿~'</set>
    <lock>
        if [[ `getprop ro.build.version.sdk` -gt 28 ]]; then
            echo '你不能在 > Android 9.0(SDK28)的系统上使用本功能！'
        else
            // 输出 unlocked、unlock、false、0 均可表示功能处于解锁状态
            // 输出其它内容则会被视作提示文本显示
            echo 'unlocked'
        fi
    </lock>
</action>
```

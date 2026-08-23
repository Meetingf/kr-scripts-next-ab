# Switch

此节点显示一个开关，在按下后会执行一段脚本

## 用法

```xml
<switch>
    <title>Click This</title>
    <get>getprop kr.test.switch</get>
    <set>setprop kr.test.switch "$state"</set>
</switch>
```

` <get>` 表示开关 初始化 或 点击后刷新 时获取自己状态的命令，输出 `1` 或 `0` 来确定开关当前状态

`<set>` 表示点击开关后要执行的命令，开关被点击后的状态会以`$state`参数传入脚本

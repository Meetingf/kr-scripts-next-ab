# Text

显示一段文本

## 用法

```xml
<text>
    <title>Title</title>
    <desc>Description Content</desc>
</text>  
```

## 拓展

### Slice

- 如果你需要定义个性化的文本样式，`slice`节点有一些简单的样式属性可以使用

- 用法
```xml
<text>
    <slice>普通文本</slice>
	<slice break="true" bold="true">加粗</slice>
	<slice break="true" italic="true">斜体</slice>
	<slice break="true">换行</slice>
	<slice break="true" bold="true" italic="true">粗斜体</slice>
	<slice break="true" size="18">字体大小18dp</slice>
	<slice break="true" color="#42A5F5">显示为蓝色</slice>
	<slice break="true" align="center">居中对齐</slice>
	<slice break="true" align="right">靠右对齐</slice>
	<slice break="true" bgcolor="#42A5F5" color="#ffffff">蓝底白字</slice>
	<slice break="true" underline="underline">文字带下划线</slice>
	<slice break="true" link="https://www.example.com/" size="16">测试网站</slice>
	<slice break="true" activity="android.settings.APN_SETTINGS" size="16">打开APN设置</slice>
	<slice break="true" run="echo '你点击了脚本！'" size="16">运行脚本（run）</slice>
</text>
```



- 属性

| 属性 | 说明 | 有效值 |
| - | - | - |
| bold **(简写: `b`)** | 是否加粗 | `true`、`false` |
| italic **(简写: `i`)** | 是否倾斜 | `true`、`false` |
| underline **(简写: `u`)** | 是否显示下划线 | `true`、`false` |
| break | 是否换行后显示 | `true`、`false` |
| size | 字体大小(dp) | 整数值 例如：`20` |
| align | 文字对齐 | `normal`、`center`、`right`、`left` |
| color | 文字颜色| #开头的十六进制色，如：`#445566` |
| background **(简写: `bg`)** | 文字背景色 | #开头的十六进制色，如：`#000000` |
| link **(或者: `href`)** | 文本链接，点击后打开网页 | 如 `http://vtools.omarea.com/` |
| activity **(简写: `a`)** | activity，点击后打开Activity | 如 `android.settings.APN_SETTINGS` |
| run **(同: `scrip`)** | 点击后要执行的脚本，脚本的输出内容将以弹窗显示 | 如 `echo "123"` |

::: warning

`align`属性的`left`、`right`目前只支持`Android 9`及更高版本系统

:::

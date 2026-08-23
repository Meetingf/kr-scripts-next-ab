# 导航

## 开始

本框架中所有的页面都是由 `Xml` 编写，下面是一个简单的页面示例

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<config title="Page 1">
	<Text>
        <title>I'm a text</title>
        <desc>Hello World!</desc>
    </Text>
</config>
```

在应用的 `assets` 文件夹中新建一个文件，并命名为 `foo.xml` ，粘贴以上内容。

编辑 `assets` 文件夹中的 `kr-config.conf` 将 `page_list_config` 等号后面(双引号里面) 的文本清空，并填入 

```
 file:///android_asset/foo.xml
```

接下来将文件保存，并签名和安装应用，我们将会看到一个示例页面。
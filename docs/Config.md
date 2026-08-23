# 全局配置文件

全局配置文件存放于应用 `assets` 目录，文件名为 `kr-script.conf`

## 示例

```sh
# 脚本执行包装器
executor_core="file:///android_asset/kr-script/executor.sh"

# 页面配置文件
page_list_config="file:///android_asset/kr-script/home.xml, file:///android_asset/kr-script/more.xml"

# 工具目录
toolkit_dir="file:///android_asset/kr-script/toolkit"
```

## 配置项说明

### toolkit_dir

存放供应用全局所使用的二进制或脚本文件，在启动时会被自动解压至应用私有目录`files/kr-script/`

### page_list_config

 声明应用主页导航项目，以 `,` 分隔，两个文件路径分别对应主页面下方导航栏的两个导航项目: 主页 和 更多

### executor_core

 声明应用的脚本执行包装器，每个脚本和命令的运行都会经过此包装器，它会提供脚本执行所需的应用专有变量，如 `$PACKAGE_NAME` 为应用的包名。

### before_start_sh (可选)

 声明的脚本文件在解析完`kr-script.conf`之后会被立即执行，执行过程中输出的内容和错误信息，会显示在启动屏上，你可以利用此脚本，完成在线检查更新

### page_list_config_sh (可选)

 声明可以输出页面配置的脚本，脚本内容如 

```sh
echo "<config>...</config>"
```

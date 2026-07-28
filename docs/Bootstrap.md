# 应用启动过程
- 在检测完ROOT权限之后，显示功能列表之前
- 应用会先读取 `assets` 下的 `kr-script.conf`
- 读取`kr-script.conf`的过程中，会完成 `toolkit_dir` 的提取处理
- `kr-script.conf` 以 `key="value"` 的格式配置一些基本信息
- 例如：

```sh
  # 脚本执行包装器
executor_core="file:///android_asset/kr-script/executor.sh"

# 页面配置文件
page_list_config="file:///android_asset/kr-script/home.xml, file:///android_asset/kr-script/more.xml"

# 工具目录
toolkit_dir="file:///android_asset/kr-script/toolkit"
 ```

## 可配置属性

| 属性                  | 说明          |
|---------------------|-------------|
| executor_core       | 执行包装器位置     |
| toolkit_dir         | 全局工具目录      |
| page_list_config    | 存储页面配置列表    |
| before_start_sh     | 启动页执行的脚本    |
| page_list_config_sh | 输出页面配置路径的脚本 |

## before_start_sh
- 在解析完`kr-script.conf`之后，会立即执行`before_start_sh` 配置的脚本
- 执行过程中输出的内容和错误信息，会显示在启动屏上
- 你可以利用此脚本，完成在线检查更新

## page_list_config_sh
- 这两个属性的存在意义，是为了动态指定页面的配置文件所在路径
- 就像 `<page config-sh="echo 页面路径;" />` 那样

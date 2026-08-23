# 从旧版 Kr-Scripts 迁移

## 行为变更

### 脚本执行包装器

1. 旧版包装器中设置预解析变量的语法为 ```VALUE="$({VALUE})"``` 而这里的 ```$()``` 与 shell 语法存在冲突，故新版本的的格式为 ```VALUE="{VALUE}"``` .
2. 属性 MAGISK_PATH 因兼容性问题被移除

### kr-script.conf

1. 属性 allow_home_page 已被删除
2. 增加了多 nav 页面支持，page_list_config 的值应该是以 "," 为分隔符的页面配置文件列表，而favorite_list_config 属性被移除
3. 改配置文件的解析器现支持把注释写在属性后

### kr-script/favorite(more).xml

1. 在多 nav 支持下，页面配置文件不再局限于 more.xml 和 favorite.xml，而在框架示例应用中被替换为 home.xml 和 more.xml，且还可以增加更多页面
2. 主页面配置文件中， ```<config title="主页"></config>``` 通过设置 title 属性更改其下方 navigationBar 显示的名称，如该节点不存在，将使用页面配置的文件名作为标题 如："home.xml"

### 所有xml配置文件

1. 请避免使用 ```<Group> <items> <root> <page>``` 这样的名称作为根节点，否则可能出现不可预测的问题，为了从长计议，应该使用 ```<config>``` 作为根节点或不使用根节点
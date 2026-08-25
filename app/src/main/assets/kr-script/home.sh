cat <<DNA
<?xml version="1.0" encoding="UTF-8" ?>
<nav title="主页">
<resource dir="file:///android_asset/samples" />
DNA

cat <<DNA
<group>
    <action auto-off="true">
          <title>选择工程</title>
          <set>echo \$TSL > \$TMPDIR/DNA.ini</set>
          <summary sh="echo '当前工程: /sdcard/DNA' ;cat \$TMPDIR/DNA.ini"/>
          <param name="TSL" options-sh="samples/project.sh CK" required="true"/>
    </action>
   
    <action reload="true" auto-off="true">
           <title>删除工程</title>
           <set>samples/project.sh SC</set>
           <param name="TSS" multiple="true" options-sh="samples/project.sh CK" required="true"/>
    </action>
    
    <action reload="true" auto-off="true">
           <title>新建工程</title>
           <set>samples/project.sh XJ</set>
           <param name="T" placeholder="请输入项目名称(非中文)：" required="true"/>
    </action>
    
    <action reload="true" interruptible="false">
           <title>解压ROM</title>
           <set>dna unzip --delete \$silence \$DNA_DIR/\$ZIP \$DNA_DIR</set>
           <param name="silence" label="删除源文件" type="checkbox" />
           <param name="ZIP" title="请选择zip文件：" options-sh="samples/findfile.sh zip1" desc="识别/sdcard/DNA工程路径下的文件" required="true"/>
    </action>
DNA

cat <<DNA
    <page title="工程菜单" reload="true" config="file:///android_asset/kr-script/dna.xml">
            <option type="refresh">快捷选择工程目录</option>
DNA
uk=$(sh samples/project.sh CK)
for dir in $uk; do
    echo '            <option type="default" id="'$dir'" auto-off="true">'$dir'</option>'
done
cat <<DNA
            <handler>
                echo "选择目录: \$menu_id"
                echo \$menu_id > \$TMPDIR/DNA.ini
            </handler>
            <lock>
                if [[ -f \$TMPDIR/DNA.ini ]]; then
                    echo 'unlocked'
                else
                    echo '不选项目，你分解个🐔儿！'
                fi
            </lock>
    </page>
</group>
DNA

cat <<DNA
<group>
    <text>
        <slices>
            <slice size="18" color="#ffff0000">声明</slice>
            <slice break="true"></slice>
            <slice size="10" color="#ffff0000">本工具不会主动破坏手机系统，因使用者不当操作，造成的一切后果自行承担</slice>
            <slice break="true"></slice>
            <slice size="10" color="#ffff0000">一句话：爱用就用不用就卸载😂😂😂</slice>
            <slice break="true"></slice>
            <slice size="10" color="#ffff0000">如果发现在你手机上使用有问题，请联系我修复，QQ：903501507</slice>
        </slices>
    </text>
</group>
</nav>
DNA

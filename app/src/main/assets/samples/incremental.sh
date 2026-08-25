cat <<DNA
<?xml version="1.0" encoding="UTF-8" ?>
DNA

cat <<DNA
<group title="分解旧版本ROM包">
    <action auto-off="true" reload="page">
        <title>分解旧版本ROM包</title>
        <desc>选择本地旧版本ROM包</desc>
        <param name="incremental" placeholder="请选择文件" type="file" suffix="zip" value="" required="true" editable="true"/>
        <set>payload_extract -i \$incremental -x -o \$DNA_PRO/oldimg/ -s</set>
    </action>
</group>
DNA

if [[ -d $DNA_PRO/oldimg ]]; then
cat <<DNA
<group title="选择增量包">
    <action auto-off="true" reload="page">
        <title>选择本地增量包</title>
        <summary sh="echo '当前文件：' ;cat \$TMPDIR/incremental.ini"/>
        <param name="incremental" placeholder="请选择文件" type="file" suffix="zip" value-sh="cat \$TMPDIR/incremental.ini" required="true" editable="true"/>
        <set>echo \$incremental > \$TMPDIR/incremental.ini</set>
    </action>
</group>
DNA
else
cat <<DNA
<group title="选择增量包">
    <action auto-off="true" reload="page">
        <title>暂未分解旧版本ROM包</title>
        <desc>请在上面分解旧版本ROM包后重试</desc>
    </action>
</group>
DNA
fi

if [[ -f $TMPDIR/incremental.ini ]]; then
cat <<DNA
<group title="增量分区列表">
    <action>
        <title>提取增量镜像文件</title>
        <set>samples/extract-bin.sh extract incremental</set>
        <param name="incrementalIMG" title="请选择分区支持多选：" separator="," multiple="true" options-sh="samples/extract-bin.sh name incremental" required="true"/>
    </action>
</group>
DNA
fi
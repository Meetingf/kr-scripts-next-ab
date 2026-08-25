cat <<DNA
<?xml version="1.0" encoding="UTF-8" ?>
DNA

cat <<DNA
<group title="xiaomirom.com">
<page title="小米ROM包" html="https://xiaomirom.com/"/>
</group>
<group title="选择如何分解">
    <action auto-off="true" reload="page">
        <title>本地分解</title>
        <desc>选择本地压缩包</desc>
        <summary sh="echo '当前文件：' ;cat \$TMPDIR/local.ini"/>
        <param name="local" placeholder="请选择文件" type="file" suffix="zip" value-sh="cat \$TMPDIR/local.ini" required="true" editable="true"/>
        <set>echo \$local > \$TMPDIR/local.ini</set>
    </action>

    <action auto-off="true" reload="page">
        <title>云端分解</title>
        <desc>填写刷机包链接</desc>
        <summary sh="echo '当前链接：' ;cat \$TMPDIR/cloud.ini"/>
        <param name="cloud" placeholder="请输入链接" value-sh="cat \$TMPDIR/cloud.ini" required="true"/>
        <set>echo \$cloud > \$TMPDIR/cloud.ini</set>
    </action>
</group>
DNA

if [[ -f $TMPDIR/local.ini ]]; then
cat <<DNA
<group title="本地分区列表">
    <action>
        <title>提取本地镜像文件</title>
        <set>samples/extract-bin.sh extract local</set>
        <param name="localIMG" title="请选择分区支持多选：" separator="," multiple="true" options-sh="samples/extract-bin.sh name local" required="true"/>
    </action>
</group>
DNA
fi

if [[ -f $TMPDIR/cloud.ini ]]; then
cat <<DNA
<group title="云端分区列表">
    <action>
        <title>提取云端镜像文件</title>
        <set>samples/extract-bin.sh extract cloud</set>
        <param name="cloudIMG" title="请选择分区支持多选：" separator="," multiple="true" options-sh="samples/extract-bin.sh name cloud" required="true"/>
    </action>
</group>
DNA
fi

cat <<DNA
<group>
    <page title="分解增量包" desc="增量包仅支持本地分解" config-sh="samples/incremental.sh" />
</group>
DNA

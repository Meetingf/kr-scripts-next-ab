#查看项目
CK () {
for i in $(ls -d $DNA_DIR/DNA_*/);do
    dirname="${i%/}"
    echo "${dirname##*/}"
done
}
#新建项目
XJ () {
if [ -d $DNA_DIR/DNA_"$T" ]; then
  echo "> 项目已存在，将自动重命名！"
  T="$T"-`date "+%Y%m%d%H%M%S"`
  echo "> 正在创建：DNA_${T}"
  mkdir -p $DNA_DIR/DNA_"$T"
else
  echo "> 正在创建：DNA_${T}"
  mkdir -p $DNA_DIR/DNA_"$T"
fi
echo "> 创建完成！"
echo "DNA_${T}" > $TMPDIR/DNA.ini
}
#删除项目
SC () {
for i in ${TSS};do
  echo "> 正在删除：${i}"
  rm -rf $DNA_DIR/$i
  rm -rf $DNA_TMP/$i
  if [ "$i" = "$project" ];then
    rm -rf $TMPDIR/DNA.ini
  fi
  echo "> 删除完成！"
done
}
#删除插件
sub (){
for i in ${sub};do
  echo "> 正在删除：$i"
  rm -rf $START_DIR/module/$i
  echo "> 删除完成！"
done
}

$1
if [ ! -d $DNA_PRO/out ];then
 mkdir -p $DNA_PRO/out
fi
for i in $IMG ;do
info=$(dna gettype $DNA_PRO/$i)
if [ "$info" = "ext" ] || [ "$info" = "erofs" ]; then
  echo "> 开始转换：$i"
  img2simg $DNA_PRO/$i $DNA_PRO/out/$i
elif [ "$info" = "sparse" ]; then
  echo "> 开始转换：$i"
  simg2img $DNA_PRO/$i $DNA_PRO/out/$i
else
  echo "> 不支持转换：$i"
fi
if [[ $silence = 1 ]]; then
  echo "> 正在删除：$i"
  rm -rf $DNA_PRO/$i
fi
if [ -f $DNA_PRO/out/$i ];then
  echo "> 转换完成，文件位于：$DNA_PRO/out"
else
  echo "> 转换失败，截图联系开发者修复！！！"
fi
done
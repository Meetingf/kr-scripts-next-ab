name(){
if [[ $1 == incremental ]];then
payload_extract -i "$(cat $TMPDIR/${1}.ini)" --incremental $DNA_PRO/oldimg -p
else
payload_extract -i "$(cat $TMPDIR/${1}.ini)" -p
fi
}

extract(){
if [[ $1 == local ]];then
payload_extract -i "$(cat $TMPDIR/${1}.ini)" --extract=$localIMG -o $DNA_PRO/
elif [[ $1 == cloud ]];then
payload_extract -i "$(cat $TMPDIR/${1}.ini)" --extract=$cloudIMG -o $DNA_PRO/
else
payload_extract -i "$(cat $TMPDIR/${1}.ini)" --incremental $DNA_PRO/oldimg --extract=$incrementalIMG -o $DNA_PRO/
fi
}

$1 $2

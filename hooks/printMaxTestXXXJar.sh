#! /bin/bash
MV=0
for i in Test*.jar ; do
	CV=$(echo $i| grep -oE '([0-9]+)')
	if [ $CV -gt $MV ] ; then
		MV=$CV
	fi
done
echo $MV

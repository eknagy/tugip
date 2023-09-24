#! /bin/bash
SUFFIX=""
for p in $@ ; do
	SUFFIX="${SUFFIX}_${p}"
done
# cp -v ~/tugip/build/distributions/tugip.tar .
# tar -xf tugip.tar libs/tugip.jar
cp -v ~/tugip/build/libs/tugip.jar Test$(($(./printMaxTestXXXJar.sh)+1))$SUFFIX.jar

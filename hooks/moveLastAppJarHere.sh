#! /bin/bash
SUFFIX=""
for p in $@ ; do
	SUFFIX="${SUFFIX}_${p}"
done
cp -v ~/nbp/app/build/distributions/app.tar .
tar -xf app.tar app/lib/app.jar
mv -v app/lib/app.jar Test$(($(./printMaxTestXXXJar.sh)+1))$SUFFIX.jar

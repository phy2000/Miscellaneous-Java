#!/bin/bash
ORIGDIR=`pwd`
DESTDIR=$ORIGDIR/ServiceCopy
rm -rf $DESTDIR
mkdir -p $DESTDIR/helper
cd ../run/server
for f in jsl.* server.jar server.log4j.properties ; do
	cp $f $DESTDIR
done

cd helper
cp *.exe $DESTDIR/helper
cd $ORIGDIR
cp OsNameArch.jar $DESTDIR
zip -r ServiceCopy.zip ServiceCopy

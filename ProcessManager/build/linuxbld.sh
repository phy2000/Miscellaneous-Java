#!/bin/bash -x 

if [ -z "$TOPDIR" ] ; then
	TOPDIR=..
fi

SRCDIR=$TOPDIR/src
BLDDIR=$TOPDIR/build
LIBDIR=$TOPDIR/lib
UTILSRC=$SRCDIR/pmutils
APISRC=$SRCDIR/pmapi
HELPER=$SRCDIR/pmserver/PMNative

SERVERSRC=$SRCDIR/pmserver/*.java
CLIENTSRC=$SRCDIR/pmclient/*.java

CPATH=$BLDDIR

for JARFILE in $LIBDIR/*.jar; do
	CPATH=$CPATH:$JARFILE
done

CLASSPATH=$CPATH:$CLASSPATH
export CLASSPATH

make -B $HELPER
cp $HELPER .
javac -d $BLDDIR -target 5 -source 5 $UTILSRC/*.java
javac -d $BLDDIR -target 5 -source 5 $CLIENTSRC
javac -d $BLDDIR -target 5 -source 5 $SERVERSRC
javac -d $BLDDIR -target 5 -source 5 $APISRC/*.java
ant -file bld-server.xml
ant -file bld-client.xml
ant -file bld-osname.xml
ant -file bld-api.xml
ant -file PMAPI-javadoc.xml

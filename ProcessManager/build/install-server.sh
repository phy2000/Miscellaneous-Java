#!/bin/bash
TOPDIR=..
SRCDIR=$TOPDIR/src
RUNDIR=$TOPDIR/run
SCRIPTDIR=$TOPDIR/scripts
HELPER=PMNative

mkdir -p $RUNDIR/server/helper
mkdir -p ../helper
PLATFORM=$(java -jar OsNameArch.jar) 

cp $HELPER $RUNDIR/server/helper/PM$PLATFORM
cp $HELPER $TOPDIR/helper/PM$PLATFORM
cp $TOPDIR/server.log4j.properties $RUNDIR/server
cp $SCRIPTDIR/runserver $RUNDIR/server
cp $SCRIPTDIR/runcheck.sh $RUNDIR/server
cp $SCRIPTDIR/runhosts.sh $RUNDIR/server
cp $SCRIPTDIR/unixhosts.txt $RUNDIR/server
cp $SCRIPTDIR/winhosts.txt $RUNDIR/server
cp $SCRIPTDIR/setjava.sh $RUNDIR/server

cp server.jar $RUNDIR/server 
cp OsNameArch.jar $RUNDIR/server

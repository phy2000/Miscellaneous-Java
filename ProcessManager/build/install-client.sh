#!/bin/bash
TOPDIR=..
SRCDIR=$TOPDIR/src
RUNDIR=$TOPDIR/run
SCRIPTDIR=$TOPDIR/scripts
LIBDIR=$TOPDIR/lib

mkdir -p $RUNDIR/client
cp client.jar $RUNDIR/client 
cp $TOPDIR/client.log4j.properties $RUNDIR/client
cp $SCRIPTDIR/pmclient $RUNDIR/client
cp $SCRIPTDIR/pmkill $RUNDIR/client

mkdir -p $RUNDIR/api
cp PMAPI.jar $RUNDIR/api
cp $SRCDIR/PMUser.java $RUNDIR/api
cp $LIBDIR/*.jar $RUNDIR/api
cp $SCRIPTDIR/README-api.txt $RUNDIR/api

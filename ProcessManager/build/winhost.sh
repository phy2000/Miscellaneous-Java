#!/bin/bash -x
USAGE="USAGE: $0 <windows host>"
if [ $# -lt 1 ] ; then
	echo $USAGE >&2
	exit 1
fi
WINHOST=$1

NAME=User
if [[ $WINHOST =~ ehenna ]] ; then
	NAME=user
fi

scp ServiceCopy.zip $NAME@$WINHOST:/cygdrive/c/ProcMgr
scp winstall.sh $NAME@$WINHOST:/cygdrive/c/ProcMgr
ssh $NAME@$WINHOST /cygdrive/c/ProcMgr/winstall.sh

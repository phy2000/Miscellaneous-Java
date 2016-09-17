#!/bin/bash 

USAGE="USAGE: $0 {start|stop|restart}"
SCRIPTDIR=$(cd $(dirname $BASH_SOURCE[0]) && pwd)

if [ "$#" -ne 1 ] ; then
	echo $USAGE >&2
	return 2> /dev/null
	exit
fi

case $1 in
	start|stop|restart)
		CMD=$1
		;;
	*)
		echo $USAGE >&2
		exit 1
		;;
esac

cd $SCRIPTDIR

if [ ! -r hosts.txt ] ; then
	echo ERROR: hosts.txt not found! >&2
	echo EXITING >&2
fi

for h in $(<hosts.txt ) ; do
	set -x
	ssh autotest@$h /home/autotest/ProcMgr/current/server/runserver $CMD
	set +x
done

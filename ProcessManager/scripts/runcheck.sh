#!/bin/bash 

USAGE="$0 [-q]"
QUIET=0
if [ $# -ge 1 ] ; then
	case $1 in
		"-q") QUIET=1 ;;
		*) echo $USAGE >&2
			exit 1 ;;
	esac
fi

JDKLIST=$(echo /usr/local/jdk*/bin /usr/java/jdk*/bin /usr/local/java/jdk*/bin)
for JDK in $JDKLIST
do
    if [[ -d "$JDK" ]]; then
        PATH=$JDK:$PATH
    fi
done

USER=$(id -un)
OUTDIR=/tmp/$USER
mkdir -p $OUTDIR

THISOUT=$OUTDIR/runcheck.out
THATMSG=$OUTDIR/runcheck.msg
THATCNT=$OUTDIR/runcheck.cnt

for h in $(<unixhosts.txt ) ; do
	../client/pmclient -h $h dump > dump.out 2>&1; 
	if [ "$?" == 0 ] ; then
		if [ $QUIET -le 0 ] ; then
			echo $h OK 
		fi
	else
		echo '>>>>>>>>>>>>>>>>>>>>>>>>>>' >> dump.err.out
		echo $h: $(date) >> dump.err.out
		cat dump.out >> dump.err.out
		echo $h needs restart;
		ssh autotest@$h "cd `pwd` && ./runserver start"
	fi;
done > $THISOUT 2>&1

for h in $(<winhosts.txt ) ; do
	../client/pmclient -h $h dump > dump.out 2>&1; 
	if [ "$?" == 0 ] ; then
		if [ $QUIET -le 0 ] ; then
			echo $h OK 
		fi
	else
		echo '>>>>>>>>>>>>>>>>>>>>>>>>>>' >> dump.err.out
		echo $h: $(date) >> dump.err.out
		cat dump.out >> dump.err.out
		echo $h needs restart;
	fi;
done >> $THISOUT 2>&1

THROTTLECNT=12
if [ $QUIET -le 0 ] ; then
	cat $THISOUT
else # in quiet mode, throttle a repeated message
	COUNT=0
	if [ -r $THATCNT ] ; then
		COUNT=$(<$THATCNT)
	fi
	if [ -r $THATMSG -a -s $THATMSG ] ; then
		diff $THATMSG $THISOUT > /dev/null 2>&1
		DIFFVAL=$?
		case $DIFFVAL in
			0) # no difference - throttle?
				rm -f $THISOUT
				let COUNT=$COUNT+1
				if [ $COUNT -ge $THROTTLECNT ] ; then
					echo THROTTLED MESSAGE REPEATED $COUNT times:
					cat $THATMSG
					COUNT=0
				fi
				echo $COUNT > $THATCNT
				;;
			1) # different
				touch $THISOUT
				mv $THISOUT $THATMSG
				echo 0 > $THATCNT
				cat $THATMSG
				;;
			*) # error
				;;
		esac
	else
		touch $THISOUT
		mv $THISOUT $THATMSG
		echo 0 > $THATCNT
		cat $THATMSG
	fi
fi

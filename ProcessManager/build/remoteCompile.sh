#!/bin/bash

#
# run this within ProcessManager/build on a Linux system
#
# Replaced Storm with Axe - build glibc 2.3 version
hostlist="raze wormwood axe blitz nix hydra ice hack vex oldhat"
# hostlist="axe"

for h in $hostlist ; do
	CC=cc
	echo $h
	case $h in
		blitz )
	    CC=/usr/sfw/bin/gcc
	    CFLAGS="-m64"
			JPATH=/usr/local/jdk1.6.0_02/bin
			JFLAGS=-d64
	    ;;

		raze )
	    CFLAGS=-m64
			JPATH=/usr/local/jdk1.5.0_06/bin/amd64
			JFLAGS=-d64
	    ;;

		wormwood )
	    CFLAGS=-m64
			JPATH=/usr/bin
			JFLAGS=-d64
	    ;;

		axe )
	    CFLAGS=-m64
			JPATH=/usr/java/jdk1.6.0/bin
			JFLAGS=-d64
	    ;;

		storm )
	    CFLAGS=-m64
			JPATH=/usr/local/jdk1.6.0_02/bin
			JFLAGS=-d64
	    ;;

		nix )
	    CFLAGS=""
			JPATH=/usr/local/jdk1.6.0_16/bin
			JFLAGS=-d64
	    ;;

		hydra )
	    CC=/usr/local/bin/gcc
	    CFLAGS=-mlp64
			JPATH=/opt/java6/bin/IA64W
			JFLAGS=-d64
	    ;;

		ice )
	    CFLAGS="-mpowerpc64 -maix64"
	    CC=/openpkg/bin/cc
			JPATH=/usr/java5_64/bin
			JFLAGS=-d64
	    ;;

		hack )
	    CFLAGS=-m32
			JPATH=/usr/local/jdk1.5.0_12/bin
			JFLAGS=-d32
	    ;;

		vex )
	    CFLAGS=-m32
	    CC=/openpkg/bin/cc
			JPATH=/usr/local/jdk1.6.0_01/bin
			JFLAGS=-d32
	    ;;

		oldhat )
	    CFLAGS=-m32
			JPATH=/usr/java/jdk1.5.0_04/bin
			JFLAGS=-d32
	    ;;

		* )
			echo Unknown host $h
			echo Using default CC=$CC, CFLAGS=$CFLAGS, JPATH=$JPATH and JFLAGS=$JFLAGS
			;;

	esac

	OSNAME=$(ssh $h.29west.com "cd `pwd` && $JPATH/java $JFLAGS -jar OsNameArch.jar")

	ssh $h.29west.com \
			"cd `pwd` && $CC $CFLAGS -o PM$OSNAME ../src/pmserver/PMNative.c"
	ssh $h.29west.com "cd `pwd` && file PM$OSNAME"
	cp PM$OSNAME ../run/server/helper
done

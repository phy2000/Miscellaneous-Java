#!/bin/bash
export PATH=/bin:/usr/bin:/usr/local/bin:$PATH
sc query PMServer
sc stop PMServer
cd /cygdrive/c/ProcMgr
rm -rf ServiceOld
rm -rf ServiceCopy
unzip -q ServiceCopy.zip
cd Service
/usr/bin/find . -type d -name data > data.txt
/usr/bin/find . -type d -name logs >> data.txt
/usr/bin/find $(<data.txt) -depth | cpio -pvd --quiet ../ServiceCopy
cd ..
sleep 10
mv Service ServiceOld
mv ServiceCopy Service
find Service | xargs chmod 777
sc query PMServer
sc start PMServer
sleep 10
sc query PMServer

copy startmsg.txt tmpmsg
hostname >> tmpmsg
sendmail < tmpmsg
java -jar server.jar
copy restartmsg.txt tmpmsg
hostname >> tmpmsg
sendmail < tmpmsg

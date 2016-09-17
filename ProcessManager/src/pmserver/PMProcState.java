package pmserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import pmutils.Misc;
import pmutils.PMDefines;
import pmutils.PMPacket;
import pmutils.PMTimerTask;
import pmutils.SelectableStream;

class PMProcState
{
  static Logger logger = Logger.getLogger("ProcMgr");
  SelectableStream stderr;
  SelectableStream stdout;
  SelectableStream stdin;
  PMPacket packet = new PMPacket(null);
  File outFile;
  File errFile;
  File inFile;
  File histFile;
  File stateFile;
  File procDir;
  FileOutputStream outStream;
  FileOutputStream errStream;
  FileInputStream inStream;
  PMClientd clientd;
  String commandLine;
  Misc.procState pstate = Misc.procState.PS_INIT;
  int killSeconds = PMDefines.DEFAULT_KILL_SECONDS;
  PMTimerTask timerTask;
  String nativeApp = null;
  int processId = -1;
  String procHandle; // offset + processId
  int procOffset; // Random 32-bit number + current time
  boolean mix = false;
  ByteBuffer errBuf;
  Date startTime;
  Date endTime;
  boolean recovered = false;
  long outPosition;
  long errPosition;
  ArrayList<PMClientd> clientWaitList = new ArrayList<PMClientd>();
  boolean isValid()
  {
    switch (pstate) {
    case PS_RUN:
    case PS_SUSPEND:
    case PS_DEAD:
      break;
    default:
      return false;
    }
    if (mix) {
      errFile = outFile;
    }
    if ((startTime != null) && (commandLine != null) && (processId > 0)
        && (errFile != null) && (outFile != null)) {
      return true;
    }
    return false;
  }

  boolean removeFiles()
  {
    boolean retval = true;
    if (!procDir.exists()) {
      return retval;
    }
    File dirFiles[] = procDir.listFiles();
    if (dirFiles != null) {
      for (int i = 0; i < dirFiles.length; i++) {
        //logger.info("Deleting " + dirFiles[i].getAbsolutePath());
        if (!dirFiles[i].delete()) {
          logger.warn("Error deleting " + dirFiles[i].getAbsolutePath());
        }
      }
    }
    if (!(procDir.delete())) {
      procDir.deleteOnExit();
      retval = false;
      logger.warn(procHandle + ": ProcDir NOT deleted - delete on exit set");
    }
    return retval;
  }
  
  PMProcState(String app) 
  {
    nativeApp = app;
    endTime = new Date(0);
    startTime = new Date(0);
    packet.setLogger(logger);
  }

  public void notifyWaiters()
  {
    if (clientWaitList.isEmpty()) {
      return;
    }
    PMClientd clientd = clientWaitList.remove(0);
    sendState(clientd, "WAIT");
    try {
      clientd.channel.close();
    } catch (IOException e) {
      logger.warn(e, e);
      // warn and continue
    }

  }
  
  public void sendState(PMClientd clientd, String cmd)
  {
    String pHandle = procHandle;
    String pid = Integer.toString(processId);
    String status;
    boolean success = false;
    switch (pstate) {
    case PS_INIT:
      status = "INIT";
      break;
    case PS_RUN:
      status = "RUN";
      break;
    case PS_SUSPEND:
      status = "SUSPEND";
      break;
    case PS_DEAD:
      status = "DEAD";
      break;
    case PS_ERROR:
      status = "ERROR";
      break;
    default:
      status = "UNKNOWN";
      break;
    }
    success = true;
    String outStr = ";";
    outStr += PMPacket.textField(cmd);
    if (success) {
      outStr += PMPacket.textField("SUCCESS");
      outStr += PMPacket.textField(pHandle);
      outStr += PMPacket.textField(commandLine);
      outStr += PMPacket.textField(pid);
    } else {
      outStr += PMPacket.textField("ERROR");
    }
    outStr += PMPacket.textField(status);
    outStr += ";";
    ByteBuffer outBuf = PMPacket.convertBuf(outStr);
    try {
      while (outBuf.hasRemaining()) {
        clientd.channel.write(outBuf);
      }
    } catch (Exception e) {
      logger.warn("Error replying to client:" + e, e);
      //e.printStackTrace();
    }
  }
}
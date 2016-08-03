package pmserver;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.*;
import java.nio.*;
import java.nio.channels.*; //import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Timer;
import java.nio.charset.*;
import java.net.*;
import org.apache.log4j.*;

import pmutils.*;

//import pmutils.PMTimerTask.eTask;

class Usage
{
  final static String message = "Usage: %s [-p OR --port=<listenPort>] [-d OR --dir=<workingDir>]\n"
      + "\t<port> is the port to listen on (10255)\n"
      + "\t<workingDir> is the directory to store process data (default ./data-hostname)\n";

  static void print(String progname)
  {
    System.err.printf(String.format(Usage.message, progname));
  }
}

enum eTimerTask
{
  E_Kill, E_Suspend, E_Resume, E_Signal, E_RecoverCleanup
};

public class PMServer
{

  static Charset _charset = Charset.forName(PMDefines.CHARSET);
  static CharsetEncoder _encoder = _charset.newEncoder();
  static CharsetDecoder _decoder = _charset.newDecoder();
  static Logger logger = Logger.getLogger("ProcMgr");
  static Selector _selector;
  static ArrayList<PMProcState> _procArray = new ArrayList<PMProcState>();
  static ArrayList<PMProcState> _removeProcs = new ArrayList<PMProcState>();
  static Timer serverTimer;
  static String nativeApp = null;
  static File dataPath = null;
  static String progname;
  static int listenPort;
  static String log4j_config;
  static String workingDir;
  static String hostname;
  static String osname;
  static String osarch;
  static Calendar todayCal;
  // static DateFormat dateFormat;
  static SimpleDateFormat dateFormat;

  static boolean getPacketFile(ByteBuffer inBuf, FileChannel chan, PMPacket pkt)
      throws Exception
  {
    // logger.debug("Enter");
    while (true) {
      int nRead;
      if (!inBuf.hasRemaining()) {
        inBuf.clear();
        try {
          nRead = chan.read(inBuf);
        } catch (Exception e) {
          nRead = -1;
          logger.warn(e, e);
        }
        if (nRead == -1) {
          return false;
        }
        inBuf.flip();
      }
      while (inBuf.hasRemaining()) {
        if (pkt.addInputBytes(inBuf)) {
          // Token is ready
          return true;
        }
      } // while
    } // while(true)
  } // getNextToken

  static void addHistory(PMPacket pkt, PMProcState pState) throws Exception
  {
    if (pkt.args.size() < 2) {
      logger.warn(pState.procHandle + ": malformed History record");
      return;
    }

    String str = PMPacket.convertStr(pkt.args.get(0));
    String arg1 = PMPacket.convertStr(pkt.args.get(1));
    if (str.compareToIgnoreCase("START") == 0) {
      pState.startTime = dateFormat.parse(arg1);
    } else if (str.compareToIgnoreCase("DEAD") == 0) {
      pState.pstate = Misc.procState.PS_DEAD;
      pState.endTime = dateFormat.parse(arg1);
    } else if (str.compareToIgnoreCase("SUSPEND") == 0) {
      if (pState.pstate != Misc.procState.PS_DEAD) {
        pState.pstate = Misc.procState.PS_SUSPEND;
      }
    } else if (str.compareToIgnoreCase("RESUME") == 0) {
      if (pState.pstate != Misc.procState.PS_DEAD) {
        pState.pstate = Misc.procState.PS_RUN;
      }
    } else if ((str.compareToIgnoreCase("EXEC") == 0) || (str.compareToIgnoreCase("RUN") == 0)) {
      pState.pstate = Misc.procState.PS_RUN;
    } else if (str.compareToIgnoreCase("ERRPOS") == 0) {
      pState.errPosition = Long.parseLong(arg1);
    } else if (str.compareToIgnoreCase("OUTPOS") == 0) {
      pState.outPosition = Long.parseLong(arg1);
    }
  }

  static void addState(PMPacket pkt, PMProcState pState) throws CharacterCodingException
  {
    if (pkt.args.size() < 2) {
      logger.warn(pState.procHandle + ": malformed State record");
      return;
    }
    String str = PMPacket.convertStr(pkt.args.get(0));
    String str2 = PMPacket.convertStr(pkt.args.get(1));

    if (str.compareToIgnoreCase("CMDLINE") == 0) {
      pState.commandLine = str2;
    } else if (str.compareToIgnoreCase("KILLSECONDS") == 0) {
      pState.killSeconds = Integer.parseInt(str2);
    } else if (str.compareToIgnoreCase("PID") == 0) {
      pState.processId = Integer.parseInt(str2);
    } else if (str.compareToIgnoreCase("HANDLE") == 0) {
      pState.procOffset = Integer.parseInt(str2);
    } else if (str.compareToIgnoreCase("MIX") == 0) {
      pState.mix = Boolean.parseBoolean(str2);
    }
  }

  static void readProcState(File subDir)
  {
    boolean hasValidFiles = false;
    String stateFiles[] = subDir.list();
    PMProcState pState = new PMProcState(nativeApp);
    ByteBuffer buf = ByteBuffer.allocate(PMDefines.BUFSIZE);
    pState.procDir = subDir;
    pState.procHandle = subDir.getName();
    FileChannel chan = null;
    try {
      for (int i = 0; i < stateFiles.length; i++) {
        File subFile = new File(subDir, stateFiles[i]);
        FileInputStream inStream = new FileInputStream(subFile);
        chan = inStream.getChannel();
        PMPacket pkt = new PMPacket(logger);
        PMPacket.initBuf(buf);
        if (stateFiles[i].compareToIgnoreCase("history.dat") == 0) {
          hasValidFiles = true;
          pState.histFile = subFile;
          while (getPacketFile(buf, chan, pkt)) {
            addHistory(pkt, pState);
            pkt = new PMPacket(logger);
          }
        } else if (stateFiles[i].compareToIgnoreCase("state.dat") == 0) {
          hasValidFiles = true;
          pState.stateFile = subFile;
          while (getPacketFile(buf, chan, pkt)) {
            addState(pkt, pState);
            pkt = new PMPacket(logger);
          }
        } else if (stateFiles[i].compareToIgnoreCase("stderr.dat") == 0) {
          hasValidFiles = true;
          pState.errFile = subFile;
        } else if (stateFiles[i].compareToIgnoreCase("stdout.dat") == 0) {
          hasValidFiles = true;
          pState.outFile = subFile;
        }
        inStream.close();
      } // for (stateFiles[])
    } catch (Exception e) {
      if (hasValidFiles) {
        pState.removeFiles();
      }
      logger.warn(e, e);
      return;
    } finally {
      if (chan != null) {
        try {
          chan.close();
          chan = null;
        } catch (IOException ie) {
          chan = null;
        }
      }
    }
    if (pState.isValid()) {
      _procArray.add(pState);
      long killTime = pState.startTime.getTime() + (pState.killSeconds * 1000);
      pState.recovered = true;
      if (pState.pstate != Misc.procState.PS_DEAD) {
        killTime -= todayCal.getTimeInMillis();
        killTime /= 1000;
        startProcessTimer(pState, (int) (killTime));
      }
    } else if (hasValidFiles) {
      // An invalid process directory is present - remove it.
      pState.removeFiles();
    }
    return;
  }

  static void restoreProcState(File dataDir)
  {
    String subDirs[] = dataDir.list();
    for (int i = 0; i < subDirs.length; i++) {
      File subDir = new File(dataDir, subDirs[i]);
      if (subDir.isDirectory()) {
        readProcState(subDir);
      }
    }
  }

  static ServerSocketChannel newServerChannel(int port) throws Exception
  {
    ServerSocketChannel newChannel = ServerSocketChannel.open();
    ServerSocket newSocket = newChannel.socket();
    newSocket.bind(new InetSocketAddress(port));
    newChannel.configureBlocking(false);
    return newChannel;
  }

  static void updateProcessStates()
  {
    Iterator<PMProcState> it = _procArray.listIterator();
    ByteBuffer buf = ByteBuffer.allocate(PMDefines.BUFSIZE);
    while (it.hasNext()) {
      PMProcState pProc = it.next();
      if (pProc.recovered && pProc.pstate != Misc.procState.PS_DEAD) {
        FileChannel chan = null;
        try {
          FileInputStream inStream = new FileInputStream(pProc.histFile);
          chan = inStream.getChannel();
          PMPacket pkt = new PMPacket(logger);
          PMPacket.initBuf(buf);
          while (getPacketFile(buf, chan, pkt)) {
            addHistory(pkt, pProc);
            pkt = new PMPacket(logger);
          }
          inStream.close();
        } catch (Exception e) {
          logger.warn(e, e);
          continue;
        } finally {
          if (chan != null) {
            try {
              chan.close();
              chan = null;
            } catch (IOException ie) {
              chan = null;
            }
          }
        }
        // If the state is now dead, check for waiting clients
        if (pProc.pstate == Misc.procState.PS_DEAD) {
          pProc.notifyWaiters();
        }
      } else if (pProc.pstate == Misc.procState.PS_DEAD) {
        // Remove the state of anything that is DEAD and older than 24 hours
        // Assumes todayCal is up-to-date
        long removeTime = pProc.endTime.getTime() + (24 * 60 * 60 * 1000);
        if (todayCal.getTimeInMillis() >= removeTime) {
          if (!pProc.removeFiles()) {
            // the dir can fail to be removed - try again later
            _removeProcs.add(pProc);
          }
          it.remove(); // Removes from _procArray
        }
      }
    }
    Iterator<PMProcState> removeIt = _removeProcs.listIterator();
    while (removeIt.hasNext()) {
      PMProcState pProc = removeIt.next();
      if (pProc.removeFiles()) {
        removeIt.remove();
      }
    }
  }

  static PMProcState findProcess(String procHandle)
  {
    Iterator<PMProcState> it = _procArray.listIterator();
    PMProcState pLast = null;
    while (it.hasNext()) {
      PMProcState pProc = it.next();
      // For debug purposes, a procId of 0 returns the first process
      if (procHandle.compareTo("0") == 0 || procHandle.compareTo(pProc.procHandle) == 0) {
        return pProc;
      }
      pLast = pProc;
    }
    if (procHandle.compareTo("00") == 0) {
      return pLast;
    } else {
      return null;
    }
  }

  static void handleClientCommand(PMClientd clientd)
  {
    if (clientd.command.compareToIgnoreCase("exec") == 0) {
      handleExecCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("status") == 0) {
      handleStatusCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("pid") == 0) {
      handlePidCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("kill") == 0) {
      handleKillCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("suspend") == 0) {
      handleSuspendCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("resume") == 0) {
      handleResumeCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("remove") == 0) {
      handleRemoveCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("stdout") == 0) {
      handleStdoutCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("stderr") == 0) {
      handleStderrCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("dump") == 0) {
      handleDumpCmd(clientd);
    } else if (clientd.command.compareToIgnoreCase("wait") == 0) {
      handleWaitCmd(clientd);
    }
  }

  private static void handleWaitCmd(PMClientd clientd)
  {
    String pHandle = "";
    PMProcState pProc;
    String status = null;
    clientd.args = clientd.packet.args.get(0);

    try {
      pHandle = PMPacket.convertStr(clientd.args);
      pProc = findProcess(pHandle);
      if (pProc == null) {
        status = "not found";
      } else {
        clientd.pState = pProc;
        pProc.clientWaitList.add(clientd);
        if (pProc.pstate == Misc.procState.PS_DEAD) {
          pProc.notifyWaiters();
        }
      }
    } catch (Exception e) {
      logger.warn(e, e);
    }
    if (status != null) {
      String outStr = ";";
      outStr += PMPacket.textField("WAIT");
      outStr += PMPacket.textField("ERROR");
      outStr += PMPacket.textField(status);
      outStr += ";";
      ByteBuffer outBuf = PMPacket.convertBuf(outStr);
      try {
        while (outBuf.hasRemaining()) {
          clientd.channel.write(outBuf);
        }
      } catch (Exception e) {
        logger.warn("Error replying to client:" + e, e);
        e.printStackTrace();
      } // try-catch
    } // if (status != null)
  } // handleWaitCmd

  // Dump - send status of all known processes
  // First, output all the field names, then output field values
  // for each process
  static void handleDumpCmd(PMClientd clientd)
  {
    String pHandle;
    String pid;
    String status;
    String commandLine;
    String reply;
    String startTime;
    String endTime;
    ByteBuffer outBuf;

    // Output title line
    reply = ";";
    reply += PMPacket.textField("HANDLE");
    reply += PMPacket.textField("PID");
    reply += PMPacket.textField("STATUS");
    reply += PMPacket.textField("STARTTIME");
    reply += PMPacket.textField("ENDTIME");
    reply += PMPacket.textField("MIX");
    reply += PMPacket.textField("RECOVERED");
    reply += PMPacket.textField("COMMAND");
    reply += ";";
    outBuf = PMPacket.convertBuf(reply);
    try {
      while (outBuf.hasRemaining()) {
        clientd.channel.write(outBuf);
      }
    } catch (Exception e) {
      logger.warn("Error replying to client:" + e, e);
    }

    Iterator<PMProcState> it = _procArray.listIterator();
    while (it.hasNext()) {
      PMProcState pProc = it.next();
      pHandle = pProc.procHandle;
      pid = Integer.toString(pProc.processId);
      switch (pProc.pstate) {
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
      commandLine = pProc.commandLine;
      startTime = dateFormat.format(pProc.startTime);
      endTime = dateFormat.format(pProc.endTime);

      String mix = Boolean.toString(pProc.mix);
      String recovered = Boolean.toString(pProc.recovered);

      reply = ";";
      reply += PMPacket.textField(pHandle);
      reply += PMPacket.textField(pid);
      reply += PMPacket.textField(status);
      reply += PMPacket.textField(startTime);
      reply += PMPacket.textField(endTime);
      reply += PMPacket.textField(mix);
      reply += PMPacket.textField(recovered);
      reply += PMPacket.textField(commandLine);
      reply += ";";
      outBuf = PMPacket.convertBuf(reply);
      try {
        while (outBuf.hasRemaining()) {
          clientd.channel.write(outBuf);
        }
      } catch (Exception e) {
        logger.warn("Error replying to client:" + e, e);
        continue;
      }
    }
    try {
      clientd.channel.socket().getOutputStream().flush();
      clientd.channel.close();
    } catch (Exception e) {
      logger.warn(e, e);
    }
  }

  static void handleExecCmd(PMClientd clientd)
  {
    int newTimeout = -1;
    String[] envArray = null;
    boolean mix = false;
    try {
      ByteBuffer nextarg;
      while (true) {
        nextarg = clientd.packet.args.get(0);
        if (nextarg.get(0) == '-') {
          switch (nextarg.get(1)) {
          case 't':
            nextarg.position(2);
            newTimeout = PMPacket.convertInt(nextarg);
            break;
          case 'e':
            clientd.packet.args.remove(0);
            ByteBuffer envBuf = clientd.packet.args.get(0);
            envArray = PMPacket.parseEnvBuf(envBuf);
            break;
          case 'm':
            mix = true;
            break;
          default:
            break;
          }
          clientd.packet.args.remove(0);
        } else {
          // On exit from the loop, packet.args will have just the command and
          // arguments
          break;
        }
      }
      clientd.cmdArgs = clientd.packet.args;

      clientd.cstate = PMClientd.cState.CS_WAIT_EXEC_RESULTS;
      sendExecCmd(clientd, newTimeout, envArray, mix);
    } catch (Exception e) {
      clientd.cstate = PMClientd.cState.CS_FIN;
      logger.warn(e, e);
      e.printStackTrace();
      String errStr = ";";
      errStr += PMPacket.textField("EXEC");
      errStr += PMPacket.textField("ERROR");
      errStr += PMPacket.textField(e.getMessage());
      errStr += ";";
      try {
        clientd.channel.write(PMPacket.convertBuf(errStr));
      } catch (Exception e2) {
        logger.warn(e2, e2);
        e2.printStackTrace();
      }
    }
  } // handleExecCmd()

  static void handleStatusCmd(PMClientd clientd)
  {
    String pHandle = "";
    PMProcState pProc = null;
    String status = null;
    clientd.args = clientd.packet.args.get(0);

    try {
      pHandle = PMPacket.convertStr(clientd.args);
      pProc = findProcess(pHandle);
    } catch (Exception e) {
      logger.warn(e, e);
      status = "Invalid Parameter";
    }
    if (pProc == null) {
      String outStr = ";";
      status = "not found";
      outStr += PMPacket.textField("ERROR");
      outStr += PMPacket.textField(status);
      outStr += ";";
      ByteBuffer outBuf = PMPacket.convertBuf(outStr);
      try {
        while (outBuf.hasRemaining()) {
          clientd.channel.write(outBuf);
        }
      } catch (Exception e) {
        logger.warn("Error replying to client:" + e, e);
        // e.printStackTrace();
      }
    } else {
      pProc.sendState(clientd, "STATUS");
    }
  } // handleStatusCmd()

  static void handleRemoveCmd(PMClientd clientd)
  {
    boolean success = false;
    String pHandle = "";
    PMProcState pProc = null;
    String status = null;
    String commandLine = "?";
    String pid = "";

    try {
      clientd.args = clientd.packet.args.get(0);
      pHandle = PMPacket.convertStr(clientd.args);
      pProc = findProcess(pHandle);
      if (pProc == null) {
        status = "not found";
      } else {
        pHandle = pProc.procHandle;
        pid = Integer.toString(pProc.processId);
      }
    } catch (Exception e) {
      logger.warn(e, e);
      status = "Invalid Parameter";
    }
    if (pProc != null) {
      if (pProc.pstate != Misc.procState.PS_DEAD) {
        status = "Process is not DEAD";
      } else {
        commandLine = pProc.commandLine;
        if (!_procArray.remove(pProc)) {
          logger.error(pProc.procHandle + ": PROC NOT deleted");
        }
        if (!pProc.removeFiles()) {
          // Save for later processing
          _removeProcs.add(pProc);
        }
        success = true;
        status = "OK";
      }
    }
    String outStr = ";";
    outStr += PMPacket.textField("REMOVE");
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
        // logger.assertLog(!outBuf.hasRemaining(),
        // "incomplete client socket write");
      }
    } catch (Exception e) {
      logger.warn("Error replying to client:" + e, e);
      e.printStackTrace();
    }
  }

  static void handleKillCmd(PMClientd clientd)
  {
    PMProcState pProc = doSignalCmd("KILL", clientd);
    if (pProc != null) {
      if (pProc.recovered && pProc.pstate != Misc.procState.PS_DEAD) {
        pProc.pstate = Misc.procState.PS_DEAD;
        pProc.endTime = todayCal.getTime();
        String buf = ";";
        buf += PMPacket.textField("DEAD");
        buf += PMPacket.textField(dateFormat.format(todayCal.getTime()));
        Misc.writeHistory(pProc.histFile, buf);
      }
    }
  }

  static void handlePidCmd(PMClientd clientd)
  {
    doSignalCmd("PID", clientd);
  }

  static void handleSuspendCmd(PMClientd clientd)
  {
    PMProcState pProc = doSignalCmd("SUSPEND", clientd);
    if (pProc != null && pProc.pstate == Misc.procState.PS_RUN) {
      pProc.pstate = Misc.procState.PS_SUSPEND;
    }
  }

  static void handleResumeCmd(PMClientd clientd)
  {
    PMProcState pProc = doSignalCmd("RESUME", clientd);
    if (pProc != null && pProc.pstate == Misc.procState.PS_SUSPEND) {
      pProc.pstate = Misc.procState.PS_RUN;
    }
  }

  static void handleStderrCmd(PMClientd clientd)
  {
    doProcOutputCmd("STDERR", clientd);
  }

  static void handleStdoutCmd(PMClientd clientd)
  {
    doProcOutputCmd("STDOUT", clientd);
  }

  static void doProcOutputCmd(String cmd, PMClientd clientd)
  {
    boolean success = false;
    String pHandle = "";
    PMProcState pProc = null;
    String status = null;
    File fileToRead = null;
    boolean increment = false;
    boolean useOutput;
    long currentPosition = 0;

    try {
      while (true) {
        ByteBuffer nextarg = clientd.packet.args.remove(0);
        if (nextarg.get(0) == '-') {
          switch (nextarg.get(1)) {
          case 'i':
            increment = true;
            break;
          default:
            break;
          }
        } else {
          clientd.args = nextarg;
          break;
        }
      }

      pHandle = PMPacket.convertStr(clientd.args);
      pProc = findProcess(pHandle);
      if (pProc == null) {
        status = String.format(" %s NOT FOUND", pHandle);
      }
    } catch (Exception e) {
      logger.warn(e, e);
      status = String.format("%s: Invalid Parameter", pHandle);
    }

    if (cmd.compareToIgnoreCase("STDOUT") == 0) {
      useOutput = true;
    } else {
      useOutput = false;
    }
    if (status == null) {
      if (useOutput) {
        fileToRead = pProc.outFile;
      } else {
        fileToRead = pProc.errFile;
      }
      if (increment) {
        currentPosition = useOutput ? pProc.outPosition : pProc.errPosition;
      }
      // Open the file and send its contents to the client
      switch (pProc.pstate) {
      case PS_RUN:
      case PS_SUSPEND:
      case PS_DEAD:
        FileChannel readChan = null;
        try {
          FileInputStream inStream = new FileInputStream(fileToRead);
          readChan = inStream.getChannel();
          readChan.position(currentPosition);
          ByteBuffer readBuf = ByteBuffer.allocate(PMDefines.BIG_BUFSIZE);
          int nRead = 0;
          while (nRead >= 0) {
            readBuf.clear();
            nRead = readChan.read(readBuf);
            if (nRead > 0) {
              readBuf.flip();
              while (readBuf.hasRemaining()) {
                clientd.channel.write(readBuf);
                // logger.assertLog(!readBuf.hasRemaining(),
                // "incomplete write on client socket");
              }
            }
          }
          currentPosition = readChan.position();
          success = true;
          if (increment) {
            String histStr = ";";
            if (useOutput) {
              histStr += PMPacket.textField("OUTPOS");
              pProc.outPosition = currentPosition;
            } else {
              histStr += PMPacket.textField("ERRPOS");
              pProc.errPosition = currentPosition;
            }
            histStr += PMPacket.textField(Long.toString(currentPosition));
            // Save position to history file
            Misc.writeHistory(pProc.histFile, histStr);
          }
          inStream.close();
        } catch (Exception e) {
          status = String.format("Error accessing file %s", fileToRead.getAbsolutePath());
          break;
        } finally {
          if (readChan != null) {
            try {
              readChan.close();
              readChan = null;
            } catch (IOException ie) {
              readChan = null;
            }
          }
        }
        // OK
        break;
      default:
        status = "INVALID STATUS";
        break;
      }
    }

    if (!success || status != null) {
      try {
        status = String.format("Server:%s - %s\n", cmd, status);
        clientd.channel.write(PMPacket.convertBuf(status));
      } catch (Exception e) {
        logger.warn(e, e);
      }
    }

    try {
      clientd.channel.socket().getOutputStream().flush();
      clientd.channel.close();
    } catch (Exception e) {
      logger.warn(e, e);
    }

    return;
  }

  static void sendSignal(String signame, PMProcState pProc) throws IOException
  {
    String cmdline = String.format("%s %s -H %s %d", nativeApp, signame, pProc.histFile
        .getAbsolutePath(), pProc.processId);
    logger.debug(cmdline);
    Runtime.getRuntime().exec(cmdline);
  }

  static PMProcState doSignalCmd(String signal, PMClientd clientd)
  {
    boolean success = false;
    String pHandle = "";
    PMProcState pProc = null;
    String status = "";
    String commandLine = "?";
    String pid = "";
    clientd.args = clientd.packet.args.get(0);
    String warning = "";
    boolean skipProcessing = false;

    try {
      pHandle = PMPacket.convertStr(clientd.args);
      pProc = findProcess(pHandle);
      if (pProc == null) {
        status = "not found";
      } else {
        pHandle = pProc.procHandle;
        pid = Integer.toString(pProc.processId);
        if (signal.compareToIgnoreCase("SUSPEND") == 0 && pProc.pstate != Misc.procState.PS_RUN) {
          warning = "Attempting to suspend a non-running process";
        } else if (signal.compareToIgnoreCase("RESUME") == 0
            && pProc.pstate != Misc.procState.PS_SUSPEND) {
          warning = "Attempting to resume a non-suspended process";
        } else if (signal.compareToIgnoreCase("KILL") == 0
            && pProc.pstate == Misc.procState.PS_DEAD) {
          warning = "Dead process not killed";
          status = warning;
          skipProcessing = true;
        }
        // If inappropriate state, log a warning but continue
        if (warning.length() != 0) {
          logger.warn(warning);
        }
        if (!skipProcessing) {
          try {
            commandLine = pProc.commandLine;
            if (signal.compareToIgnoreCase("PID") == 0) {
              status = pid;
            } else {
              sendSignal(signal, pProc);
              status = "OK";
            }
            success = true;
          } catch (IOException ie) {
            logger.warn(ie, ie);
            status = ie.getMessage();
          }
        }
      }
    } catch (Exception e) {
      logger.warn(e, e);
      status = "Invalid Parameter";
    }

    String outStr = ";";
    outStr += PMPacket.textField(signal);
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
    try {
      ByteBuffer outBuf = PMPacket.convertBuf(outStr);
      // TODO select for writing if the client socket doesn't complete on 1
      // write
      // Do this on all client socket writes
      while (outBuf.hasRemaining()) {
        clientd.channel.write(outBuf);
      }
    } catch (Exception e) {
      logger.warn("Error replying to client:" + e, e);
      e.printStackTrace();
    }
    if (success) {
      return pProc;
    } else {
      return null;
    }
  } // doSignalCmd()

  private static void sendExecCmd(PMClientd clientd, int newTimeout, String[] env, boolean mix)
      throws Exception
  {
    PMProcState procd = new PMProcState(nativeApp);
    procd.procOffset = (int) todayCal.getTimeInMillis()
        + ((int) Math.round(Math.random() * (double) Integer.MAX_VALUE));
    procd.procHandle = Integer.toString(Math.abs(procd.procOffset));
    procd.procDir = new File(workingDir + "/" + procd.procHandle);
    procd.outFile = new File(procd.procDir, "stdout.dat");
    procd.mix = mix;
    if (procd.mix) {
      procd.errFile = new File(procd.procDir, "stdout.dat"); // mix - use the
                                                             // same file
    } else {
      procd.errFile = new File(procd.procDir, "stderr.dat");
    }
    procd.histFile = new File(procd.procDir, "history.dat");
    procd.stateFile = new File(procd.procDir, "state.dat");

    procd.procDir.mkdirs();

    String writeString = ";";
    todayCal = Calendar.getInstance();
    procd.startTime = todayCal.getTime();
    writeString += PMPacket.textField("START");
    writeString += PMPacket.textField(dateFormat.format(procd.startTime));
    Misc.writeHistory(procd.histFile, writeString);

    ArrayList<ByteBuffer> cmdList = new ArrayList<ByteBuffer>();
    cmdList.add(PMPacket.convertBuf(nativeApp));
    cmdList.add(PMPacket.convertBuf(clientd.command));
    cmdList.add(PMPacket.convertBuf("-d"));
    cmdList.add(PMPacket.convertBuf(procd.procDir.getAbsolutePath()));
    if (procd.mix) {
      cmdList.add(PMPacket.convertBuf("-M"));
    }
    cmdList.addAll(clientd.cmdArgs);
    String cmdStr[] = PMPacket.convertStrArray(cmdList);

    ExecCommand eCmd = new ExecCommand(cmdStr);
    eCmd.execute(_selector, procd, env);
    logger.debug(PMPacket.convertStr(cmdStr));
    // Make cmdStr
    cmdStr = PMPacket.convertStrArray(clientd.cmdArgs);
    procd.commandLine = PMPacket.convertStr(cmdStr);
    procd.stderr = eCmd.getErrStream();
    procd.stdout = eCmd.getOutStream();
    procd.stdin = eCmd.getInStream();
    procd.clientd = clientd;
    if (newTimeout > -1) {
      procd.killSeconds = newTimeout;
    }
    procd.mix = mix;

    _procArray.add(procd);
  }

  static boolean validCommand(String tokenString)
  {
    for (int i = 0; i < PMDefines.CMDLIST.length; i++) {
      if (tokenString.compareToIgnoreCase(PMDefines.CMDLIST[i]) == 0) {
        return true;
      }
    }
    return false;
  }

  static void handleClientDisconnect(PMClientd clientd)
  {
    Socket sock = clientd.channel.socket();
    InetAddress iAddr = sock.getInetAddress();
    logger.info(String.format("Disconnected from %s(%s):%d(0x%04X)", iAddr.getHostAddress(),
        iAddr.getHostName(), sock.getPort(), sock.getPort()));
    if (clientd.pState != null) {
      clientd.pState.clientWaitList.remove(clientd);
      clientd.pState = null;
    }
  } // handleClientDisconnect()

  static void handleClientSocket(SelectionKey key) throws Exception
  {
    // logger.debug("Enter");
    // Accept and continue
    if (key.isReadable()) {
      int nBytes;
      PMClientd clientd = (PMClientd) key.attachment();
      SocketChannel client = (SocketChannel) key.channel();
      ByteBuffer buf = clientd.buffer;

      // Read and process input
      try {
        nBytes = client.read(buf);
      } catch (IOException e) {
        nBytes = -1; // Force -1 on error
        logger.warn("Client socket IO: " + e.getMessage());
      }
      // logger.debug("Read " + nBytes + " bytes");
      if (nBytes < 0) {
        client.close();
        key.cancel();
        handleClientDisconnect(clientd);
      } else if (nBytes > 0) {
        buf.flip();
        while (buf.hasRemaining()) {
          if (clientd.packet.addInputBytes(buf)) {
            if (!clientd.validated) {
              String idStr = PMPacket.convertStr(clientd.packet.args.remove(0));
              if (PMDefines.PMCLIENT_ID.compareToIgnoreCase(idStr) == 0) {
                clientd.validated = true;
              }
            } else {
              clientd.command = PMPacket.convertStr(clientd.packet.args.remove(0));
              handleClientCommand(clientd);
            }
            clientd.packet = new PMPacket(logger);
          } // if (addBytes)
        } // while (hasRemaining())
        buf.clear();
      } // if (nBytes > D0)
    } else {
      logger.info(String.format("Not Readable = 0x%04x", key.interestOps()));
      Thread.sleep(1000);
    } // if (key.isReadable())
    // logger.debug("Exit");
  } // handleClientSocket

  static void handleServerSocket(SelectionKey key) throws Exception
  {
    // Accept and continue
    // logger.debug("Enter");
    if (key.isAcceptable()) {
      // Accept and register the socket
      ServerSocketChannel server = (ServerSocketChannel) key.channel();
      SocketChannel client = (SocketChannel) server.accept();
      Socket sock = client.socket();
      InetAddress iAddr = sock.getInetAddress();
      client.configureBlocking(false);
      String writeString = ";";
      writeString += PMPacket.textField(PMDefines.PMSERVER_ID);
      writeString += PMPacket.textField(String.format("%06x", PMDefines.PM_VERSION));
      writeString += ";";
      ByteBuffer outBuf = ByteBuffer.wrap(writeString.getBytes());
      outBuf.rewind();
      while (outBuf.hasRemaining()) {
        client.write(outBuf);
      }
      ByteBuffer buf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
      PMPacket pkt = new PMPacket(logger);
      PMClientd clientd = new PMClientd(client, pkt, buf);

      client.register(key.selector(), SelectionKey.OP_READ, clientd);
      logger.debug(String.format("Connected to %s(%s):%d(0x%04X)", iAddr.getHostAddress(), iAddr
          .getHostName(), sock.getPort(), sock.getPort()));
    }
    // logger.debug("Exit");
  } // handleServerSocket

  static void handleSelectableStream(SelectionKey key) throws Exception
  {
    SelectableStream stream = (SelectableStream) key.attachment();
    ByteBuffer buf = stream.getBuffer();
    ExecCommand eCmd = (ExecCommand) stream.getClientd();
    PMProcState procState = (PMProcState) eCmd.userObj;
    String strName = stream.getName();
    int count;

    // logger.debug(String.format("%s 0x%04x", stream.getName(),
    // key.readyOps()));
    if (key.isValid() && key.isReadable()) {
      ReadableByteChannel chan = (ReadableByteChannel) stream.getSelectableChannel();
      // Read data into buffer
      buf.clear();
      count = chan.read(buf);
      if (count > 0) {
        buf.flip();
        if ((strName.compareToIgnoreCase("stdout") == 0) && procState.processId == -1) {
          // Get the process ID
          if (procState.packet.addInputBytes(buf)) {
            // Got the ID
            // Receive: ;\<procid>;;
            // Send: ;EXEC;SUCCESS;\<procid>;;
            procState.processId = PMPacket.convertInt(procState.packet.args.get(0));
            if (procState.processId == 0) {
              procState.errBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
              procState.errBuf.clear();
            } else {
              procState.pstate = Misc.procState.PS_RUN;
              // Start the process timer
              if (procState.killSeconds != 0) {
                startProcessTimer(procState, procState.killSeconds);
              }
              FileOutputStream writeStream = new FileOutputStream(procState.stateFile, true);
              String writeString = ";";
              writeString += PMPacket.textField("CMDLINE");
              writeString += PMPacket.textField(procState.commandLine);
              writeString += ";\n";
              writeString += ";" + PMPacket.textField("KILLSECONDS");
              writeString += PMPacket.textField(Integer.toString(procState.killSeconds));
              writeString += ";\n";
              writeString += ";" + PMPacket.textField("PID");
              writeString += PMPacket.textField(Integer.toString(procState.processId));
              writeString += ";\n";
              writeString += ";" + PMPacket.textField("HANDLE");
              writeString += PMPacket.textField(procState.procHandle);
              writeString += ";\n";
              writeString += ";" + PMPacket.textField("MIX");
              writeString += PMPacket.textField(Boolean.toString(procState.mix));
              writeString += ";\n";
              ByteBuffer outBuf = PMPacket.convertBuf(writeString);
              writeStream.write(outBuf.array(), outBuf.position(), outBuf.limit()
                  - outBuf.position());
              writeStream.close();

              String response = ";";
              response += PMPacket.textField("EXEC");
              response += PMPacket.textField("SUCCESS");
              response += PMPacket.textField(procState.procHandle);
              response += PMPacket.textField(Integer.toString(procState.processId));
              response += ";";
              logger.debug("Sending " + response);
              outBuf = PMPacket.convertBuf(response);
              procState.clientd.channel.write(outBuf);
            }
          } // if (addBytes)
        } // if (stdout)
        if (procState.processId == 0 && strName.compareToIgnoreCase("stderr") == 0) {
          // There was an error spawning the process - collect stderr until a
          // newline
          // We will ignore stdout at this point
          while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == '\n' || b == '\r') {
              // send contents to client and close.
              procState.errBuf.flip();
              String response = ";";
              response += PMPacket.textField("EXEC");
              response += PMPacket.textField("ERROR");
              response += PMPacket.binaryField(procState.errBuf);
              response += ";";
              logger.debug("Sending " + response);
              ByteBuffer outBuf = PMPacket.convertBuf(response);
              procState.clientd.channel.write(outBuf);
              count = -1; // Cause the process to be deleted
            } else {
              procState.errBuf.put(b);
            }
          }
        }
        buf.clear();
      }
      if (count < 0) {
        logger.info("Close(" + stream.getName() + ")");
        chan.close();
        key.cancel();
        SelectableChannel inChannel = eCmd.getInStream().getSelectableChannel();
        if (inChannel.isOpen()) {
          logger.info("Close(" + eCmd.getInStream().getName() + ")");
          inChannel.close();
          if (inChannel.keyFor(eCmd.getSelector()) != null) {
            inChannel.keyFor(eCmd.getSelector()).cancel();
          }
        } // if (inChannel.isOpen())
        if (procState.pstate != Misc.procState.PS_DEAD) {
          if (procState.timerTask != null) {
            procState.timerTask.cancel();
          }
          procState.pstate = Misc.procState.PS_DEAD;
          procState.endTime = todayCal.getTime();
          procState.notifyWaiters();
        }
        try {
          if (strName.compareToIgnoreCase("stdout") == 0 && procState.outStream != null) {
            procState.outStream.close();
            procState.outStream = null;
          } else if (procState.errStream != null) {
            procState.errStream.close();
            procState.errStream = null;
          }
        } catch (NullPointerException e) {
          logger.error("Stream " + strName + " out = " + procState.outStream + "; err = "
              + procState.errStream);
        }
        if (procState.processId == 0 && procState.outStream == null
            && procState.errStream == null) {
          procState.removeFiles();
          _procArray.remove(procState);
        }
      } // if (count)
    } // if (key.isReadable)
    if (key.isValid() && key.isWritable()) {
      logger.debug(stream.getName() + " isWritable");
      if (procState.pstate != Misc.procState.PS_DEAD) {
        WritableByteChannel chan = (WritableByteChannel) stream.getSelectableChannel();
        if (buf.hasRemaining()) {
          try {
            chan.write(buf);
          } catch (ClosedChannelException e) {
            logger.warn(e, e);
            buf.position(buf.limit()); // clear any remaining
            procState.pstate = Misc.procState.PS_DEAD;
            procState.endTime = todayCal.getTime();
            procState.notifyWaiters();
          }
        }
        if (!buf.hasRemaining()) {
          key.cancel(); // Nothing left to write
        }
      } // if (!isDead)
    } // if (isValid && isWritable)
  }// handleSelectableStream

  // Schedule a timer task to run on the server Timer thread
  static void startProcessTimer(PMProcState pState, int tSeconds)
  {
    // Schedule timer task on PMServer timer thread
    PMTimerHandler handler = new PMTimerHandler(eTimerTask.E_Kill);
    pState.timerTask = new PMTimerTask(pState, handler);
    if (tSeconds < 0) {
      tSeconds = 0;
    }
    serverTimer.schedule(pState.timerTask, tSeconds * 1000);

  }

  static void cmdOptions(String args[]) throws Exception
  {
    try {
      int argId;
      LongOpt[] longopts = new LongOpt[3];
      longopts[0] = new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p');
      longopts[1] = new LongOpt("dir", LongOpt.REQUIRED_ARGUMENT, null, 'd');
      longopts[2] = new LongOpt("log4j", LongOpt.REQUIRED_ARGUMENT, null, 'L');

      Getopt g = new Getopt(progname, args, "+:p:d:L:", longopts);
      g.setOpterr(false); // We'll do our own error handling

      while ((argId = g.getopt()) != -1) {
        switch (argId) {
        case 'p':
          // Host name
          listenPort = Integer.parseInt(g.getOptarg());
          break;
        case 'd':
          // Session ID
          workingDir = g.getOptarg();
          break;
        case 'L':
          // name for log4j configuration file
          log4j_config = g.getOptarg();
          break;
        case ':':
          // Missing option arg
          Usage.print(progname);
          throw new Exception("Missing argument");
          // break;
        case '?':
          // Unknown option
          Usage.print(progname);
          throw new Exception("Unknown option");
          // break;
        default:
          logger.warn("Unexpected getopt error: <" + argId + ">");
          // shouldn't happen
          break;
        } // switch (argId)
      } // while (argId)
    } catch (Exception e) {
      logger.error(e, e);
      e.printStackTrace();
      throw e;
    } // try-catch
  } // cmdOptions

  public static void main(String args[]) //throws Exception
  {
    // ExecCommand eCmd;
    TFTPServer _tftpServer = null;
    try {
      cmdOptions(args);
    } catch (Exception e) {
      System.err.println("cmdOptions: " + e.getMessage());
      logger.error("cmdOptions: " + e.getMessage());
      return;
    }
    if (log4j_config == null) {
      log4j_config = "server.log4j.properties";
    }
    ServerSocketChannel cmdSocket = null;
    PropertyConfigurator.configure(log4j_config);
    RollingFileAppender app = (RollingFileAppender) Logger.getRootLogger().getAppender(
        "rollingFile");
    progname = Thread.currentThread().getStackTrace()[1].getClassName();

    todayCal = Calendar.getInstance();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
    InetAddress addr = null;
    try {
      addr = InetAddress.getLocalHost();
    }
    catch (UnknownHostException uhe) {
      System.err.println("getLocalHost: " + uhe.getMessage());
      logger.error("getLocalHost: " + uhe.getMessage());
      return;
    }
    hostname = addr.getCanonicalHostName();
    osname = System.getProperty("os.name");
    osarch = System.getProperty("os.arch");
    // Replace all space characters with underscore
    logger.info("Replacing spaces with underscores");
    hostname = hostname.replaceAll(" ", "_");
    osname = osname.replaceAll(" ", "_");
    osarch = osarch.replaceAll(" ", "_");
    // create the directories for data, log and output
    (new File(hostname + "/data/")).mkdirs();
    (new File(hostname + "/logs/")).mkdirs();

    app.setFile(hostname + "/logs/PMServer.log");
    app.activateOptions();
    String hostInfo = String.format("%s/%s/%s", hostname, osname, osarch);
    String startStr = String.format("Start %s on %s at %s - 0x%08x", progname, hostInfo,
        dateFormat.format(todayCal.getTime()), todayCal.getTimeInMillis());
    logger.info(startStr);
    logger.info(String.format("Working dir = %s", System.getProperty("user.dir")));
    if (listenPort == 0) {
      listenPort = PMDefines.PMSERVER_LISTEN_PORT;
    }
    if (workingDir == null) {
      workingDir = String.format("%s/data", hostname);
    }
    try {
      dataPath = new File(workingDir);
      nativeApp = String.format("helper/PM%s-%s", osname, osarch);
      File appFile = new File(nativeApp);
      File appFile2 = new File(nativeApp + ".exe");
      if (!appFile.canRead() && !appFile2.canRead()) {
        System.err.println(nativeApp + " not found - exiting");
        logger.error(nativeApp + " not found - exiting");
        System.exit(-1);
      }
      dataPath.mkdirs();
      serverTimer = new Timer(true); // Create daemon timer thread
      _selector = Selector.open();

      // Open command socket
      try {
        cmdSocket = newServerChannel(listenPort);
      } catch (BindException be) {
        System.err.printf("Bind to TCP Port %d: <%s>\n", listenPort, be.getMessage());
        logger.error("Bind to port " + listenPort + ": " + be.getMessage());
        System.exit(-1);
      }
      cmdSocket.register(_selector, SelectionKey.OP_ACCEPT, cmdSocket);
      PrintStream tftpLogStream = new PrintStream(new File(hostname+"/logs/TFTPServer.log"));
      try {
        _tftpServer = new TFTPServer(new File(hostname), new File(hostname), listenPort + 1,
            TFTPServer.ServerMode.GET_AND_PUT, tftpLogStream, tftpLogStream);
        _tftpServer.setSocketTimeout(2000);
      } catch (SocketException se) {
        _tftpServer = null;
        System.err.printf("Error creating TFTP server: <%s>\n", se.getMessage());
        System.err.printf("Continuing without TFTP support\n");
      }
      restoreProcState(dataPath);
    } catch (Exception e) {
        //e.printStackTrace();
        logger.error(e, e);
        System.exit(-1);
    } 
      // logger.debug("Entering Selector loop");
    int eventcount = 0;
    int selecterr = 0;
      while (!_selector.keys().isEmpty()) {
        int nKeys;
        try {
          nKeys = _selector.select(60000);
        } catch (IOException ie) {
          System.err.println("Select: " + ie.getMessage());
          logger.warn("Select: " + ie.getMessage());
          selecterr++;
          if (selecterr > 10) {
            System.err.println("Too many consecutive Select errors - exiting...");
            logger.error("Too many consecutive Select errors - exiting...");
            break;
          }
          nKeys = 0;
        }
        selecterr = 0;
        if (nKeys == 0 || eventcount >= 60) {
          if (eventcount > 120) {
            logger.info("Event trigger");
          }
          eventcount = 0;
          todayCal = Calendar.getInstance();
          String timeStamp = String.format("%s - 0x%08x",
              dateFormat.format(todayCal.getTime()), todayCal.getTimeInMillis());
          logger.info(timeStamp);
          updateProcessStates();
          if (nKeys == 0) {
            continue; // Nothing to do
          }
        }
        eventcount++;
        Iterator<SelectionKey> it = _selector.selectedKeys().iterator();
        while (it.hasNext()) {
          SelectionKey key = (SelectionKey) it.next();
          // String objClass = key.attachment().getClass().getName();
          String objClass = key.channel().getClass().getName();
          // logger.debug(String.format("Selected object = <%s>", objClass));
          try {
            if (objClass.contains("SourceChannel")) {
              handleSelectableStream(key);
            } else if (objClass.contains("ServerSocketChannel")) {
              handleServerSocket(key);
            } else if (objClass.contains("SocketChannel")) {
              handleClientSocket(key);
            } else {
              Exception e = new Exception("Unrecognized object selected: " + objClass);
              throw e;
            }
          } catch (Exception e) {
            System.err.println("Error handling selected channel: " + e.getMessage());
            logger.warn("Error handling selected channel: " + e.getMessage());
          }
          it.remove(); // Remove from list of selected descriptors
        } // while (it.hasNext())
      } // while (!selector.keys().isEmpty())
      // logger.info("Exit Selector loop");
    if (_tftpServer != null) {
      _tftpServer.shutdown();
    }
  } // main

  /*****************************************************************/
} // class PMServer

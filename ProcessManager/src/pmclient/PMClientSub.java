package pmclient;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
//import java.util.ArrayList;
import java.util.Timer;

import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPClient;
import org.apache.log4j.Logger;

import pmutils.PMDefines;
import pmutils.PMPacket;

public class PMClientSub
{
  protected String hostname = null;
  protected String sessionId = PMDefines.PMCLIENT_SESSION_NAME;
  protected String destPort = String.format("%d", PMDefines.PMSERVER_LISTEN_PORT);
  protected boolean verbose = false;
  protected String progname;
  protected Logger subLogger;// = Logger.getLogger("ProcMgr");
  protected SocketChannel socketChannel;

  Charset charset = Charset.forName(PMDefines.CHARSET);
  CharsetEncoder encoder = charset.newEncoder();
  CharsetDecoder decoder = charset.newDecoder();

  void clientLogDebug(Object msg)
  {
    if (subLogger != null) {
      subLogger.debug(msg);
    }
  }
  
  void clientLogWarn(Object msg)
  {
    if (subLogger != null) {
      subLogger.warn(msg);
    }
  }
  
  void clientLogInfo(Object msg)
  {
    if (subLogger != null) {
      subLogger.info(msg);
    }
  }
  
  void clientLogError(Object msg)
  {
    if (subLogger != null) {
      subLogger.error(msg);
    }
  }
  
  void clientLogError(Object msg, Throwable t)
  {
    if (subLogger != null) {
      subLogger.error(msg, t);
    }
  }
  
  String[] copyArgs(String[] src, int start, int len)
  {
    String[] dst = new String[len];
    for (int i = 0; i < len; i++) {
      dst[i] = src[i + start];
    }
    return dst;
  }
  
  /**
   * Send buffer contents to connected channel.
   * @param outBuf - Buffer to send.
   * @throws IOException
   */
  protected void sendBuf(ByteBuffer outBuf) throws IOException
  {
    while (outBuf.hasRemaining()) {
      socketChannel.write(outBuf);
    }
  }

  /**
   * @param args
   * @throws Exception
   */
  void handleExec(String[] args) throws Exception
  {
    ByteBuffer outBuf;
    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    String outString;
    int argId;
    String envFile = null;
    Integer timeout = -1;
    boolean mix = false;
    LongOpt[] longopts = new LongOpt[3];
    longopts[0] = new LongOpt("timeout", LongOpt.REQUIRED_ARGUMENT, null, 't');
    longopts[1] = new LongOpt("envfile", LongOpt.REQUIRED_ARGUMENT, null, 'e');
    longopts[2] = new LongOpt("mix", LongOpt.NO_ARGUMENT, null, 'M');   // Mix stdout and stderr

    Getopt g = new Getopt("EXEC", args, "+:e:t:M", longopts);
    g.setOpterr(false); // We'll do our own error handling

    while ((argId = g.getopt()) != -1) {
      switch (argId) {
      case 'e':
        // Host name
        envFile = g.getOptarg();
        break;
      case 't':
        // Session ID
        timeout = Integer.decode(g.getOptarg());
        break;
      case 'M':
        mix = true;
        break;
      default:
        subLogger.warn("Unexpected getopt error: <" + argId + ">");
        // shouldn't happen
        break;
      } // switch (argId)
    } // while (argId)
    ByteBuffer envBuf = null;
    if (envFile != null) {
      envBuf = PMPacket.readEnvFile(envFile);

    }
    outString = ";"; // Start packet
    outString += PMPacket.textField("EXEC");
    if (envFile != null) {
      outString += PMPacket.textField("-e");
      outString += PMPacket.binaryField(envBuf);
    }
    if (timeout != -1) {
      outString += PMPacket.textField("-t " + timeout);
    }
    if (mix) {
      outString += PMPacket.textField("-m");
    }
    for (int i = g.getOptind(); i < args.length; i++) {
      outString += PMPacket.textField(args[i]);
    }
    outString += ';'; // End packet
    outBuf = ByteBuffer.wrap(outString.getBytes());
    clientLogDebug("Sending \"" + outString + "\"");
    while (outBuf.hasRemaining()) {
      socketChannel.write(outBuf);
    }
    PMPacket.initBuf(inBuf);
    PMPacket packet = new PMPacket(subLogger);
    if (getPacket(inBuf, packet)) {
      // Check for error/success status
      String nextArg = PMPacket.convertStr(packet.args.remove(0));
      if (nextArg.compareToIgnoreCase("EXEC") != 0) {
        System.err.println("Malformed reply from server: \"" + nextArg + "\"");
        System.out.println("-1");
        return;
      }

      nextArg = PMPacket.convertStr(packet.args.remove(0));

      if (nextArg.compareToIgnoreCase("SUCCESS") == 0) {
        if (packet.args.size() < 2) {
          System.err.println("Malformed reply from server - missing fields");
        }
        String handle = "";
        String pid;
        handle = PMPacket.convertStr(packet.args.remove(0));
        pid = PMPacket.convertStr(packet.args.remove(0));
        if (verbose) {
          handle += " PID=" + pid;
        }
        System.out.println(handle);
        return;
      } else if (nextArg.compareToIgnoreCase("ERROR") == 0) {
        if (packet.args.size() < 1) {
          System.err.println("Malformed reply from server - missing fields");
        } else {
          nextArg = PMPacket.convertStr(packet.args.remove(0));
          System.out.println("-1");
          System.err.println(nextArg);
        }
      }
    } else {
      // EOF waiting for return status
      System.out.println("-1");
      System.err.println("Error - EOF waiting for reply");
      return;
    }
    return;
  }

  /**
   * 
   * @param cmd
   * @param args
   * @throws Exception
   */
  void doProcessCmd(String cmd, String[] args) throws Exception
  {
    ByteBuffer outBuf;
    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    String outString;

    if (args.length < 1) {
      System.err.println("Error - Missing operand");
      Usage.print(progname);
      return;
    }

    outString = ";";
    outString += PMPacket.textField(cmd);
    outString += PMPacket.textField(args[0]);
    outString += ";";
    outBuf = ByteBuffer.wrap(outString.getBytes());
    while (outBuf.hasRemaining()) {
      socketChannel.write(outBuf);
    }
    PMPacket.initBuf(inBuf);
    PMPacket packet = new PMPacket(subLogger);
    if (getPacket(inBuf, packet)) {
      // Check for error/success status
      String nextArg = PMPacket.convertStr(packet.args.remove(0));
      if (nextArg.compareToIgnoreCase(cmd) != 0) {
        System.err.println("Malformed reply from server: \"" + nextArg + "\"");
        System.out.println("-1");
        return;
      }

      try {
        nextArg = PMPacket.convertStr(packet.args.remove(0));

        if (nextArg.compareToIgnoreCase("SUCCESS") == 0) {
          if (packet.args.size() < 4) {
            System.err.println("Malformed SUCCESS reply from server - missing fields");
          }
          int procId = PMPacket.convertInt(packet.args.remove(0));
          String cmdline = PMPacket.convertStr(packet.args.remove(0));
          String pid  = PMPacket.convertStr(packet.args.remove(0));
          String status = PMPacket.convertStr(packet.args.remove(0));
          String outputLine;

          if (verbose) {
            outputLine = String.format("Handle %d; Command %s; PID %s; Status %s", procId, cmdline,
                pid, status);
          } else {
            outputLine = status;
          }
          System.out.println(outputLine);
          return;
        } else if (nextArg.compareToIgnoreCase("ERROR") == 0) {
          if (packet.args.size() < 1) {
            System.err.println("Malformed ERROR reply from server - missing fields");
          } else {
            System.out.println("-1");
            System.err.println("ERROR " + PMPacket.convertStr(packet.args.remove(0)));
          }
        }
      } catch (Exception e) {
        clientLogInfo("Malformed reply");
        System.out.println("-1");
        System.err.println("Malformed reply from server!");
      }
    } else {
      // EOF waiting for return status
      clientLogInfo("Unexpected disconnect from server");
      System.out.println("-1");
      System.err.println("Error - EOF waiting for reply");
    }
  }

  /**
   * Perform the status subcommand
   * @param args
   * @throws Exception
   */
  void handleStatus(String[] args) throws Exception
  {
    doProcessCmd("STATUS", args);
  }

  /**
   * Perform the PID subcommand
   * @param args
   * @throws Exception
   */
  void handlePid(String[] args) throws Exception
  {
    doProcessCmd("PID", args);
  }

  void handleRemove(String[] args) throws Exception
  {
    doProcessCmd("REMOVE", args);
  }

  void handleSuspend(String[] args) throws Exception
  {
    doProcessCmd("SUSPEND", args);
  }

  void handleResume(String[] args) throws Exception
  {
    doProcessCmd("RESUME", args);
  }

  void handleKill(String[] args) throws Exception
  {
    doProcessCmd("KILL", args);
  }

 /**
  * Perform the DUMP subcommand
  * @param args
  * @throws Exception
  */
  void handleDump(String [] args) throws Exception
  {
    String request = ";";
    request += PMPacket.textField("dump");
    request += ";";
    ByteBuffer reqBuf = PMPacket.convertBuf(request);
    while (reqBuf.hasRemaining()) {
      socketChannel.write(reqBuf);
    }
    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    PMPacket pkt = new PMPacket(subLogger);
    PMPacket.initBuf(inBuf);
    while (getPacket(inBuf, pkt)) {
      for (int i = 0; i < pkt.args.size(); i++) {
        String field = PMPacket.convertStr(pkt.args.get(i));
        System.out.print(field);
        if (i != (pkt.args.size() - 1)) {
          System.out.print(",");
        } else {
          System.out.println("");
        }
      }
      pkt = new PMPacket(subLogger);
    }
  }

  void doGetOutputCommand(String cmd, String[] args) throws Exception
  {
 
     String handle = null;
    boolean increment = false;

    for (int i = 0; i < args.length; i++) {
      if (!args[i].startsWith("-i")) {
        handle = args[i];
      } else {
        increment = true;
      }
    }
    if (handle == null) {
      System.err.println("Error - Missing operand");
      Usage.print(progname);
      return;
    }
    doSendGetOutput(cmd, handle, increment);
    getServerOutputPrint();
  }
    
  protected boolean doSendGetOutput(String cmd, String handle, boolean increment) throws Exception
  {
    String outString;
    ByteBuffer outBuf;

    outString = ";";
    outString += PMPacket.textField(cmd);
    if (increment) {
      outString += PMPacket.textField("-i");
    }
    outString += PMPacket.textField(handle);
    outString += ";";
    outBuf = ByteBuffer.wrap(outString.getBytes());
    sendBuf(outBuf);
    return true;
  }
  
  protected static ByteBuffer getServerOutputBuf(SocketChannel chan) throws Exception
  {
    ByteBuffer returnBuf = ByteBuffer.allocate(PMDefines.BIG_BUFSIZE);
    ByteBuffer inBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
    int nread = 0;
    while (nread >= 0) {
      inBuf.clear();
      nread = chan.read(inBuf);
      if (nread > 0) {
        inBuf.flip();
        returnBuf = PMPacket.putByteBuffer(inBuf, returnBuf);
      }
    }
    returnBuf.flip();
    return returnBuf;
  }
  
  void getServerOutputPrint() throws Exception
  {
    ByteBuffer inBuf = ByteBuffer.allocate(PMDefines.BIG_BUFSIZE);
    int totalWritten = 0;
    int nread = 0;
    while (nread >= 0) {
      inBuf.clear();
      nread = socketChannel.read(inBuf);
      if (nread > 0) {
        inBuf.flip();
        while (inBuf.hasRemaining()) {
          int lastpos, firstpos;
          firstpos = inBuf.position();
          if ((inBuf.limit() - firstpos) < PMDefines.BUFSIZE) {
            lastpos = inBuf.limit();
          } else {
            lastpos = firstpos + PMDefines.BUFSIZE;
          }
          totalWritten += lastpos - firstpos;
          System.out.write(inBuf.array(), firstpos, lastpos - firstpos);
          inBuf.position(lastpos);
        }
        //       System.out.flush();
      }
      clientLogDebug(String.format("read %d bytes", nread));
    }
    clientLogDebug(String.format("wrote %d bytes", totalWritten));
  }

  void handleStderr(String[] args) throws Exception
  {
    try {
      doGetOutputCommand("stderr", args);
    } catch (Exception e) {
      System.err.printf("STDERR command failed");
      clientLogError(e, e);
    }
  }

  void handleStdout(String[] args)
  {
    try {
      doGetOutputCommand("stdout", args);
    } catch (Exception e) {
      System.err.printf("STDOUT command failed");
      subLogger.error(e, e);
    }
  }

  void handleStdin(String[] args) throws Exception
  {
    System.err.printf("Stdin not implemented\n");
  }

  void handleGet(String[] args) throws Exception
  {
    try {
      handleTFTPArgs(args, hostname, true);
    } catch (Exception e) {
      System.err.printf("%s\n", e.getMessage());
      subLogger.error(e.getMessage());
      throw e;
    }
  }

  void handlePut(String[] args) throws Exception
  {
    try {
      handleTFTPArgs(args, hostname, false);
    } catch (Exception e) {
      System.err.printf("%s\n", e.getMessage());
      subLogger.error(e.getMessage());
      throw e;
    }
  }

  protected static void handleTFTP(String hostname, boolean receiveFile, 
      String localFilename, String remoteFilename, int transferMode, boolean force,
      int serverPort) throws Exception
   {
    TFTPClient tftp = null;
    
    // Create our TFTP instance to handle the file transfer.
    tftp = new TFTPClient();

    // We want to timeout if a response takes longer than 60 seconds
    tftp.setDefaultTimeout(60000);

    // Open local socket
    try {
      tftp.open();
    } catch (SocketException e) {
      throw new IOException("TFTP open: " + e.getMessage());
    }

    // We haven't closed the local file yet.
    
    // If we're receiving a file, receive, otherwise send.
    if (receiveFile) {
      FileOutputStream output = null;
      File file;

      file = new File(localFilename);
      if (file.isDirectory()) {
        tftp.close();
        throw new IOException("Filename: " + localFilename + "is existing directory");
      }

      // If file exists, don't overwrite it.
      if (file.exists() && !force) {
        tftp.close();
        throw new IOException("Error: " + localFilename + " already exists.");
      }

      // Try to open local file for writing
      try {
        output = new FileOutputStream(file);
      } catch (IOException e) {
        tftp.close();
        throw new IOException(localFilename + ": " + e.getMessage());
      }

      // Try to receive remote file via TFTP
      try {
        tftp.receiveFile(remoteFilename, transferMode, output, hostname, serverPort);
      } catch (IOException e) {
        throw new IOException("receiveFile: " + e.getMessage());
      } finally {
        // Close local socket and output file
        tftp.close();
        try {
          if (output != null) {
            output.close();
          }
        } catch (IOException e) {
          throw new IOException ("close file: " + e.getMessage());
        }
      } // try-catch-finally
    } else {
      // We're sending a file
      FileInputStream input = null;

      // Try to open local file for reading
      try {
        input = new FileInputStream(localFilename);
      } catch (IOException e) {
        tftp.close();
        throw new IOException("open: " + e.getMessage());
      }

      // Try to send local file via TFTP
      try {
        tftp.sendFile(remoteFilename, transferMode, input, hostname, serverPort);
      } catch (UnknownHostException e) {
        throw new UnknownHostException(hostname + ": " + e.getMessage());
      } catch (IOException e) {
        throw new IOException("sendFile: " + e.getMessage());
      } finally {
        // Close local socket and input file
        tftp.close();
        try {
          if (input != null) {
            input.close();
          }
        } catch (IOException e) {
          throw new IOException ("close file: " + e.getMessage());
        }
      } // try-catch-finally
    }   // send File
  } // handleTFTP
  
  void handleTFTPArgs(String[] args, String hostname, boolean receiveFile)
  {
    int transferMode = TFTP.BINARY_MODE, argc;
    String arg, localFilename, remoteFilename;
 
    boolean force = false;

    // Parse options
    for (argc = 0; argc < args.length; argc++) {
      arg = args[argc];
      if (arg.startsWith("-")) {
        if (arg.equals("-a")) {
          transferMode = TFTP.ASCII_MODE;
        } else if (arg.equals("-b")) {
          transferMode = TFTP.BINARY_MODE;
        } else if (arg.equals("-f")) {
          force = true;
        } else {
          System.err.println("Error: unrecognized option.");
          Usage.print(progname);
          System.exit(1);
        }
      } else
        break;
    }

    // Make sure there are enough arguments
    if (args.length - argc < 2) {
      System.err.println("Error: invalid number of arguments.");
      Usage.print(progname);
      System.exit(1);
    }

    // Get host and file arguments
    if (receiveFile) {
      remoteFilename = args[argc];
      localFilename = args[argc+1];
    } else {
      localFilename = args[argc];
      remoteFilename = args[argc + 1];
    }
    int status = 0;
    try {
      handleTFTP( hostname,  receiveFile,  localFilename,  remoteFilename,  transferMode,  force, 
          Integer.parseInt(destPort) + 1);
    } catch (Exception e) {
      System.err.println("TFTP error: " + e.getMessage());
      status = -1;
    } 
    System.exit(status);
  }

  void handleDir(String[] args) throws Exception
  {
    System.err.printf("Dir not implemented\n");
  }

  /*
   * Read until EOF or packet is ready 
   * Return false if EOF is reached without completing a packet. 
   * Return true if a packet has been parsed.
   */
  protected boolean getPacket(ByteBuffer inBuf, PMPacket pkt) throws Exception
  {
    SocketChannel chan = socketChannel;
    // myDebug("Enter");
    while (true) {
      int nRead;
      if (!inBuf.hasRemaining()) {
        inBuf.clear();
        try {
          nRead = chan.read(inBuf);
        } catch (java.nio.channels.ClosedByInterruptException cbi) {
          nRead = -1;
          clientLogWarn(String.format("Interrupted reading server socket: %s", cbi.getMessage()));
        } catch (IOException e) {
          nRead = -1;
          clientLogWarn(String.format("IOException reading server socket: %s", e.getMessage()));
          System.err.println(String.format("Caught IOException reading server socket: %s",
              e.getMessage()));
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

  protected void connectServer() throws Exception
  {
    socketChannel = SocketChannel.open(new InetSocketAddress(hostname, Integer
        .parseInt(destPort)));
    // confirm it is a PM Server on the other end
    String writeString = ";";
    writeString += PMPacket.textField(PMDefines.PMCLIENT_ID);
    writeString += PMPacket.textField(String.format("%06x", PMDefines.PM_VERSION));
    writeString += ";";
    ByteBuffer outBuf = ByteBuffer.wrap(writeString.getBytes());
    outBuf.rewind();
    while (outBuf.hasRemaining()) {
      socketChannel.write(outBuf);
    }
    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    PMPacket pkt = new PMPacket(subLogger);
    PMPacket.initBuf(inBuf);
    if (getPacket(inBuf, pkt)) {
      String idStr = PMPacket.convertStr(pkt.args.remove(0));
      // myDebug("ID = " + idStr);
      if (idStr.compareToIgnoreCase(PMDefines.PMSERVER_ID) == 0) {
        // Identified remote sever - begin processing
        // System.out.println("SUCCESSFULLY identified server\n");
      } else {
        // End of file - no token available OR ID does not match
        Exception e = new Exception("Target server is not a ProcessManager");
        throw e;
      }
    }
  } // connectServer

  void doSubCommand(String subcmd, String[] args, Getopt g) throws Exception
  {
    // myDebug("Enter");
    if (!validSubcommand(subcmd)) {
      clientLogError("Invalid subcmd " + subcmd);
      String errout = "Invalid subcmd " + subcmd;

      System.err.println(errout);
      return;
    }
    try {
      connectServer();
    } catch (ConnectException ce) {
      clientLogError(ce, ce);
      System.err.printf("Connection to %s:%s - <%s>\n", hostname, destPort, ce.getMessage());
      throw ce;
    } catch (Exception e) {
      clientLogError(e, e);
      throw e;
    }
    int len = args.length - g.getOptind() - 1;
    String[] cmdArgs = new String[len];
    System.arraycopy(args, g.getOptind() + 1, cmdArgs, 0, len);
    // String[] cmdArgs = copyArgs(args, g.getOptind(), args.length);
    if (subcmd.compareToIgnoreCase("exec") == 0) {
      handleExec(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("status") == 0) {
      handleStatus(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("pid") == 0) {
      handlePid(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("suspend") == 0) {
      handleSuspend(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("resume") == 0) {
      handleResume(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("kill") == 0) {
      handleKill(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("stdout") == 0) {
      handleStdout(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("stderr") == 0) {
      handleStderr(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("get") == 0) {
      handleGet(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("put") == 0) {
      handlePut(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("dir") == 0) {
      handleDir(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("remove") == 0) {
      handleRemove(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("dump") == 0) {
      handleDump(cmdArgs);
    } else if (subcmd.compareToIgnoreCase("wait") == 0) {
      handleWait(cmdArgs);
    } else {
      System.err.println("Unknown sub command " + subcmd);
      Usage.print(progname);
    }
  } // doSubCommand

  void handleWait(String[] cmdArgs) throws Exception
  {
    int waitSeconds = 0;
    Timer timer;
    if (cmdArgs.length > 1) {
      if (cmdArgs[0].compareToIgnoreCase("-t") == 0) {
        try {
          waitSeconds = Integer.parseInt(cmdArgs[1]);
        } catch (NumberFormatException e) {
          System.err.printf("ERROR: Invalid wait time %s", cmdArgs[1]);
          System.exit(-1);
        }
        String[] newArray = new String[cmdArgs.length - 2];
        System.arraycopy(cmdArgs, 2, newArray, 0, cmdArgs.length - 2);
        cmdArgs = newArray;
        timer = new Timer(true);
        PMTimer timerTask = new PMTimer();
        timer.schedule(timerTask, waitSeconds * 1000);
      }
    }
    doProcessCmd("WAIT", cmdArgs);
  }

  boolean validSubcommand(String in)
  {
    // myDebug("Enter");
    for (int i = 0; i < PMDefines.CMDLIST.length; i++) {
      if (in.compareToIgnoreCase(PMDefines.CMDLIST[i]) == 0) {
        return true;
      }
    }
    return false;
  } // validSubcommand

  protected void socketCleanup()
  {
    if (socketChannel != null) {
      try {
      socketChannel.close();
      } catch (IOException ie) {
        clientLogError("Error closing socketChannel", ie);
      }
      socketChannel = null;
    }
  }

}


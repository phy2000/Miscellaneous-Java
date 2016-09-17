package pmapi;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.tftp.TFTP;
import org.apache.log4j.Logger;
import pmclient.PMClientSub;
import pmutils.PMDefines;
import pmutils.PMPacket;

/**
 * @author      Paul Young <pyoung@informatica.com>
 * @version     3.0                   
 * @since       2012-05-11
 */

/**
 * 
 */
class PMWaitTimer extends TimerTask
{
  PMProcess process;
  /**
   * Create a timer task to handle wait command
   * @param pProc - Process object doing wait
   */
  PMWaitTimer(PMProcess pProc)
  {
    process = pProc;
  }
  public void run()
  {
    synchronized(process._handle){ 
      if (process.waitThread != null) {
        process.waitTimedOut = true;
        process.waitThread.interrupt();
      } // if
    }   // synchronized
  } // run()
}   // PMWaitTimer

public class PMProcess extends PMClientSub
{
  String _handle;
  private int _pid;
  private String _state;
  private String _serverMessage;
  private ArrayList<String> _cmdArray;
  private boolean _mix;
  private String _env[];
  private int _timeoutSeconds;
  private String _serverCmdLine;   // Returned from the server
  private ByteBuffer _output;
  private ByteBuffer _errout;
  Thread waitThread;
  boolean waitTimedOut;
  
  /**
   * Create a process object with the default settings.
   */  
  public PMProcess()
  {
    _pid = -1;
    _state = "";
    _mix = false;
    _timeoutSeconds = PMDefines.DEFAULT_KILL_SECONDS;
    _cmdArray = new ArrayList<String>();
    subLogger = null;
  }
  
  // Access Functions
  /**
   * Set the logger object used to log API messages
   * @param logger - the new value of the logger.
   * @return previous value of the logger, null by default.
   */
  public Logger setLogger(Logger logger)
  {
    Logger previous = subLogger;
    subLogger = logger;
    return previous;
  }
  
  /**
   * Get the current value of the logger
   * @return current value of the logger, null by default.
   */
  public Logger getLogger()
  {
    return subLogger;
  }
  
  /**
   * Get the command line as interpreted by the server
   * @return Command line returned from a previous status command.
   * <br> On successful return of {@link #cmdStatus()} or {@link #cmdPid()}, the values returned by
   * {@link #getState()}, {@link #getPid()}, and {@link #getServerCmdLine()} will be valid.  
   */
  public String getServerCmdLine()
  {
    return _serverCmdLine;
  }
  /**
   * Retrieve the message from the most recent server request (subcommand).
   * @return String value of the text received from the most recent subcommand
   */
  public String getServerMessage()  { return _serverMessage;}
  
  /**
   * Set the server message returned by getServerMessage()
   * @param message
   * @return Previous value of the server message.
   */
  String setServerMessage(String message)
  {
    String previous = _serverMessage;
    _serverMessage = message;
    return previous;
  }
  /**
   * Retrieve the process handle.
   * @return If the process has been successfully {@link #cmdExec() Exec'd } return the process handle.
   * Otherwise, return null.
   */
  public String getHandle() { return _handle; }
 
  /**
   * Retrieve the process PID
   * @return The process ID assigned by the server, or -1 if the exec command
   * has not been successfully executed.
   * <br> On successful return of {@link #cmdStatus()} or {@link #cmdPid()}, the values returned by
   * {@link #getState()}, {@link #getPid()}, and {@link #getServerCmdLine()} will be valid.
   */
  public int getPid()  { return _pid; }
  
  /**
   * Get the state returned from the server.
   * @return Text string describing the process state returned on the most
   * recent subcommand.
   * <br> On successful return of {@link #cmdStatus()} or {@link #cmdPid()}, the values returned by
   * {@link #getState()}, {@link #getPid()}, and {@link #getServerCmdLine()} will be valid.   */
  public String getState() { return _state;}
  
  /**
   * Get the host name of this process.
   * @return A text string of the hostname as set by the {@link #setHost(String) 
   * setHost} call, or <b>null</b> if it has not been set.
   */
  public String getHost() { return hostname;}
  
  /**
   * Get the command line.
   * @return The commandline set by  
   * {@link #addCmdLine(String)} or {@link #setCmdArray(ArrayList)}
   */
  public ArrayList<String> getCmdArray() { return _cmdArray; }
  
  /**
   * Get the mix flag.
   * @return The mix flag as set by the {@link #setMix(boolean) setMix} call.
   * Default is <b>false</b>.
   */
  public boolean getMix() { return _mix; }
  
  /**
   * Get the environment array.
   * @return The environment array set by a previous call to {@link #setEnv(String[])
   * setEnv} or <b>null</b> if it has not been set.
   */
  public String[] getEnv() { return _env; }
  
  /**
   * Get the process timeout value.
   * @return the timeout value in seconds set by a previous call to
   *  {@link #setTimeout(int) setTimeout}
   * or the default of 300 seconds if it has not been set.
   */
  public int getTimeout() { return _timeoutSeconds; }
  
  /**
   * Retrieve the stdout buffer contents. This buffer is replaced each time the stdout command is issued.
   * @return A ByteBuffer with the contents of the most recent stdout request for this process object.
   */
  public ByteBuffer getStdout()
  {
    return _output;
  }
  
  /**
   * Retrieve the stderr buffer contents. This buffer is replaced each time the stderr command is issued.
   * @return A ByteBuffer with the contents of the most recent stderr request for this process object.
   */
  public ByteBuffer getStderr()
  {
    return _errout;
  }
  
  /**
   * Set target host name or address for remote server
   *
   * @param  host Host name or IP address
   * @return Previous value of the Process host.
   */
  public String setHost(String host)
  { 
    String oldHost = hostname;
    hostname = host;
    return oldHost;
  }
 
  /**
   * Set the command line to an array list of Strings. Each individual string is passed
   * to the remote command interpreter.
   *  
   * @param  cmds New command line
   */
  public void setCmdArray(ArrayList<String> cmds)
  { 
    _cmdArray.clear();
    _cmdArray.addAll(cmds);
  }
  
  /**
   * Append a string to the command line.
   * @param cmd
   */
  public void addCmdLine(String cmd)
  {
    _cmdArray.add(cmd);
  }
  /**
   * Set the environment array for the remote process
   *  
   * @param  newEnv String array to replace the current environment variables
   * @return oldEnv the previous environment variables
   */
  public String[] setEnv(String newEnv[])
  {
    String oldEnv[] = _env;
    _env = newEnv;
    return oldEnv;
  }
  
  /**
   * Set the mix flag for the process to mix stderr and stdout together
   *  
   * @param  mix set the value of the mix flag to mix stdout and stderr
   * @return the previous value of mix (defaults to false)
   */
  public boolean setMix(boolean mix)
  {
    boolean old = _mix;
    _mix = mix;
    return old;
  }
  
  /**
   * Set the timeout of the process.
   * <p>
   * The process will automatically be killed if it is still running after 
   * (timeout) seconds. 
   *  
   * @param  timeout - new timeout value in seconds.
   * @return the previous value of the timeout (defaults to 300 seconds)
   */

  public int setTimeout(int timeout)
  {
    int old = _timeoutSeconds;
    _timeoutSeconds = timeout;
    return old;
  }
 
  // Sub commands
  
  /**
   * Start a process execution on the remote host.
   * <p>
   *  
   * @return <b>true</b> if the command is successful, <b>false</b> otherwise
   * <br> On success, the process handle and pid are set.
   * <br> On failure, an error message can be retrieved via {@link #getServerMessage()}.
   * 
   * @throws Exception from doSubCommand
   */
  public boolean cmdExec() throws Exception
  {
    ByteBuffer envBuf = null;
    if (_env != null) {
      
      // TODO - convert string array to env ByteBuffer
      envBuf =  ByteBuffer.allocate(PMDefines.BUFSIZE);
      
      PMPacket.initBuf(envBuf);
      for (int i = 0; i < _env.length; i++) {
        ByteBuffer tmpBuf = PMPacket.convertBuf(_env[i]);
        envBuf = PMPacket.putByteArray(tmpBuf.array(), envBuf, _env[i].length());
        envBuf = PMPacket.putByte((byte)'\n', envBuf);
      }
      envBuf.flip();
      envBuf = PMPacket.cleanEnvBuf(envBuf);
    }
    String outString = ";"; // Start packet
    outString += PMPacket.textField("EXEC");
    if (envBuf != null) {
      outString += PMPacket.textField("-e");
      outString += PMPacket.binaryField(envBuf);
    }
    if (_timeoutSeconds != -1) {
      outString += PMPacket.textField("-t " + _timeoutSeconds);
    }
    if (_mix) {
      outString += PMPacket.textField("-m");
    }
    for (int i = 0; i < _cmdArray.size(); i++) {
      outString += PMPacket.textField(_cmdArray.get(i));
    }
    outString += ';'; // End packet
    ByteBuffer outBuf = ByteBuffer.wrap(outString.getBytes());

    try {
      connectServer();
    } catch (ConnectException ce) {
      socketCleanup();
      throw ce;
    } catch (Exception e) {
      socketCleanup();
      throw e;
    }
   
    sendBuf(outBuf);
    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    PMPacket.initBuf(inBuf);
    PMPacket packet = new PMPacket(subLogger);
    if (getPacket(inBuf, packet)) {
      // Check for error/success status
      String nextArg = PMPacket.convertStr(packet.args.remove(0));
      if (nextArg.compareToIgnoreCase("EXEC") != 0) {
        _serverMessage = "Malformed reply from server: \"" + nextArg + "\"";
        socketCleanup();
        return false;
      }

      nextArg = PMPacket.convertStr(packet.args.remove(0));

      if (nextArg.compareToIgnoreCase("SUCCESS") == 0) {
        if (packet.args.size() < 2) {
          _serverMessage = "Malformed reply from server - missing fields";
          socketCleanup();
          return false;
        }
        _handle = PMPacket.convertStr(packet.args.remove(0));
        _pid = PMPacket.convertInt(packet.args.remove(0));
        _serverMessage = "OK";
        socketCleanup();
        return true;    // Only true return
      } else if (nextArg.compareToIgnoreCase("ERROR") == 0) {
        if (packet.args.size() < 1) {
          _serverMessage = "Malformed reply from server - missing fields";
        } else {
          _serverMessage = PMPacket.convertStr(packet.args.remove(0));
        }
      }
    } else {
      _serverMessage = "EOF waiting for server reply";
    }
    socketCleanup();
    return false;
  }
 
  /**
   * Retrieve the process status from the remote host.
   * <p>
   *  
   * @return <b>true</b> if the command is successful, <b>false</b> otherwise
   * <br> On success, the process state is set and can be retrieved via {@link #getState()}
   * <br> On failure, an error message can be retrieved via {@link #getServerMessage() }
   * <br> On successful return of {@link #cmdStatus()} or {@link #cmdPid()}, the values returned by
   * {@link #getState()}, {@link #getPid()}, and {@link #getServerCmdLine()} will be valid.
   * 
   * @throws Exception from processCommand
   */
  public boolean cmdStatus() throws Exception
  {
    boolean bVal = processCommand("STATUS");
    if (bVal) {
      _serverMessage = _state;
    }
    return bVal;
  }
  
  
  /**
   * Request the process be suspended on the remote host.
   * @return <b>true</b> if the command is successful.
   * <br> The server message will be set, <b>false</b> otherwise 
   * @throws Exception from processCommand
   */
  public boolean cmdSuspend() throws Exception
  {
    return processCommand("SUSPEND");
  }
  /**
   * Request that the process be resumed if it is suspended
   * @return <b>true</b> if the command is successful, <b>false</b> otherwise
   * <br> The server message will be set. 
   * @throws Exception from processCommand
  */
  public boolean cmdResume() throws Exception
  {
    return processCommand("RESUME");
  } 
  /**
   * Request that the process be Killed if it is running on the remote host.
   * @return <b>true</b> if the command is successful, <b>false</b> otherwise
   * <br> The server message will be set.
   * @throws Exception from processCommand
   */
  public boolean cmdKill() throws Exception
  {
    return processCommand("KILL");
  }
  /**
   * Remove the state of a dead process from the server's database.
   * @return <b>true</b> if the command is successful and <b>false</b> otherwise.
   * <br> The server message will be set.
   * @throws Exception from processCommand
   */
  public boolean cmdRemove() throws Exception
  {
    return processCommand("REMOVE");
  }
  /**
   * Retrieve the process ID from the remote server.
   * @return <b>true</b> if the command is successful and <b>false</b> otherwise.
   * <br> On failure, an error message can be retrieved via {@link #getServerMessage() }
   * <br> On successful return, use {@link #getPid()} to retrieve the PID returned as an integer. 
   * <br> On successful return, use {@link #getServerMessage()} to retrieve the PID as a string. 
   * <br> On successful return of {@link #cmdStatus()} or {@link #cmdPid()}, the values returned by
   * {@link #getState()}, {@link #getPid()}, and {@link #getServerCmdLine()} will be valid.
   * @throws Exception from processCommand
   */
  public boolean cmdPid() throws Exception
  {
    boolean bVal = processCommand("PID");
    if (bVal) {
      _serverMessage = Integer.toString(_pid);
    }
    return bVal;
  }
  
  /**
   * Wait for a process to enter the DEAD state.
   * @param timeout - time to wait in seconds.
   * @return  <b>true</b> if the command is successful and <b>false</b> otherwise.
   * <br> The server message is set to "DEAD" if the process died, and "TIMEDOUT" if the
   * timeout expired without the process going dead.
   */
  public boolean cmdWait(int timeout) throws Exception
  {
    boolean cmdValue = true;
    if (_handle == null) {
      _serverMessage = "Invalid handle";
      return false;
    }
    synchronized(_handle) {
      if (waitThread != null) {
        _serverMessage = "Already waiting";
        cmdValue = false;
      } else {
        waitThread = Thread.currentThread();
      }
    }
    if (timeout > 0) {
      Timer timer = new Timer(true);
      PMWaitTimer timerTask = new PMWaitTimer(this);
      timer.schedule(timerTask, timeout * 1000);
    }
    try {
      cmdValue = processCommand("WAIT");
    } catch (java.nio.channels.ClosedByInterruptException ie) {
      // Expected.
    } 
    synchronized (_handle) {
      waitThread = null;
      if (waitTimedOut) {
        _serverMessage = "TIMEDOUT";
        cmdValue = true;
      }
      waitTimedOut = false;
    }
    return cmdValue;
  }

  
  /**
   * Retrieve the stdout for this process.
   * @param incremental - Set to true if this should be an incremental retrieval.
   * @return  <b>true</b> if the command is successful and <b>false</b> otherwise.
   * <br> Use {@link #getStdout()} to retrieve the stdout value.
   * @throws Exception 
   */
  public boolean cmdStdout(boolean incremental) throws Exception
  {
    connectServer();
    doSendGetOutput("STDOUT", _handle, incremental);
    _output = getServerOutputBuf(socketChannel);
    socketCleanup();
    return true;
  }
  
  /**
  * Retrieve the stderr for this process.
   * @param incremental - Set to true if this should be an incremental retrieval.
   * @return  <b>true</b> if the command is successful and <b>false</b> otherwise.
   * <br> Use {@link #getStderr()} to retrieve the stderr value.
   * @throws Exception from processCommand
   */
  public boolean cmdStderr(boolean incremental) throws Exception
  {
    connectServer();
    doSendGetOutput("STDERR", _handle, incremental);
    _errout = getServerOutputBuf(socketChannel);
    socketCleanup();
    return true;
  }
 
  /**
   * Retrieve a process dump from the specified host.
   * @param host - the hostname or IP address of the target server.
   * @param port - Server listening port. If 0, uses default listening port.
   * @return A string Array representing the dump output sent by the server.
   * <br> Dump returns a CSV representing a list of all processes known by the remote host.
   * Each CSV record is an array element , individual fields are separated by
   * commas. The first record returned contains labels for each field.
   * @throws Exception if the target is not a Process Manager Server
   * 
   */
  public static ArrayList<String> cmdDump(String host, int port) throws Exception
  {
    if (port == 0) {
      port = PMDefines.PMSERVER_LISTEN_PORT;
    }
    SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port));
    // confirm it is a PM Server on the other end
    String writeString = ";";
    writeString += PMPacket.textField(PMDefines.PMCLIENT_ID);
    writeString += PMPacket.textField(String.format("%06x", PMDefines.PM_VERSION));
    writeString += ";";
    ByteBuffer outBuf = ByteBuffer.wrap(writeString.getBytes());
    outBuf.rewind();
    while (outBuf.hasRemaining()) {
      channel.write(outBuf);
    }
    ByteBuffer inBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
    PMPacket pkt = new PMPacket(null);
    PMPacket.initBuf(inBuf);
    if (getPacketChannel(inBuf, pkt, channel)) {
      String idStr = PMPacket.convertStr(pkt.args.remove(0));
      if (idStr.compareToIgnoreCase(PMDefines.PMSERVER_ID) == 0) {
        String request = ";";
        request += PMPacket.textField("dump");
        request += ";";
        ByteBuffer reqBuf = PMPacket.convertBuf(request);
        while (reqBuf.hasRemaining()) {
          channel.write(reqBuf);
        }
      } else {
        // End of file - no token available OR ID does not match
        Exception e = new Exception("Target server is not a ProcessManager");
        channel.close();
        throw e;
      }
    }
    pkt = new PMPacket(null);
    PMPacket.initBuf(inBuf);
    ArrayList<String> dumpArray = new ArrayList<String>();
    String dumpStr;
    while (getPacketChannel(inBuf, pkt, channel)) {
      dumpStr = new String("");
      for (int i = 0; i < pkt.args.size(); i++) {
        String field = PMPacket.convertStr(pkt.args.get(i));
        dumpStr += field;
        if (i != (pkt.args.size() - 1)) {
          dumpStr += ",";
        } else {
          dumpArray.add(dumpStr);
        }
      }
      pkt = new PMPacket(null);
    }
    channel.close();
    return  dumpArray;
  }
  
  /**
   * Put a file on a remote system. This is a static function and does not reference any fields in the
   * PMProcess object. This abbreviated interface uses port 10256 and binary transfer mode.
   * @param localFile - Local filename, including directory.
   * @param remoteFile - Absolute path to the remote file.
   * @param hostName - hostname of the server
   * @throws Exception - Message will describe the source and cause of the exception.
   */
  public static void cmdPut(String localFile, String remoteFile, String hostName) throws Exception
  {
    cmdPut(localFile, remoteFile, hostName, PMDefines.PMSERVER_LISTEN_PORT+1);
  }
  
  /**
   * Put a file on a remote system. This is a static function and does not reference any fields in the
   * PMProcess object.
   * @param localFile - Local filename, including directory.
   * @param remoteFile - Absolute path to the remote file.
   * @param hostName - hostname of the server
   * @param serverPort - Listening port of the remote server.
   * @throws Exception - Message will describe the source and cause of the exception.
   */
  public static void cmdPut(String localFile, String remoteFile, String hostName, int serverPort
      ) throws Exception
  {
    int mode;
    boolean useBinary = false;
    if (useBinary) {
      mode = TFTP.BINARY_MODE;
    } else {
      mode = TFTP.ASCII_MODE;
    }
    handleTFTP(hostName, false, localFile, remoteFile, mode, false, serverPort);
  }
  
  /**
   * Get a file from the remote system. This is a static function and does not reference any fields in the
   * PMProcess object. This abbreviated interface uses port 10256, binary transfer mode, and no overwrite.
   * @param remoteFile - Absolute path to the remote file to be retrieved
   * @param localFile - Local filename, including directory.
   * @param hostName - Hostname of the server
   * @throws Exception - Message will describe the source and cause of the exception.
   */
  public static void cmdGet(String remoteFile, String localFile, String hostName) throws Exception
  {
    cmdGet(remoteFile, localFile, hostName, PMDefines.PMSERVER_LISTEN_PORT+1, false);
  }
  
  /**
   * Get a file from the remote system. This is a static function and does not reference any fields in the
   * PMProcess object.
   * @param remoteFile - Absolute path to the remote file to be retrieved
   * @param localFile - Local filename, including directory.
   * @param hostName - Hostname of the server
   * @param serverPort - Listening port of the remote server
   * @param force - TRUE to force overwrite of local file, FALSE to fail if the file exists.
   * @throws Exception - Message will describe the source and cause of the exception.
   */
  public static void cmdGet(String remoteFile, String localFile, String hostName, int serverPort,
      boolean force) throws Exception
  {
    int mode;
    boolean useBinary = false;
    if (useBinary) {
      mode = TFTP.BINARY_MODE;
    } else {
      mode = TFTP.ASCII_MODE;
    }
    handleTFTP(hostName, true, localFile, remoteFile, mode, force, serverPort);
  }

  static boolean getPacketChannel(ByteBuffer inBuf, PMPacket pkt, 
      SocketChannel chan)  throws Exception
  {
    while (true) { 
      int nRead;
      if (!inBuf.hasRemaining()) {
        inBuf.clear();
        try {
          nRead = chan.read(inBuf);
        } catch (IOException e) {
          throw e;
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
  } // getPacketChannel
  /**
   * Performs work for STATUS, SUSPEND, RESUME, REMOVE, KILL commands
   * If successful, the serverMessage is set to the process state.
   * @param cmd
   * @return true if successful, false otherwise
   * @throws Exception
   */
  boolean processCommand(String cmd) throws Exception
  {
    if (_handle == null) {
      _serverMessage = "Invalid Process Handle";
      return false;
    }
    try {
      connectServer();
    } catch (ConnectException ce) {
      _serverMessage = String.format("Connection to %s:%s - <%s>\n", hostname, destPort, ce.getMessage());
      socketCleanup();
      throw ce;
    } catch (Exception e) {
      socketCleanup();
      throw e;
    }

    String outString = ";";
    outString += PMPacket.textField(cmd);
    outString += PMPacket.textField(_handle);
    outString += ";";
    ByteBuffer outBuf = PMPacket.convertBuf(outString);
    sendBuf(outBuf);

    ByteBuffer inBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
    PMPacket.initBuf(inBuf);
    PMPacket packet = new PMPacket(subLogger);
    if (getPacket(inBuf, packet)) {
      // Check for error/success status
      String nextArg = PMPacket.convertStr(packet.args.remove(0));
      if (nextArg.compareToIgnoreCase("ERROR") == 0) {
        _serverMessage = nextArg;
        while (!packet.args.isEmpty()) {
          _serverMessage += " " + PMPacket.convertStr(packet.args.remove(0));
        }
        socketCleanup();
        return false;
      } else if (nextArg.compareToIgnoreCase(cmd) != 0) {
        _serverMessage = "Malformed reply from server: \"" + nextArg + "\"";
        socketCleanup();
        return false;
      }

      nextArg = PMPacket.convertStr(packet.args.remove(0));

      if (nextArg.compareToIgnoreCase("SUCCESS") == 0) {
        if (packet.args.size() < 4) {
          _serverMessage = "Malformed reply from server - missing fields";
          socketCleanup();
          return false;
        }
        _pid = PMPacket.convertInt(packet.args.remove(0));
        _serverCmdLine = PMPacket.convertStr(packet.args.remove(0));
        packet.args.remove(0);  // Redundant PID in text
        _state = PMPacket.convertStr(packet.args.remove(0));
        _serverMessage = _state;
        socketCleanup();
        return true;
      }
    }
    socketCleanup();
    return false;
  }
  /**
   * Utility program to read an environment file into a string array.
   * @param envFileName
   * @return a String array where each string is an environment variable definition of
   * the form: <name>=<value>         
   * <br>
   * This array can be passed directly to the {@link #setEnv(String[])} method.
   */
  public static String[] readEnvFile(String envFileName)
  {
    ByteBuffer envBuf =   PMPacket.readEnvFile(envFileName);
    String[] envStrs = PMPacket.parseEnvBuf(envBuf);
    return envStrs;
  }
  
}

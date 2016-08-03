package pmclient;

//import java.lang.*;
import java.util.Calendar;
import java.text.DateFormat; //import java.io.*;
import java.io.File;
import java.net.ConnectException;

import org.apache.log4j.*;
import gnu.getopt.*;
import java.util.TimerTask;

class PMTimer extends TimerTask
{
  public void run()
  {
    System.out.println("TIMEDOUT");
    System.exit(1);
  }
}

class Usage
{
  final static String message = "Usage: %s [-h|--host=<host> [ -p|--port=<port> ] <subcmd> <subcmd parameters>\n"
      + "\t<host> is a hostname or IP address\n"
      + "\t<sessionname> is an optional string to identify a session\n"
      + "\t<port> is an alternate port where the server is listening\n"
      + "\n"
      + "Where <subcmd> is one of:\n"
      + "  exec [-t|--timeout=<timeout>] [-e|--envfile=<envfile>] [-M|--mix] \"command + args\"\n"
      + "      Execute the given command line and print a handle to stdout.\n"
      + "      <envfile> - optional file with environment variable definitions\n"
      + "      <timeout> - seconds before process is killed - defaults to 300 seconds - 0 means no timeout\n"
      + "      --mix - Mix the stdout and stderr into stdout only\n"
      + "  status <handle>\n"
      + "      Print the status of the process associated with <handle>\n"
      + "  suspend <handle>\n"
      + "      Suspend the process on the remote machine\n"
      + "  resume <handle>\n"
      + "      Resume the previously suspended process on the remote machine\n"
      + "  kill <handle>\n"
      + "      Kill the process on the remote machine - handle info is maintained\n"
      + "  remove <handle>\n"
      + "      Remove the information maintained for <handle> and invalidate the handle\n"
      + "  stdout [-i] <handle>\n"
      + "      Retrieve the stdout contents of the process\n"
      + "      -i - get the contents incrementally\n"
      + "  stderr [-i] <handle>\n"
      + "      Retrieve the stderr contents of the process\n"
      + "      -i - get the contents incrementally\n"
      + "  pid <handle>\n"
      + "      Retrieve the process ID of the process\n"
      + "  dump \n"
      + "      Retrieve csv list of known processes and states\n"
//      + "  stdin <handle>\n"
//      + "      Read from stdin and send to the process identified by <handle>\n"
      + "  get [-{a|b}] <remote-path> <local-path>\n"
      + "      Retrieve a file from the remote system\n"
      + "      -a: ascii mode; -b: binary mode (default)\n"
      + "      -f: FORCE overwrite of existing file\n"
      + "  put [-{a|b}] <local-path> <remote-path>\n"
      + "      Send a file to the remote system\n"
      + "      -a: ascii mode; -b: binary mode (default)\n"  
      + "  wait [-t <seconds> ] <handle> \n"
      + "      Wait for a process to die, or for the specified time in seconds\n"
      + "      If the process had died, prints DEAD \n"
      + "      If the timeout expires, prints TIMEDOUT\n"
      + "\n"
//    + "Example commands:\n"
      //      + "$ pmclient -h saturn exec \"ls -l /\"\n"
      //+ "12345678\n"
      //+ "$ pmclient -h saturn stdout 12345678\n"
      //+ "total 220\n"
      //+ "drwxr-xr-x   5 root root  4096 Feb 24  2011 29W\n"
      //+ "drwxr-xr-x   2 root root  4096 Dec  9 04:02 bin\n"
      + "";

  static void print(String progname)
  {
    System.err.printf(String.format(Usage.message, progname, progname, progname));
    System.exit(1);
  }
}

public class PMClientMain
{

  static PMClientSub subClient;
  static Logger logger = Logger.getLogger("ProcMgr");
  static String _progname;
  static String log4j_config = "client.log4j.properties";

  /*
  static String _hostname = null;
  static String _sessionId = PMDefines.PMCLIENT_SESSION_NAME;
  static String _destPort = String.format("%d", PMDefines.PMSERVER_LISTEN_PORT);

  static Charset _charset = Charset.forName(PMDefines.CHARSET);
  static CharsetEncoder _encoder = _charset.newEncoder();
  static CharsetDecoder _decoder = _charset.newDecoder();
  static SocketChannel _socketChannel;
  static boolean _verbose = false;
  static boolean _verify = false;
*/

  public static void main(String args[])
  {
    PMClientSub mySub = new PMClientSub();
    _progname = Thread.currentThread().getStackTrace()[1].getClassName();
    mySub.progname = _progname;
    
    try {
      int argId;
      LongOpt[] longopts = new LongOpt[5];
      longopts[0] = new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'h');
      longopts[1] = new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'p');
      longopts[2] = new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v');
      longopts[3] = new LongOpt("log4j", LongOpt.REQUIRED_ARGUMENT, null, 'L');
      longopts[4] = new LongOpt("session", LongOpt.REQUIRED_ARGUMENT, null, 's');

      Getopt g = new Getopt(_progname, args, "+:h:s:p:L:vV", longopts);
      g.setOpterr(false); // We'll do our own error handling

      while ((argId = g.getopt()) != -1) {
        switch (argId) {
        case 'h':
          // Host name
          mySub.hostname = g.getOptarg();
          break;
        case 's':
          // Session ID
          mySub.sessionId = g.getOptarg();
          break;
        case 'p':
          // Port number
          mySub.destPort = g.getOptarg();
          break;
        case 'v':
          mySub.verbose = true;
          break;
        case 'L':
          log4j_config = g.getOptarg();
          break;
        case ':':
          // Missing option arg
          Usage.print(_progname);
          throw new Exception("Missing argument");
          // break;
        case '?':
          // Unknown option
          Usage.print(_progname);
          throw new Exception("Unknown option");
          // break;
        default:
          System.err.println("Unexpected getopt error: <" + argId + ">");
          System.exit(1);
          break;
        } // switch (argId)
      } // while (argId)
      File logconf = new File(log4j_config);
      if (logconf.canRead()) {
        PropertyConfigurator.configure(log4j_config);
      } else {
        logger.setLevel(Level.WARN);
        BasicConfigurator.configure();
      }
      Calendar cal = Calendar.getInstance();
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);
      logger.info("Start application at " + df.format(cal.getTime()));
      mySub.subLogger = logger;
      if (mySub.hostname == null) {
        Usage.print(_progname);
        throw new Exception("Hostname is required");
      }
      mySub.doSubCommand(args[g.getOptind()], args, g);
    } catch (ConnectException ce) {
      System.exit(-1);
    }
    catch (Exception e) {
      logger.error(e, e);
      System.err.printf("Exception - check log file for stack trace: %s\n", e.getMessage());
      System.exit(-1);
    } // try-catch
    System.exit(0);
  } // main

  /*****************************************************************/
} // class PMClient

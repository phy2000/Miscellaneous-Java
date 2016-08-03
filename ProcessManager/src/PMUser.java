//package pmuser;
import java.io.*;
import java.util.ArrayList;

import gnu.getopt.*;
import pmapi.*;
import pmutils.PMPacket;

public class PMUser
{
  static UserThread pThread;


  /**
   * @param args
   */
  public static void main(String[] args)
  {
    pThread = new UserThread(args);
    pThread.start();
    while (true) {
      try {
        pThread.join();
        break;
      } catch (InterruptedException ie) {
        continue;
      }
    }
    System.out.println("UserThread has exited");
  } // main()
}   // class PMUser

class UserThread extends Thread 
{
  final String Usage = " " + "<args>";

  PMProcess pProc;
  String myArgs[];
  String progname;

  UserThread(String[] args) 
  {
    pProc = null;
    myArgs = args;
    progname = Thread.currentThread().getStackTrace()[1].getClassName();
  }

  void printUsage(String progname)
  {
    System.err.println(progname + Usage);
  }

  public void run() 
  {
    int argId;
    LongOpt[] longopts = new LongOpt[3];
    longopts[0] = new LongOpt("arg1", LongOpt.REQUIRED_ARGUMENT, null, '1');
    longopts[1] = new LongOpt("arg2", LongOpt.OPTIONAL_ARGUMENT, null, '2');
    longopts[2] = new LongOpt("arg3", LongOpt.NO_ARGUMENT, null, '3');

    Getopt g = new Getopt(progname, myArgs, "+:1:2::3", longopts);
    //g.setOpterr(false); // We'll do our own error handling

    while ((argId = g.getopt()) != -1) {
      switch (argId) {
      case '1':
        String reqArg = g.getOptarg();
        System.out.println("Required arg is " + reqArg);
        break;
        
      case '2':
        String optArg = g.getOptarg();
        if (optArg != null) {
          System.out.println("Optional argument is " + optArg);
        }
        break;
      case '3':
        // Do nothing
        break;
      default:
        printUsage(myArgs[0]);
        return;
//        break;
      }   // switch
    } // while
    
    try {
      runtest();
    } catch (Exception e) {
      System.err.printf("runtest: %s\n", e.getMessage());
    }

  } // void run()

  /**
   * 
   */
  void runtest() throws Exception
  {

    String host="saturn.29west.com";
    ArrayList<String> dumpArray = PMProcess.cmdDump(host, 0);
    System.out.println("Dump output:");
    for (int i = 0; i < dumpArray.size(); i++) {
      System.out.printf("%s\n", dumpArray.get(i));
    }
    System.out.println("End Dump Output");
    dumpArray = null;   // So GC dumps the array
    testFileGetPut(host);
    
    // Perform tests
    String env[] = PMProcess.readEnvFile("/home/pyoung/work/test/env.dat");
    
    pProc = new PMProcess();
    pProc.setEnv(env);
    pProc.setHost(host);
    pProc.addCmdLine("bash");
    pProc.addCmdLine("-c");
    pProc.addCmdLine("pwd && echo $CVSROOT && cd /home/pyoung/work/test && ./myapi-test.sh");
    pProc.setTimeout(60);
    boolean bValue;
    try {
       bValue = pProc.cmdExec();
    } catch (Exception e) {
      System.err.printf("cmdExec: %s", e.getMessage());
      bValue = false;
    }
    if (!bValue) {
      return;
    }
    System.out.println("Handle = " + pProc.getHandle());
    String msg = pProc.getServerMessage();
    System.out.printf("exec returned %s\n", msg);
    sleepCycles(1);
    bValue = pProc.cmdStatus();
    if (bValue) {
      System.out.println(pProc.getState());
    } else {
      System.out.println(pProc.getServerMessage());
    }
    bValue = pProc.cmdStdout(false);
    System.out.println("cmdStdout returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStdout()));
    bValue = pProc.cmdStderr(false);
    System.out.println("cmdStderr returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStderr()));
    sleepCycles(2);
    bValue = pProc.cmdStdout(true);
    System.out.println("cmdStdout returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStdout()));
    bValue = pProc.cmdStderr(true);
    System.out.println("cmdStderr returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStderr()));
    // Suspend
    bValue = pProc.cmdSuspend();
    System.out.println("cmdSuspend returned " + bValue);
    sleepCycles(2);
    bValue = pProc.cmdStatus();
    System.out.println("cmdStatus returned " + bValue);
    if (bValue) {
      System.out.println(pProc.getState());
    } else {
      System.out.println(pProc.getServerMessage());
    }
    bValue = pProc.cmdStdout(true);
    System.out.println("cmdStdout returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStdout()));
    bValue = pProc.cmdStderr(true);
    System.out.println("cmdStderr returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStderr()));
    sleepCycles(2);
    // Resume
    bValue = pProc.cmdResume();
    System.out.println("cmdResume returned " + bValue);
    sleepCycles(2);
    bValue = pProc.cmdStatus();
    System.out.println("cmdStatus returned " + bValue);
    if (bValue) {
      System.out.println(pProc.getState());
    } else {
      System.out.println(pProc.getServerMessage());
    }
    bValue = pProc.cmdStdout(true);
    System.out.println("cmdStdout returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStdout()));
    bValue = pProc.cmdStderr(true);
    System.out.println("cmdStderr returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStderr()));
    sleepCycles(2);
    // Wait with timeout
    bValue = pProc.cmdWait(10);
    System.out.println("cmdWait(10) has completed - returned " + bValue);
    System.out.println(pProc.getServerMessage());
    sleepCycles(2);
    // Wait forever
    System.out.println("Calling cmdWait - kill " + pProc.getHandle() + " to continue OR wait for the EXEC timeout");
    bValue = pProc.cmdWait(0);
    System.out.println("cmdWait has completed - returned " + bValue);
    System.out.println(pProc.getServerMessage());
    sleepCycles(1);
    System.out.println("Calling cmdWait again - should return immediately");
    bValue = pProc.cmdWait(0);
    System.out.println("cmdWait has completed - returned " + bValue);
    System.out.println(pProc.getServerMessage());
    sleepCycles(1);
    // Kill
    bValue = pProc.cmdKill();
    System.out.println("cmdKill returned " + bValue);
    sleepCycles(2);
    bValue = pProc.cmdStatus();
    System.out.println("cmdStatus returned " + bValue);
    if (bValue) {
      System.out.println(pProc.getState());
    } else {
      System.out.println(pProc.getServerMessage());
    }
    bValue = pProc.cmdStdout(true);
    System.out.println(PMPacket.convertStr(pProc.getStdout()));
    bValue = pProc.cmdStderr(true);
    System.out.println("cmdStderr returned " + bValue);
    System.out.println(PMPacket.convertStr(pProc.getStderr()));
    // Remove
    System.out.printf("Calling remove on %s\n", pProc.getHandle());
    bValue = pProc.cmdRemove();
    System.out.println("cmdRemove returned " + bValue);
    System.out.println(pProc.getServerMessage());
    sleepCycles(2);
    System.out.printf("Getting Status on %s\n", pProc.getHandle());
    bValue = pProc.cmdStatus();
    System.out.println("cmdStatus returned " + bValue);
    if (bValue) {
      System.out.println(pProc.getState());
    } else {
      System.out.println(pProc.getServerMessage());
    }
  }
  
  void testFileGetPut(String host)
  {
    String createdFileName = "createdFile.txt";
    String putFileNameB = "/local/temp/putFile-bin.txt";
    String getFileNameB = "getFile-bin.txt";

    try {
      // Create a local file
      File createdLocalFile = new File(createdFileName);
      File retrievedFileB = new File(getFileNameB);
      if (createdLocalFile.exists()) {
        createdLocalFile.delete();
      }
      System.out.println("Creating local file " + createdLocalFile.getCanonicalPath());
      PrintStream outStream = new PrintStream(createdLocalFile);
      for (int i = 0 ; i < 1000 ; i++) {
        outStream.printf("%dn", i);
      }
      outStream.close();
      if (retrievedFileB.exists()) {
        retrievedFileB.delete();
      }
      
      System.out.printf("Putting local file to %s:%s\n", host, putFileNameB);
      PMProcess.cmdPut(createdLocalFile.getCanonicalPath(), putFileNameB, host);
      System.out.printf("Getting remote file to %s\n", getFileNameB);
      
      PMProcess.cmdGet(putFileNameB, getFileNameB, host);
      System.out.printf("%s and %s completed\n", createdFileName, getFileNameB);
      System.out.printf("Now get with force flag false\n");
      try {
        PMProcess.cmdGet(putFileNameB, getFileNameB, host, 10256, false);
      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println("Get failed");
      }
      System.out.printf("Now get with the force flag TRUE\n");
      try {
        PMProcess.cmdGet(putFileNameB, getFileNameB, host, 10256, true);
        System.out.println("Get Success!");
      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println("Get failed");
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
    System.out.println("Done with Get/Put testing.");
    // System.exit(0);
  }
  
  void sleepCycles(int cycles)
  {
    if (cycles < 2) {
      cycles = 2;
    }
    for (int i = 0; i < cycles; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ie) {
        continue;
      } // try/catch
    } // for
  } // sleepCycles()
}   // class UserThread

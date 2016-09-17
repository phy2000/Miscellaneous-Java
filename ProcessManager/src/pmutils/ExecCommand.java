package pmutils;

//import java.io.*;
///import java.nio.*;
import java.nio.channels.*;

//import java.util.Iterator;
//import org.apache.log4j.Logger;

public class ExecCommand
{
  enum ExecState
  {
    ES_INIT, ES_STARTED, ES_DIED, ES_INVALID
  }

  public Object userObj;
  private SelectableChannel _outChannel;
  private SelectableChannel _errChannel;
  private SelectableChannel _inChannel;

  private SelectableStream _outStream;
  private SelectableStream _errStream;
  private SelectableStream _inStream;

  private Process _process;
  private Selector _selector;

  public String[] _env;
  private String _cmdLine[];

  private ExecState _currentState;

  /*
   * TODO - Allow caller to set environment (add, remove, clear) TODO - Use
   * ProcessBuilder class, not just exec()
   */
  public ExecCommand(String[] cmdLine) {
    // _env = env;
    _cmdLine = cmdLine;
    _currentState = ExecState.ES_INIT;
  }

  public ExecState state()
  {
    return _currentState;
  }

  public SelectableChannel getOutChannel()
  {
    return _outChannel;
  }

  public SelectableChannel getErrChannel()
  {
    return _errChannel;
  }

  public SelectableChannel getInChannel()
  {
    return _inChannel;
  }

  public SelectableStream getOutStream()
  {
    return _outStream;
  }

  public SelectableStream getErrStream()
  {
    return _errStream;
  }

  public SelectableStream getInStream()
  {
    return _inStream;
  }

  public Process getProcess()
  {
    return _process;
  }

  public Selector getSelector()
  {
    return _selector;
  }

  public boolean execute(Selector selector, Object userp) throws Exception
  {
    return execute(selector, userp, null);
  }
  
  public boolean execute(Selector selector, Object userp, String[] env) throws Exception
  {
    // _process = Runtime.getRuntime().exec (_cmdLine, _env);
    _process = Runtime.getRuntime().exec(_cmdLine, env);
    _selector = selector;
    // Map stdout to outStream and outChannel
    _outStream = new SelectableStream(_process.getInputStream(), "stdout", this);
    _outStream.start();
    _outChannel = _outStream.getSelectableChannel();
    _outChannel.register(selector, SelectionKey.OP_READ, _outStream);

    // Map stdin to inStream and inChannel
    _inStream = new SelectableStream(_process.getOutputStream(), "stdin", this);
    _inStream.start();
    _inChannel = _inStream.getSelectableChannel();

    // Map stderr to errStream and errChannel
    _errStream = new SelectableStream(_process.getErrorStream(), "stderr", this);
    _errStream.start();
    _errChannel = _errStream.getSelectableChannel();
    _errChannel.register(selector, SelectionKey.OP_READ, _errStream);

    _currentState = ExecState.ES_STARTED;
    userObj = userp; // Save user object
    return true;

  }
}

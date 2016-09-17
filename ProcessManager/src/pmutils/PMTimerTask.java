package pmutils;

import java.util.TimerTask;
import org.apache.log4j.Logger;

public class PMTimerTask extends TimerTask
{

  enum eTask
  {
    E_Kill, E_Suspend, E_Resume, E_Signal
  };

  PMTimerCallback _callback;
  Object _userObj;
  eTask _etask; // What to do when the timer expires
  static Logger logger = Logger.getLogger("ProcMgr");

  public PMTimerTask(Object obj, PMTimerCallback userCb) {
    _userObj = obj;
    _callback = userCb;
  }

  @Override
  public void run()
  {
    _callback.onExpiration(_userObj);
   }

}

package pmserver;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;

import pmutils.Misc;
import pmutils.PMPacket;
import pmutils.PMTimerCallback;

class PMTimerHandler implements PMTimerCallback
{
  static Logger logger = Logger.getLogger("ProcMgr");
  eTimerTask _etask;

  public PMTimerHandler(eTimerTask task) {
    _etask = task;
  }

  public void onExpiration(Object obj)
  {
    PMProcState pProc = (PMProcState) obj;
    int procId = pProc.processId;
    String nativeApp = pProc.nativeApp;
    String cmdStr;
    logger.info(String.format("Timeout handle %s; pid %d; task %s", pProc.procHandle,
        pProc.processId, _etask.toString()));
    switch (_etask) {
    case E_Kill:
      cmdStr = String.format("%s kill -H %s %d", nativeApp,
          pProc.histFile.getAbsolutePath(), procId);
      try {
        Runtime.getRuntime().exec(cmdStr);
      } catch (Exception e) {
        logger.warn(e, e);
      }
      // Force a DEAD status if the process was recovered from disk - there are some instances
      // where the process dies without the history file being updated.
      if (pProc.recovered && pProc.pstate != Misc.procState.PS_DEAD) {
        Calendar todayCal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss");
        pProc.pstate = Misc.procState.PS_DEAD;
        pProc.endTime = todayCal.getTime();
        String buf = ";";
        buf += PMPacket.textField("DEAD");
        buf += PMPacket.textField(dateFormat.format(todayCal.getTime()));
        Misc.writeHistory(pProc.histFile, buf);
      }
      break;
    case E_Suspend:
      cmdStr = String.format("%s suspend -H %s %d", nativeApp, 
          pProc.histFile.getAbsolutePath(), procId);
      try {
        Runtime.getRuntime().exec(cmdStr);
      } catch (Exception e) {
        logger.warn(e, e);
      }
      break;
    case E_Resume:
      cmdStr = String.format("%s resume -H %s %d", nativeApp,
          pProc.histFile.getAbsolutePath(), procId);
      try {
        Runtime.getRuntime().exec(cmdStr);
      } catch (Exception e) {
        logger.warn(e, e);
      }
      break;

    case E_Signal:
      cmdStr = String.format("%s signal -H %s %d", nativeApp, 
          pProc.histFile.getAbsolutePath(), procId);
      try {
        Runtime.getRuntime().exec(cmdStr);
      } catch (Exception e) {
        logger.warn(e, e);
      }
      break;
    case E_RecoverCleanup:
      // Cleanup recovered 
      break;
    }
      
  }
} // class PMTimerHandler
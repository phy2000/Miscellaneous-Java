package pmutils;

public class PMDefines
{
  public final static int PM_VERSION = 0x030103;
	public final static int BUFSIZE = 1024;
	public final static int BIG_BUFSIZE = (1024*1024);
	public final static int PMSERVER_LISTEN_PORT = 10255;
	public final static String CHARSET = "UTF-8";
	public final static String PMCLIENT_SESSION_NAME = "PMDefaultSession";
	public final static String PMCLIENT_ID = "PMClient";
	public final static String PMSERVER_ID = "PMServer";
	
	public final static int ESUCCESS = 0;
	public final static int EBADCMD = 100;
	public final static int DEFAULT_KILL_SECONDS = 300;
	
	public final static String CMDLIST[] = {
		"status",
		"suspend",
		"resume",
		"kill",
		"stdout",
		"stderr",
		"stdin",
		"pid",
		"get",
		"put",
//		"dir",
        "exec",
        "dump",
        "wait",
		"remove"
	};
}

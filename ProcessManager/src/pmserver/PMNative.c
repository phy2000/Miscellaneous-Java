// PMWindows.c : Defines the entry point for the console application.
//

// Make sure precompiled headers are turned off!

#define _CRT_SECURE_NO_WARNINGS 1
#define _WIN32_WINNT _WIN32_WINNT_WINXP
//#include <WinSDKVer.h>
//#include <SDKDDKVer.h>

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>

#if !defined(__CYGWIN__) && !defined(_WIN32)
#include <unistd.h>
#include <sys/wait.h>
#endif

#ifdef _WIN32
#include <io.h>
#include <tchar.h>
#include <windows.h>
#include <direct.h>
#include <TlHelp32.h>

#define PATH_CHAR "\\"

#define open _open
#define write _write
#define close _close
#define read _read

#define SLEEP(x)    Sleep((x)*1000)
#define SLEEP_MS(x)   Sleep(x)
#define GETCWD            _getcwd
#define STRCASECMP(a,b)   _stricmp(a,b)
#define CHDIR(a)        _chdir(a)
#define SIGNAL(a, b)       signalProcess(a,b)
#define STARTPROCESS(a, b, c, d, e) winStartProcess(a, b, c, d, e)
#define PRINTERROR(a, b)  printError(a, b)
#define MAXDIRNAME MAX_PATH

typedef HANDLE FD_TYPE;
typedef enum
{
  SIGNAL_KILL,
  SIGNAL_ABORT,
  SIGNAL_STOP,
  SIGNAL_CONT
} signame;

void signalProcess(int procId, signame sig);
int winStartProcess(char *cmd, char *args[], char *outFile, char *errFile, char *histFile);
BOOL PauseResumeThreadList(DWORD dwOwnerPID, BOOL bResumeThread);
#else 	// !_WIN32

#define PATH_CHAR "/"

#define SLEEP(x) sleep(x)
#define SLEEP_MS(x) usleep((x)*1000)
#define GETCWD getcwd
#define SIGNAL(a, b)  unixSignal(a,b)
#define SIGNAL_KILL SIGKILL
#define SIGNAL_ABORT SIGABRT
#define SIGNAL_STOP SIGSTOP
#define SIGNAL_CONT SIGCONT
#define STARTPROCESS(a, b, c, d, e) unixStartProcess(a, b, c, d, e)
#define STRCASECMP(a,b) strcasecmp(a,b)
#define CHDIR(a)  chdir(a)
#define PRINTERROR(a,b) printError(a,b)
typedef int BOOL;
#ifndef TRUE
#define FALSE 0
#define TRUE (!0)
#endif
extern char **environ;
void unixSignal(int procId, int sig);

#endif	// _WIN32

char tracestr[BUFSIZ];
#define TRACELOG(x) {sprintf(tracestr, "%s:%d %s", __FILE__, __LINE__, (x)); writelog(tracestr);}

char pm_logname[BUFSIZ];

void printError(char * progName, char *execFile);
char *format_timeofday(void);
void addHistory(char *message, char *fName);
char *progname;
BOOL setWorkDir(void);
void writelog(char *);

#ifdef _WIN32
char *tstr2str(LPTSTR);
BOOL
TerminateProcessTree(DWORD dwPID)
{
	char *exeFile;
  BOOL bRet = FALSE;
	BOOL destroyHandle = FALSE;
	HANDLE hProcessSnap;
	PROCESSENTRY32 pe32 = {0};
	DWORD err;
	HANDLE hProc;
	static char outstr[132];

	if (dwPID <= 0) {
		sprintf(outstr, "Invalid PID %d\n", dwPID);
		TRACELOG(outstr);
		fprintf(stderr, "%s", outstr);
		return FALSE;
	}

	// First, open the process the make sure it exists
	hProc = OpenProcess(PROCESS_ALL_ACCESS, FALSE, dwPID);
	if (hProc == NULL) {
		err = GetLastError();
    sprintf(outstr, "PID %d - OpenProcess returned %d\n", dwPID, err);
		TRACELOG(outstr);
    fprintf(stderr, "%s", outstr);
		return FALSE;
	}
	// Close the handle while recursive processing takes place
	CloseHandle(hProc);

  // Take a snapshot of all threads currently in the system.

	hProcessSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
	if (hProcessSnap == INVALID_HANDLE_VALUE) {
		return (FALSE);
	}
	// Fill in the size of the structure before using it.
	pe32.dwSize = sizeof(PROCESSENTRY32);
	if (!Process32First(hProcessSnap, &pe32)) {
		CloseHandle(hProcessSnap);
		return FALSE;
	}


  // Walk the thread snapshot to find all threads of the process.
  // If the thread belongs to the process, add its information
  // to the display list.

	do {
		if (pe32.th32ProcessID == dwPID) {
			exeFile = tstr2str(pe32.szExeFile);
			printf("PID %d EXE File = %s\n", pe32.th32ProcessID, exeFile);
			if (strstr(exeFile, "csrss") != NULL) {
				// Skip dangerous PID - never kill csrss
				TRACELOG("Skip CSRSS");
				free(exeFile);
				return FALSE;
			}
			free(exeFile);
		}
		if (pe32.th32ParentProcessID == dwPID) {
			// Recursive call to kill children of children
			sprintf(outstr, "Recurse PID %d\n", pe32.th32ProcessID);
			TRACELOG(outstr);
			TerminateProcessTree(pe32.th32ProcessID);
		}
	} while (Process32Next(hProcessSnap, &pe32));

	hProc = OpenProcess(PROCESS_ALL_ACCESS, FALSE, dwPID);
	if (hProc == NULL) {
		err = GetLastError();
		sprintf(outstr, "PID %d - OpenProcess returned %d\n", dwPID, err);
		TRACELOG(outstr);
		fprintf(stderr, "%s", outstr);
		CloseHandle(hProcessSnap);
		return FALSE;
	}
	sprintf(outstr, "Terminating PID %d\n", dwPID);
	TRACELOG(outstr);
	if (!TerminateProcess(hProc, 0)) {
		err = GetLastError();
		sprintf(outstr, "TerminateProcess(%x, %d) returned %d\n", hProc, 0, err);
		TRACELOG(outstr);
		fprintf(stderr, "%s", outstr);
	}
	CloseHandle(hProc);
	CloseHandle(hProcessSnap);

  return TRUE;
}

void KillProcess(int procId)
{
	TerminateProcessTree(procId);
  return;
}

void signalProcess(int procId, signame sig)
{
  switch (sig) {
  case SIGNAL_KILL:
    KillProcess(procId);
    break;
  case SIGNAL_STOP:
    PauseResumeThreadList(procId, FALSE);
    break;
  case SIGNAL_CONT:
    PauseResumeThreadList(procId, TRUE);
    break;
  default:
    break;
  }
}
#else
void unixSignal(int procId, int sig)
{
	kill(-procId, sig);
	kill(procId, sig);
}

#endif // _WIN32

void errExit(char * name) 
{
  printf(";\\0;;");
  fflush(stdout);
  SLEEP_MS(10);
  PRINTERROR(progname, name);
  fflush(stderr);
  SLEEP(1);
  exit(1);
}

#ifdef _WIN32
char * tstr2str(LPTSTR tstr)
{
	int i;
  size_t sLen;
	char *str;
  sLen = wcstombs(NULL, tstr, 0);

  str = (char *)malloc(sLen+1);
  sLen++;
  sLen = wcstombs(str, tstr, sLen);
  if (sLen < 0) {
    free(str);
    return NULL;
  }
  for (i = 0 ; i < (int)sLen; i++) {
	  str[i] = tolower(str[i]);
  }
  return str;
}

LPTSTR str2tstr(char *str)
{
  size_t tLen;
  LPTSTR tStr;
  tLen = (strlen(str) + 1) * sizeof (*tStr);
  tStr = (LPTSTR)malloc(tLen);
  tLen = mbstowcs(tStr, str, tLen);
  if (tLen < 0) {
    free(tStr);
    return NULL;
  }
  return tStr;
}

HANDLE openFileHandle(char *fname)
{
  HANDLE h;
#if 1
  int fd;
  fd = open(fname, O_RDWR|O_CREAT, 0666);
  h = (HANDLE)_get_osfhandle(fd);
#else
  LPTSTR tname = str2tstr(fname);
  h = CreateFile(
    tname, //__in      LPCTSTR lpFileName,
    /*GENERIC_READ | */GENERIC_WRITE, //__in      DWORD dwDesiredAccess,
    FILE_SHARE_READ, //__in      DWORD dwShareMode,
    NULL, //__in_opt  LPSECURITY_ATTRIBUTES lpSecurityAttributes,
    CREATE_ALWAYS, //__in      DWORD dwCreationDisposition,
    FILE_ATTRIBUTE_NORMAL, //__in      DWORD dwFlagsAndAttributes,
    NULL //__in_opt  HANDLE hTemplateFile
    );
  free(tname);
#endif
  SetHandleInformation(h, HANDLE_FLAG_INHERIT, HANDLE_FLAG_INHERIT);
  return h;
}

char *quoteSpaces(char *input)
{
	static char output[BUFSIZ];
	size_t i;
	int hasWhiteSpace = 0;
	for (i = 0; i < strlen(input) && i < (BUFSIZ-3); i++) {
		if (isspace(input[i])) {
			hasWhiteSpace = 1;
			break;
		}
	}
	output[0] = 0;
	if (hasWhiteSpace) {
		strcat(output, " \"");
		strcat(output, input);
		strcat(output, "\"");
	} else {
		strcat(output, " ");
		strcat(output, input);
	}
	return output;
}



int winStartProcess(char *cmd, char *args[], char *outFile, char *errFile, char *histFile)
{
  BOOL retVal;
  char *cmdLine;
  size_t cmdLength;
  int i;
  PROCESS_INFORMATION procInfo;
  STARTUPINFO startupInfo;
  LPTSTR tCmdLine;

  HANDLE hErr, hOut;
  hErr = openFileHandle(errFile);
  if (STRCASECMP(outFile, errFile) == 0) {
    hOut = hErr;
  } else {
    hOut = openFileHandle(outFile);
  }

  if (hErr == NULL) {
    errExit(errFile);
  }
  if (hOut == NULL) {
    errExit(outFile);
  }
  memset(&procInfo, 0, sizeof(procInfo));
  memset(&startupInfo, 0, sizeof(startupInfo));

  startupInfo.cb = sizeof(startupInfo);
  startupInfo.hStdError = hErr;
  startupInfo.hStdOutput = hOut;
  startupInfo.hStdInput = GetStdHandle(STD_INPUT_HANDLE);
  startupInfo.dwFlags = STARTF_USESTDHANDLES;
  cmdLength = strlen(cmd) + 2; // Quotes around cmd are needed
  for (i = 1; args[i] != NULL; i++) {
    cmdLength += strlen(args[i]);
  }
  cmdLength += (3*i) + 1; // Add space char and 2 quotes, plus null character
  cmdLine = (char *)malloc(cmdLength);
  *cmdLine = '\0';
  strcat(cmdLine, "\"");
  strcat(cmdLine, cmd);
  strcat(cmdLine, "\"");
  for (i = 1; args[i] != NULL; i++) {
    strcat(cmdLine, quoteSpaces(args[i]));
    //strcat(cmdLine, " \"");
    //strcat(cmdLine, args[i]);
    //strcat(cmdLine, "\"");
  }

	TRACELOG(cmdLine);
  tCmdLine = str2tstr(cmdLine);

  if (!setWorkDir()) {
    printf(";\\0;;");
    fprintf(stderr, "%s: No working directory!\n", progname);
    SLEEP_MS(10);
    return errno;
  }
  retVal = CreateProcess(
    NULL, //__in_opt     LPCTSTR lpApplicationName,
    tCmdLine, //__inout_opt  LPTSTR lpCommandLine,
    NULL, //__in_opt     LPSECURITY_ATTRIBUTES lpProcessAttributes,
    NULL, //__in_opt     LPSECURITY_ATTRIBUTES lpThreadAttributes,
    TRUE, //TRUE, //__in         BOOL bInheritHandles,
    CREATE_SUSPENDED, //__in         DWORD dwCreationFlags,
    NULL, //__in_opt     LPVOID lpEnvironment,
    NULL, //__in_opt     LPCTSTR lpCurrentDirectory,
    &startupInfo, //__in         LPSTARTUPINFO lpStartupInfo,
    &procInfo //__out        LPPROCESS_INFORMATION lpProcessInformation
    );
  free(tCmdLine);

  if (!retVal) {
    addHistory("FAIL", histFile);
    errExit(cmd);
  } else {
    DWORD procId = procInfo.dwProcessId;
    DWORD resumeVal;
    printf(";\\%d;;", procId);
    fflush(stdout);
    addHistory("EXEC", histFile);
    SLEEP_MS(100);
    resumeVal = ResumeThread(procInfo.hThread);
    WaitForSingleObject(procInfo.hProcess, INFINITE);
    addHistory("DEAD", histFile);
  }

  return 0;
}

BOOL PauseResumeThreadList(DWORD dwOwnerPID, BOOL bResumeThread)
{
  HANDLE hThreadSnap = NULL;
  BOOL bRet = FALSE;
  THREADENTRY32 te32 = {0};

  // Take a snapshot of all threads currently in the system.

  hThreadSnap = CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
  if (hThreadSnap == INVALID_HANDLE_VALUE) {
    return (FALSE);
  }

  // Fill in the size of the structure before using it.

  te32.dwSize = sizeof(THREADENTRY32);

  // Walk the thread snapshot to find all threads of the process.
  // If the thread belongs to the process, add its information
  // to the display list.

  if (Thread32First(hThreadSnap, &te32)) {
    do {
      if (te32.th32OwnerProcessID == dwOwnerPID) {
        HANDLE hThread = OpenThread(THREAD_SUSPEND_RESUME, FALSE, te32.th32ThreadID);
        if (bResumeThread) {
          ResumeThread(hThread);
        } else {
          SuspendThread(hThread);
        }
        CloseHandle(hThread);
      }
    }
    while (Thread32Next(hThreadSnap, &te32));
    bRet = TRUE;
  } else {
    bRet = FALSE; // could not walk the list of threads
  }

  // Do not forget to clean up the snapshot object.
  CloseHandle (hThreadSnap);

  return (bRet);
}

#else
int unixStartProcess(char *cmd, char *args[], char *outFile, char *errFile, char *histFile)
{
  int execRet;
  pid_t pid;
  int errFd = -1;
  int outFd = -1;

  int status;
  if (outFile != NULL) {
    outFd = open(outFile, O_RDWR|O_CREAT, 0666);
    if (outFd < 0) {
      PRINTERROR(progname, outFile);
      return errno;
    }
  }
  if (errFile != NULL) {
    if (strcmp(errFile, outFile) == 0) {
      errFd = outFd;
    } else {
      errFd = open(errFile, O_RDWR|O_CREAT, 0666);
      if (errFd < 0) {
        PRINTERROR(progname, errFile);
        return errno;
      }
    }
  }
  if (!setWorkDir()) {
    printf(";\\0;;");
		fflush(stdout);
		SLEEP_MS(10);
		fprintf(stderr, "No working directory!\n");
		TRACELOG("No working directory\n");
    return errno;
  }

  pid = fork();
  if (pid == 0) {
		setpgid(0, 0);
		{
			char **envp = environ;
			while (*envp) {
				TRACELOG(*envp);
				envp++;
			}
		}
    // Forked child
    // set stdout and stderr to files opened earlier
    if (errFd >= 0) {
      dup2(errFd, 2);
    }
    if (outFd >= 0) {
      dup2(outFd, 1);
    }
    SLEEP_MS(100);
    execRet = execvp(cmd, args);
		perror(cmd);
		exit(1);
  } else {
    if (pid < 0) {
      printf(";\\0;;");
      fflush(stdout);
      SLEEP_MS(10);
      PRINTERROR(progname, cmd);
      addHistory("ERROR", histFile);
      SLEEP(1);
    } else {
      printf(";\\%d;;", pid);
      fflush(stdout);
      TRACELOG("EXEC");
      addHistory("EXEC", histFile);
			close(errFd);
			if (errFd != outFd) {
				close(outFd);
			}
      wait(&status);
      addHistory("DEAD", histFile);
    }
  }
  return status;
}

#endif	// else _WIN32

void printError(char * progName, char *execFile)
{
#ifdef _WIN32
  // Retrieve the system error message for the last-error code

  LPTSTR lpMsgBuf;
  DWORD dw = GetLastError();
  char charBuf[BUFSIZ];

  FormatMessage(
    FORMAT_MESSAGE_ALLOCATE_BUFFER |
    FORMAT_MESSAGE_FROM_SYSTEM |
    FORMAT_MESSAGE_IGNORE_INSERTS,
    NULL,
    dw,
    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
    (LPTSTR) &lpMsgBuf,
    0, NULL );
  wcstombs(charBuf, lpMsgBuf, BUFSIZ);

  // Display the error message and exit the process

  fprintf(stderr, "command %s: %s\n", execFile, charBuf);
  LocalFree(lpMsgBuf);
#else
    char errmsg[256];

    sprintf(errmsg, "%s", execFile);
    perror(errmsg);
		fprintf(stderr, "\n");
#endif
}

void writelog(char *str)
{
  char *date = format_timeofday();
  int fd = open(pm_logname, O_CREAT|O_APPEND|O_RDWR, 0666);
  if (fd < 0) {
    perror(pm_logname);
		return;
  }
  write(fd, date, (unsigned)strlen(date));
  write(fd, "--", 2);
  write(fd, str, (unsigned)strlen(str));
  if (str[strlen(str) - 1] != '\n') {
    write(fd, "\n", 1);
  }
  close(fd);
}

BOOL
  setWorkDir(void)
{
#ifdef _WIN32
  char tmpbuf[MAX_PATH];
#endif

  char *workdir = getenv("PM_WORKDIR");
  if (workdir == NULL) {
    workdir = getenv("TEMP");
    if (workdir == NULL) {
      workdir = getenv("TMP");
      if (workdir == NULL) {
#ifdef _WIN32
        GetWindowsDirectoryA(tmpbuf, MAX_PATH);
        strcat(tmpbuf, "\\TEMP");
        workdir = tmpbuf;
#else
        workdir = "/local/temp";
#endif
      } // Windows directory/TEMP
    } // PM_WORKDIR
  }
	TRACELOG(workdir);
	if (CHDIR(workdir) != 0) {
		return FALSE;
	}
  return TRUE;
}

char *
  format_timeofday(void)
{
#ifdef _WIN32
  time_t tt;
#else
  struct timeval tv;
#endif
  struct tm* ptm;
  static char time_string[40];

  /* Obtain the time of day, and convert it to a tm struct. */
#ifdef _WIN32
  time(&tt);
  ptm = localtime(&tt);
#else
  gettimeofday(&tv, NULL);
  ptm = localtime(&tv.tv_sec);
#endif

  /* Format the date and time, down to a single second. */
  strftime(time_string, sizeof(time_string), "%Y-%m-%d:%H-%M-%S", ptm);
  return time_string;
}

void addHistory(char *message, char *fName)
{
  int histFd;
  char stateMsg[BUFSIZ];
  if (fName == NULL) {
    return;
  }
  histFd = open(fName, O_CREAT|O_RDWR|O_APPEND, 0666);
  if (histFd >= 0) {
    sprintf(stateMsg, ";%s;\\%s;;\n", message, format_timeofday());
    write(histFd, stateMsg, (unsigned)strlen(stateMsg));
    close(histFd);
  } else {
    perror(fName);
    TRACELOG("error opening history file\n");
  }
}
// PMNative exec -O stdout.file -E stderr.file "execfile" "exec args"
int main(int argc, char* argv[])
{
  int execRet;
  char *cmd = "";
  char *outName = NULL;
  char *errName = NULL;
  char *historyName = NULL;  // File to create when the process dies
	char *parentDir = NULL;
	int mix = FALSE;
#if _WIN32
    // Windows has no line buffering - use no buffering
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
#else
    setvbuf(stdout, NULL, _IOLBF, 0);
#endif
	(void)GETCWD(pm_logname, BUFSIZ);
	strcat(pm_logname, PATH_CHAR "PMNative.log");

	TRACELOG("Start\n");

  progname = *argv++; argc--;
  if (argc > 0) {
    cmd = *argv++; argc--;
  } else {
    // TODO - print usage
    exit(EINVAL);
  }

	while ((*argv)[0] == '-' && argc > 0) {
		switch ((*argv)[1]) {
			case 'O': // Output file name
				outName = argv[1];
				argv++; argc--;
				break;
			case 'E': // Error file name
				errName = argv[1];
				argv++; argc--;
				break;
			case 'H': // History file name
				historyName = argv[1];
				argv++; argc++;
				break;
			case 'd':
				parentDir = argv[1];
				argv++; argc--;
				break;
			case 'M':
				mix = TRUE;
				break;
			default:
				break;
		}
    argv++; argc--;
  }

	if (parentDir) {
		historyName = malloc(BUFSIZ);
		strcpy(historyName, parentDir);
		strcat(historyName, PATH_CHAR "history.dat");
		outName = malloc(BUFSIZ);
		strcpy(outName, parentDir);
		strcat(outName, PATH_CHAR "stdout.dat");
		// If mixing stdout and stderr, copy output name to error name
		if (mix) {
			errName = outName;
		} else {
			errName = malloc(BUFSIZ);
			strcpy(errName, parentDir);
			strcat(errName, PATH_CHAR "stderr.dat");
		} 
	}

  if (STRCASECMP(cmd, "exec") == 0) {
    // EXEC command
		char *envName = malloc(BUFSIZ);
    char *execFile;
    char **execArgs;
		if (parentDir) {
			char **envp = environ;
			FILE *fenv;
			strcpy(envName, parentDir);
			strcat(envName, PATH_CHAR "environ.dat");
			fenv = fopen(envName, "w");
			if (fenv) {
				while (*envp) {
					fprintf(fenv, "%s\n", *envp);
					envp++;
				}
				fclose(fenv);
			}
		}

    // Execute the command
    // Make sure the argv is terminated with a NULL pointer
    argv[argc] = NULL;
		execFile = *argv;
		execArgs = argv;

    execRet = STARTPROCESS(execFile, execArgs, outName, errName, historyName);
    return execRet;
  } else {
    if (STRCASECMP(cmd, "KILL") == 0) {
      // KILL cmd
      int procId = strtol(*argv, NULL, 0);
      argv++; argc--;
      if (argc < 0) {
				printf("INTERNAL ERROR: invalid kill command line\n");
				return EINVAL;
      }
      addHistory("KILL", historyName);
      SIGNAL (procId, SIGNAL_KILL);
    } else if (STRCASECMP(cmd, "suspend") == 0) {
      int procId = strtol(*argv, NULL, 0);
      argv++; argc--;
      if (argc < 0) {
				printf("INTERNAL ERROR: invalid suspend command line\n");
        return EINVAL;
      }
      addHistory("SUSPEND", historyName);
      SIGNAL (procId, SIGNAL_STOP);
    } else if (STRCASECMP(cmd, "resume") == 0) {
      int procId = strtol(*argv, NULL, 0);
      argv++; argc--;
      if (argc < 0) {
				printf("INTERNAL ERROR: invalid resume command line\n");
        return EINVAL;
      }
      addHistory("RESUME", historyName);
      SIGNAL (procId, SIGNAL_CONT);
    } // if (resume)
  } // if (!exec)
  return 0;
}

package pmutils;
import java.net.InetAddress;

/**
 * @author pyoung
 * @version     3.0                   
 * @since       2012-05-11
 */
public class OsNameArch
{
  
  static final String Usage = "Usage: OsNameArch [hostname|osname|arch|all]\n" +
  "\tPrint the hostname, Operating system name, architecture, or all 3\n";

  /**
   * @param args
   * If no args are passed, return Operating System and Architecture
   * Else, return "hostname", "osname", "arch", or "all"
   */
  public static void main(String[] args)
  {
    String osname = System.getProperty("os.name").replaceAll(" ", "_");
    String osarch = System.getProperty("os.arch").replaceAll(" ", "_");
    String hostname;
    
    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (Exception e) {
      hostname = String.format("NONAME.%s.%s", osname, osarch);
      e.printStackTrace(System.err);
     // System.err.printf("getLocalHost().getCanonicalHostName(): %s\n", e.getMessage());
    }
    
    if (args.length == 0) {
      System.out.printf("%s-%s", osname, osarch);
    } else if (args[0].compareToIgnoreCase("hostname") == 0) {
      System.out.printf("%s", hostname);
    } else if (args[0].compareToIgnoreCase("osname") == 0) {
      System.out.printf("%s", osname);
    } else if (args[0].compareToIgnoreCase("arch") == 0) {
      System.out.printf("%s", osarch);
    } else if (args[0].compareToIgnoreCase("all") == 0) {
      System.out.printf("%s-%s-%s", hostname, osname, osarch);
    } else {
      System.err.printf("%s\n", Usage);
    }
  }

} // Class OsNameArch

import java.io.*;
//import gnu.getopt.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class TimedPipe {
	private static final Logger LOG = LoggerFactory.getLogger(TimedPipe.class);

	static String m_msec = "100"; // number of msecs between sends
	static double m_variance, m_interval;

	public static void main(String[] args) {
		String _progname = Thread.currentThread().getStackTrace()[1].getClassName();
		process_cmdline(_progname, args);

		try {
			BufferedReader bread = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				long prevTime = System.currentTimeMillis();
				String strLine = bread.readLine();
				if (strLine == null) {
					break;
				}
				System.out.println(strLine);
				long currTime = System.currentTimeMillis();
				long next = nextInterval(prevTime, currTime);
				if (next > 0)
					Thread.sleep(next);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void process_cmdline(String _progname, String[] args) {

		switch (args.length) {
		case 0:
			break;
		case 1:
			m_msec = args[0];
			break;
		default:
			Usage.print_and_exit(_progname);
			break;
		}

		try {
			Integer.parseInt(m_msec);
		} catch (NumberFormatException nfe) {
			System.err.println("Number error: " + nfe.getMessage());
			Usage.print_and_exit(_progname);
		}
		m_interval = Double.valueOf(m_msec);
		m_variance = (m_interval * 2 / 3.0);
		m_interval = m_variance;

	} // process_cmdline

	static long nextInterval(long prevTime, long currTime) {

		double rand = Math.random();
		long next = Math.round((rand * m_variance) + m_interval);
		LOG.debug("Random = " + next);
		next -= (currTime - prevTime);
		return next > 0 ? next : 0;
	}
}

class Usage {
	final static String message = "Usage: %s [<pausetime>]\n";

	static void print_and_exit(String progname) {
		System.err.printf(String.format(Usage.message, progname));
		System.exit(1);
	}
} // Class Usage

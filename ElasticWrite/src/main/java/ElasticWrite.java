import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import gnu.getopt.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;
import io.searchbox.indices.mapping.PutMapping;

public class ElasticWrite {
	// private static final Logger LOG =
	// LoggerFactory.getLogger(ElasticWrite.class);

	public static void main(String[] args) {
		String _progname = Thread.currentThread().getStackTrace()[1].getClassName();
		ElasticWriteSub subClass = new ElasticWriteSub(_progname);
		subClass.subMain(args);
	}
}

class ElasticWriteSub {
	String m_progName;
	String m_indexName;
	String m_typeName;
	String m_elasticUrl;
	String m_jsonFileName = null;
	String m_mappingJson;
	JestClient m_jestClient;
	private boolean m_debugMode = false;
	private static final Logger LOG = LoggerFactory.getLogger(ElasticWrite.class);

	ElasticWriteSub(String progname) {
		m_progName = progname;
	}

	void subMain(String[] args) {
		process_cmdline(m_progName, args);
		Path mapPath = null;
		try {
			if (m_jsonFileName != null) {
				mapPath = Paths.get(m_jsonFileName);
				byte[] inbuf = Files.readAllBytes(mapPath);
				m_mappingJson = new String(inbuf, Charset.defaultCharset());
			}
			open();
			BufferedReader bread = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String strLine = bread.readLine();
				if (strLine == null) {
					break;
				}
				write(strLine);
			}
		} catch (Exception e) {
			LOG.error("Fatal: ", e);
		}
	}

	void open() throws Exception {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig.Builder(m_elasticUrl).multiThreaded(true).build());
		m_jestClient = factory.getObject();
		if (m_mappingJson != null) {

		}
		PutMapping putMapping = new PutMapping.Builder(m_indexName, m_typeName, m_mappingJson).build();
		try {
			m_jestClient.execute(putMapping);
		} catch (Exception e) {
			LOG.error("ES execute: ", e);
			throw e;
		}

	}

	void write(String event) throws Exception {
		Index index = new Index.Builder(event).index(m_indexName).type(m_typeName).build();
		if (m_debugMode) {
			System.out.println("Write <" + event + "> to " + m_indexName + ":" + m_typeName);
		} else {
			m_jestClient.execute(index);
		}
	}

	void process_cmdline(String progName, String[] args) {
		int argId;
		LongOpt[] longopts = new LongOpt[] { new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 'd'),
				new LongOpt("index", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
				new LongOpt("type", LongOpt.REQUIRED_ARGUMENT, null, 't'),
				new LongOpt("url", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
				new LongOpt("map", LongOpt.REQUIRED_ARGUMENT, null, 'm') };

		Getopt g = new Getopt(progName, args, "+di:t:u:m:", longopts);
		// g.setOpterr(false); // We'll do our own error handling

		while ((argId = g.getopt()) != -1) {
			switch (argId) {

			case 'd':
				m_debugMode = true;
				break;
			case 'i':
				// index name
				m_indexName = g.getOptarg();
				break;
			case 't':
				// index type
				m_typeName = g.getOptarg();
				break;
			case 'u':
				// URL to
				m_elasticUrl = g.getOptarg();
				break;
			case 'm':
				m_jsonFileName = g.getOptarg();
				break;
			case '?':
			default:
				Usage.print_and_exit(progName);
				break;
			}
		}
		// int nextOpt = g.getOptind();
		if (m_indexName == null || m_typeName == null || m_elasticUrl == null) {
			Usage.print_and_exit(progName);
		}

	} // process_cmdline

}

class Usage {
	final static String message = "Usage: %s --index=<indexname> --type=<type> --url=<URL> [--map=<mapfile>]\n";

	static void print_and_exit(String progname) {
		System.err.printf(String.format(Usage.message, progname));
		System.exit(1);
	}
} // Class Usage

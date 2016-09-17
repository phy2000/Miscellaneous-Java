package com.mapr.examples;

import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * This producer will send a bunch of messages to topic "fast-messages". Every
 * so often, it will send a message to "slow-messages". This shows how messages
 * can be sent to multiple topics. On the receiving end, we will see both kinds
 * of messages but will also see how the two topics aren't really synchronized.
 */
public class Producer2 {
	public static void main(String[] args) throws IOException {
		String infile = "stdin", outfile = "stdout", desttopic;
		System.err.printf("args.length = %d\n", args.length);
		String progname = args[0];
		if (args.length <= 1) {
			System.err.printf("Usage: %s <desttopic>\n", progname);
			System.exit(1);
		}
		desttopic = args[1];
		if (args.length > 2) {
			infile = args[2];
			System.setIn(new FileInputStream(infile));
		}
		if (args.length > 3) {
			outfile = args[3];
			System.setOut(new PrintStream(outfile));
		}
		System.err.printf("%s %s %s %s\n", progname, desttopic, infile, outfile);
		// set up the producer
		KafkaProducer<String, byte[]> producer;
		try (InputStream props = Resources.getResource("producer.props").openStream()) {
			Properties properties = new Properties();
			properties.load(props);
			producer = new KafkaProducer<>(properties);
		}

		try {
			byte[] buf = new byte[4096];
			BufferedInputStream inStream = new BufferedInputStream(System.in);
			int count = 0;
			do {
				try {
					count = inStream.read(buf);
				} catch (IOException io) {
					io.printStackTrace(System.err);
					break;
				}
				if (count <= 0) {
					break;
				}
				System.out.write(buf, 0, count);
				producer.send(new ProducerRecord<String, byte[]>(desttopic, Arrays.copyOf(buf, count)));
			} while (count > 0);
		} catch (Throwable throwable) {
			throwable.printStackTrace(System.err);
		} finally {
			producer.close();
		}

	}
}

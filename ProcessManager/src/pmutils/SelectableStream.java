package pmutils;

import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ClosedByInterruptException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.apache.log4j.*;

/**
 * Class which encapsulates any stream as a selectable channel.
 * Instantiate this class, call start() on it to run the background
 * thread, then call getSelectableChannel() to get a SelectableChannel
 * object which can be used with a Selector object.
 *
 * @author Ron Hitchens (ron@ronsoft.com)
 * created: Jan 2003
 * Modified pyoung@informatica.com to handle general input and output streams
 */
public class SelectableStream
{
	static Logger logger = Logger.getLogger("ProcMgr");
	public static int BUFSIZE = 1024;
	Pipe _pipe;
	PipeThread _pipeThread;
	boolean _isInputStream;
	boolean _isRunning = false;
	SelectableChannel _channel;
	String _name;
	private ByteBuffer _readbuffer;
	Object _clientd;

	/*
	 * Constructor to create selectable input channel from a stream
	 */
	public SelectableStream (InputStream in, String name, Object client) throws IOException
	{
		_pipe = Pipe.open();

		_pipeThread = new PipeThread (in, _pipe.sink(), name);
		_isInputStream = true;
		_channel = _pipe.source();
		_channel.configureBlocking(false);
		_name = name;
		_readbuffer = ByteBuffer.allocate(BUFSIZE);
		_clientd = client;
	}

	/*
	 * Constructor to create selectable output channel from a stream
	 */
	public SelectableStream (OutputStream out, String name, Object clientd) throws IOException
	{
		_pipe = Pipe.open();

		_pipeThread = new PipeThread (out, _pipe.source(), name);
		_isInputStream = false;
		_channel = _pipe.sink();
		_channel.configureBlocking(false);
		_name = name;
		_readbuffer = ByteBuffer.allocate(BUFSIZE);
		_clientd = clientd;
	}

	/*
	 * Call start to start reading thread
	 */
	public void start()
	{
		if (!_isRunning) {
			_isRunning = true;
			_pipeThread.start();
		}
	}

	public Object getClientd()
	{
		return _clientd;
	}

	public String getName()
	{
		return _name;
	}
	public ByteBuffer getBuffer()

	{
		return _readbuffer;
	}

	public boolean isStarted()
	{
		return _isRunning;
	}

	/*
	 * return the selectable channel object
	 */
	public SelectableChannel getSelectableChannel() throws IOException
	{
		return (_channel);
	}

	protected void finalize()
	{
		_pipeThread.shutdown();
	}

	/*
	 * Thread to perform blocking I/O for the selectable channel
	 */
	public static class PipeThread extends Thread
	{
		boolean _keepRunning = true;
		byte [] _bytes = new byte [BUFSIZE];
		ByteBuffer _buffer = ByteBuffer.wrap (_bytes);
		static Logger logger = Logger.getLogger("ProcMgr");

		InputStream _inStream;
		OutputStream _outStream;
		WritableByteChannel _outChannel;
		ReadableByteChannel _inChannel;
		boolean _isInStream;
		private String _name;
		public String getMyName()
		{
			return _name;
		}


		/*
		 * Constructor for readable stream
		 */
		PipeThread (InputStream inStream, WritableByteChannel outChannel, String name)
		{
			this._inStream = inStream;
			this._outChannel = outChannel;
			this.setDaemon (true);	// Needed so JVM will not wait on this thread
			_isInStream = true;
			_name = name;
			setName(_name+"Read");
		}

		/*
		 * Constructor for a writable stream
		 */
		PipeThread (OutputStream outStream, ReadableByteChannel inChannel, String name)
		{
			this._inChannel = inChannel;
			this._outStream = outStream;
			this.setDaemon (true);	// Needed so JVM will not wait on this thread
			_isInStream = false;
			_name = name;
			setName(_name+"Write");
		}

		public void shutdown()
		{
			_keepRunning = false;
			this.interrupt();
		}

		public void run()
		{
//			logger.debug("Enter " + _name);
			while (_keepRunning) {
				int count;
				int nBytes;
				try {
					if (_isInStream) {
						/* Blocking point - no data available on other side of stream */
						count = _inStream.read (_bytes);
						if (count < 0) {
							break;
						}
						_buffer.clear().limit (count);
						//_buffer.flip();
						nBytes = 0;
						while (_buffer.hasRemaining()) {
							nBytes += _outChannel.write (_buffer);
						}
						logger.assertLog(nBytes == count, String.format("nBytes = %d, count = %d", nBytes, count));
					} else {
						/* May block here */
						try {
							count = _inChannel.read (_buffer);
						} catch (ClosedByInterruptException e) {
							// Expected interrupt if the channel gets closed on the other end
							logger.info(e, e);
							break;
						}
						if (count < 0) {
							break;
						}
						_buffer.clear().limit (count);
						/* Blocking point - if other end of stream is not reading */
						_outStream.write (_bytes);
						_outStream.flush();
					}
				} catch (IOException e) {
					logger.error(e, e);
				} // try-catch
			}	// while
			try {
				if (_isInStream) {
					_outChannel.close(); _outChannel = null;
					_inStream.close(); _inStream = null;
				} else {
					_inChannel.close(); _inChannel = null;
					_outStream.close(); _outStream = null;
				}
				this.shutdown();
			} catch (Exception e) {
				logger.error(e, e);
			} // try-catch
//			logger.debug("Exit " + _name);
		}	// run
	}	// class PipeThread
}	// class SelectableStream


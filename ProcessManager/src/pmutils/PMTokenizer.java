package pmutils;

import java.nio.*;
import java.nio.charset.*;
import java.lang.Exception;
import org.apache.log4j.*;

public class PMTokenizer
{
	static Logger logger = Logger.getLogger("ProcMgr");
	static Charset _charset = Charset.forName(PMDefines.CHARSET);
	static CharsetEncoder _encoder = _charset.newEncoder();
	static CharsetDecoder _decoder = _charset.newDecoder();

	public final static char Escape = '\\';
	public final static char Separator = ';';
	public enum tState {
		TS_Start,
		TS_WaitEnd,
		TS_Escape,
		TS_Binary,
		TS_End
	}
	tState tstate;
	ByteBuffer _tokenBuf;
	int binaryLength;

	public PMTokenizer()
	{
		tstate = tState.TS_Start;
		_tokenBuf = ByteBuffer.allocateDirect(PMDefines.BUFSIZE);
		PMPacket.initBuf(_tokenBuf);
	}

	public boolean isBinary()
	{
		return (tstate == tState.TS_Binary);
	}

	public ByteBuffer getToken()
	{
		return _tokenBuf;
	}

	public String getTokenString() throws Exception
	{
		_tokenBuf.mark();
		String tString = _decoder.decode(_tokenBuf).toString();
		_tokenBuf.rewind();
		return tString;
	}

	public void startBinary(int length) throws Exception
	{
		switch (tstate) {
			case TS_Start:
			case TS_End:
				tstate = tState.TS_Binary;
				binaryLength = length;
				break;
			default:
				Exception e = new Exception("Illegal state: " + tstate);
				throw e;
		}	// switch (tstate)
	}	// startBinary

	public void clear()
	{
		PMPacket.initBuf(_tokenBuf);
		tstate = tState.TS_Start;
	}

	public tState state()
	{
		return tstate;
	}

	public boolean parse(ByteBuffer inBuf) throws Exception
	{
//		logger.debug("Enter");
		byte b;
		while (inBuf.hasRemaining()) {
			b = inBuf.get();
			switch (tstate) {
				case TS_End:
				case TS_Start:
					switch (b) {
						case Separator:
							break;
						case Escape:
							tstate = tState.TS_Escape;
							break;
						default:
							tstate = tState.TS_WaitEnd;
							_tokenBuf.limit(_tokenBuf.limit()+1); _tokenBuf.put(b);
							break;
					}
					break;
				case TS_WaitEnd:
					switch (b) {
						case Separator:
							// Token completed
							tstate = tState.TS_End;
							_tokenBuf.rewind();
							return true;
						case Escape:
							tstate = tState.TS_Escape;
							break;
						default:
							_tokenBuf.limit(_tokenBuf.limit()+1); _tokenBuf.put(b);
							break;
					}
					break;
				case TS_Escape:
          _tokenBuf.limit(_tokenBuf.limit()+1); _tokenBuf.put(b);
					tstate = tState.TS_WaitEnd;
					break;
				case TS_Binary:
					_tokenBuf.put(b);
					break;
				default:
					// Unhandled state
					Exception e = new Exception("Unhandled Tokenizer State:" + tstate);
					throw e;
			}	// switch
		}	// while	hasRemaining()
		return false;
	}	// parse()
}

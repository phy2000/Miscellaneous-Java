package pmutils;

import java.io.FileInputStream;
import java.nio.*;
import java.nio.charset.*; //import java.lang.Exception;
import java.util.ArrayList;
import org.apache.log4j.*;

/**
 * Packets are strings of  separated by semicolons and terminated by
 * double semicolons. '\' is an escape if special characters are in the field.
 * If a field begins with a digit, it will be treated as a length of the next field.
 * The next field is read directly with no interpretations.
 * For instance:
 * ";;CMD;arg1;arg2;arg3;;
 * ";;CMD;12345;\field of 12345 bytes...;arg2;;"
 * The packets must be preceded by at least 1 semicolon, and terminated by at least
 * 2 semicolons
 * 
 * @author      Paul Young <pyoung@informatica.com>
 * @version     1.0                   
 * @since       2012-01-01
 */
public class PMPacket
{
  static Charset _charset = Charset.forName(PMDefines.CHARSET);
  static CharsetEncoder _encoder = _charset.newEncoder();
  static CharsetDecoder _decoder = _charset.newDecoder();
  ByteBuffer packetBuf;
  static Logger logger = null;

  public ArrayList<ByteBuffer> args;

  /**
   * 
   * @author pyoung
   *
   */
  public enum pktState
  {
    PS_Init, PS_Start, PS_Data, PS_StartBinary, PS_Binary, PS_Length, PS_Escape, PS_EndField, PS_End, PS_Error, PS_Error2
  }

  /**
   * 
   */
  pktState pstate;
  /**
   * 
   */
  int length = -1;
  ByteBuffer currentBuf = null;

  /**
   * Input packet processor. 
   * @param log - log4j logger object, used to log trace information.
   */
  public PMPacket(Logger log) {
    pstate = pktState.PS_Init;
    args = new ArrayList<ByteBuffer>();
    logger = log;
  }

  public Logger setLogger(Logger log)
  {
    Logger previous = logger;
    logger = log;
    return previous;
  }

  /**
   * Convert a String into a ByteBuffer.
   * @param from
   * @return output ByteBuffer.
   */
  public static ByteBuffer convertBuf(String from) 
  {
    ByteBuffer out = ByteBuffer.wrap(from.getBytes());
    out.position(0).limit(out.capacity());
    return out;
  }

  /**
   * Convert a ByteBuffer into a String
   * @param from
   * @return output String.
   * @throws CharacterCodingException
   */
  public static String convertStr(ByteBuffer from) throws CharacterCodingException
  {
    String str;
    int pos = from.position();
    str = _decoder.decode(from).toString();
    from.position(pos);
    return str;
  }
  
  /**
   * Convert a ByteBuffer ArrayList into a String array
   * @param from input ByteBuffer ArrayList
   * @return String array
   * @throws CharacterCodingException
   */
  public static String[] convertStrArray(ArrayList<ByteBuffer> from) throws CharacterCodingException
  {
    int size = from.size();
    String value[] = new String[size];
    for (int i = 0; i < size; i++) {
      value[i] = convertStr(from.get(i));
    }
    return value;
  }
  
  /**
   * Convert a string array into a single string separated by spaces. 
   * @param array Input string array.
   * @return converted String.
   */
  public static String convertStr(String array[])
  {
    String str = "";
    for (int i = 0; i < array.length; i++) {
      str += "\""+ array[i] + "\"";
      if (i < (array.length - 1)) {
        str += " ";
      }
    }
    return str;
  }

  /**
   * Convert a bytebuffer into an int
   * @param from input ByteBuffer
   * @return converted contents of the input.
   * @throws NumberFormatException If the input cannot be parsed into an int.
   * @throws CharacterCodingException If the input contains invalid bytes.
   */
  public static int convertInt(ByteBuffer from) throws NumberFormatException,
      CharacterCodingException
  {
    return Integer.parseInt(_decoder.decode(from).toString().trim());
  }

  /**
   * Initialize a ByteBuffer before reading.
   * @param buf Input ByteBuffer
   */
  public static void initBuf(ByteBuffer buf)
  {
    buf.position(0).limit(0).mark();
  }

  /**
   * Create a semicolon separated text field with escape characters where necessary.
   * @param input
   * @return A string suitable for adding to an outgoing packet.
   */
  public static String textField(String input)
  {
    String outStr = "";
    // Leading digit should be escaped
    if (Character.isDigit(input.charAt(0))) {
      outStr += '\\';
    }
    for (int i = 0; i < input.length(); i++) {
      switch (input.charAt(i)) {
      case ';':
      case '\\':
        outStr += '\\';
        outStr += input.charAt(i);
        break;
      default:
        outStr += input.charAt(i);
        break;
      }
    }
    outStr += ";";
    return outStr;
  }

  /**
   * Create a binary packet field for a packet.
   * @param input contents of the field
   * @return a semi-colon delimited field suitable for inclusion in an outgoing packet.
   * @throws CharacterCodingException
   */
  public static String binaryField(ByteBuffer input) throws CharacterCodingException
  {
    String outStr = "";
    outStr += input.limit() - input.position();
    outStr += ";" + convertStr(input);
    outStr += ";";
    return outStr;
  }

  /**
   * Adds a byte to a ByteBuffer, allocating a new one if necessary.
   * @param b Byte to insert.
   * @param buf The ByteBuffer to insert the byte.
   * @return the new buffer, if it was created, the old one otherwise.
   */
  // It then returns the new (or old) ByteBuffer;
  public static ByteBuffer putByte(byte b, ByteBuffer buf)
  {
    if (buf.limit() == buf.capacity()) {
      ByteBuffer newBuf = ByteBuffer.allocate(buf.capacity() + PMDefines.BUFSIZE);
      System.arraycopy(buf.array(), 0, newBuf.array(), 0, buf.limit());
      newBuf.position(0).mark();
      newBuf.limit(buf.limit()).position(buf.position());
      buf = newBuf;
    }
    buf.limit(buf.limit() + 1);
    buf.put(b);
    return buf;
  }

  /**
   * Appends a byte array to an existing bytebuffer
   * @param src Source array of bytes
   * @param dest Destination ByteBuffer
   * @param length Number of bytes to insert.
   * @return The new ByteBuffer, if one was created. Otherwise the original ByteBuffer.
   */
  // An new ByteBuffer will be allocated if the destination is over capacity
  // The new or old destination is returned.
  public static ByteBuffer putByteArray(byte src[], ByteBuffer dest, int length)
  {
    while (true) {
      try {
        dest.put(src, 0, length);
        break;
      } catch (BufferOverflowException e) {
        ByteBuffer newBuf = ByteBuffer.allocate(dest.limit() + length);
        System.arraycopy(dest.array(), 0, newBuf.array(), 0, dest.position());
        newBuf.limit(newBuf.capacity()).position(dest.position());
        dest = newBuf;
      }
    }
    return dest;
  }
 
  /**
   * Appends a byte buffer to an existing bytebuffer, reallocating to a larger buffer if necessary.
   * @param src Source ByteBuffer
   * @param dest Destination ByteBuffer
   * @return The new ByteBuffer, if one was created. Otherwise the original dest ByteBuffer.
   */
  // An new ByteBuffer will be allocated if the destination is over capacity
  // The new or old destination is returned.
  public static ByteBuffer putByteBuffer(ByteBuffer src, ByteBuffer dest)
  {
    while (true) {
      try {
        dest.put(src);
        break;
      } catch (BufferOverflowException e) {
        ByteBuffer newBuf = ByteBuffer.allocate(
            dest.capacity() + ((src.limit() > PMDefines.BUFSIZE) ? src.limit() : PMDefines.BUFSIZE));
        System.arraycopy(dest.array(), 0, newBuf.array(), 0, dest.position());
        newBuf.limit(newBuf.capacity()).position(dest.position());
        dest = newBuf;
      }
    }
    return dest;
  }

  /**
   * Add incoming bytes to the packet.
   * @param input buffer
   * @return true if a complete packet has been read. false otherwise.
   * @throws Exception on Encoding error.
   */
  public boolean addInputBytes(ByteBuffer input) throws Exception
  {
    int pos = input.position();
    input.position(pos);
    while (input.hasRemaining()) {
      byte b = input.get();
      switch (pstate) {
      case PS_Init:
        if (b == ';') {
          pstate = pktState.PS_Start;
        }
        break;
      case PS_EndField:
      case PS_Start:
        if (b == ';') {
          if (pstate == pktState.PS_EndField) {
            pstate = pktState.PS_End;
            // Log packet
            {
              int totalLen = 0;
              String outStr = "";
              ByteBuffer buf;
              if (logger != null) {
                for (int i = 0; i < args.size(); i ++) {
                  buf = args.get(i);
                  totalLen += buf.remaining();
                  try {
                    outStr += ";" + convertStr(buf); // For debugging
                  } catch (Exception e) {
                    outStr += "convert exception: " + e.getMessage();
                  }
                }
                logger.debug(String.format("Packet with %d Fields ;%d Bytes", args.size(), totalLen));
                logger.debug(outStr + ";;");
              }
            }
            return true;
          } else {
            break;
          }
        }
        currentBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
        initBuf(currentBuf);
        if (b == '\\') {
          pstate = pktState.PS_Escape;
        } else {
          if (Character.isDigit(b)) {
            pstate = pktState.PS_Length;
          } else {
            pstate = pktState.PS_Data;
          }
          currentBuf = putByte(b, currentBuf);
        }
        break;
      case PS_Data:
        if (b == '\\') {
          pstate = pktState.PS_Escape;
        } else if (b == ';') {
          currentBuf.rewind();
          args.add(currentBuf);
          currentBuf = null;
          pstate = pktState.PS_EndField;
        } else {
          putByte(b, currentBuf);
        }
        break;
      case PS_Escape:
        putByte(b, currentBuf);
        pstate = pktState.PS_Data;
        break;
      case PS_Length:
        if (b == ';') {
          try {
            currentBuf.rewind();
            length = Integer.decode(_decoder.decode(currentBuf).toString());
            currentBuf = null;
            if (length <= 0) {
              pstate = pktState.PS_EndField;
              length = -1;
            } else {
              pstate = pktState.PS_StartBinary;
            }
          } catch (CharacterCodingException e) {
            pstate = pktState.PS_Error;
            throw e;
          }
        } else {
          currentBuf = putByte(b, currentBuf);
        }
        break;
      case PS_StartBinary:
        currentBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
        initBuf(currentBuf);
        currentBuf = putByte(b, currentBuf);
        length--;
        pstate = pktState.PS_Binary;
        break;
      case PS_Binary:
        currentBuf = putByte(b, currentBuf);
        length--;
        if (length <= 0) {
          pstate = pktState.PS_Data;
          length = -1;
        }
        break;
      case PS_End:
        // Don't do anything - we're done,
        // Caller should not call us.
        Exception e = new Exception("Packet is already completed");
        throw e;
        // break;
      case PS_Error:
        // Scan for 2 semicolons in a row
        if (b == ';') {
          pstate = pktState.PS_Error2;
        }
        break;
      case PS_Error2:
        if (b == ';') {
          return true;
        } else {
          pstate = pktState.PS_Error;
        }
      } // switch
    } // while (hasRemaining()
    return false;
  }

  /**
   * Convert a bytebuffer containing environment variable definitions.
   * @param envBuf Input buffer
   * @return String array with individual environment variable/values.
   */
  public static String[] parseEnvBuf(ByteBuffer envBuf)
  {
    String[] envArray = null;
    int linecount = 0;
    int limit;
    int bufIndex;
    int arrayIndex;
    // Skip leading whitespace
    for (bufIndex = envBuf.position(); Character.isWhitespace(envBuf.get(bufIndex)); bufIndex++) {
    }
    envBuf.position(bufIndex);
    // Trim trailing whitespace
    for (bufIndex = envBuf.limit() - 1; Character.isWhitespace(envBuf.get(bufIndex)); bufIndex--) {
    }
    envBuf.limit(bufIndex + 1);
    // Loop once to count lines
    limit = envBuf.limit();
    for (bufIndex = envBuf.position(); bufIndex < limit; bufIndex++) {
      byte b = envBuf.get(bufIndex);
      if (b == '\n' || b == '\r') {
        linecount++;
        // Skip whitespace
        while (((bufIndex + 1) < limit) && Character.isWhitespace(envBuf.get(bufIndex + 1))) {
          bufIndex++;
        }
      }
    }
  
    envArray = new String[linecount + 1];
    envArray[0] = new String();
    // Loop again to extract strings
    for (bufIndex = envBuf.position(), arrayIndex = 0; bufIndex < limit; bufIndex++) {
      if (envBuf.get(bufIndex) == '\n') {
        if (arrayIndex < (linecount)) {
          arrayIndex++;
          envArray[arrayIndex] = new String();
          // Skip whitespace
          while (((bufIndex + 1) < limit) && Character.isWhitespace(envBuf.get(bufIndex + 1))) {
            bufIndex++;
          }
        }
      } else {
        envArray[arrayIndex] += (char) envBuf.get(bufIndex);
      }
    }
    for (arrayIndex = 0; arrayIndex < envArray.length; arrayIndex++) {
      envArray[arrayIndex].trim();
    }
    return envArray;
  }

  /**
   * Read an environment file.
   * @param envFile Input file
   * @return A byte buffer with the environment definitions.
   */
  public static ByteBuffer readEnvFile(String envFile) 
  {
    ByteBuffer envBuf = ByteBuffer.allocate(PMDefines.BUFSIZE);
    try {
      FileInputStream inFile = new FileInputStream(envFile);
      envBuf.clear();
      byte[] inArray = new byte[PMDefines.BUFSIZE];
      while (true) {
        int nRead = inFile.read(inArray);
        if (nRead < 0) {
          break;
        }
        envBuf = putByteArray(inArray, envBuf, nRead);
      }
      inFile.close();
      // Make sure there is an ending newline
      envBuf = putByteArray("\n".getBytes(), envBuf, 1);
      envBuf.flip();
      envBuf = cleanEnvBuf(envBuf);
    } catch (Exception e) {
      return null;
    }
    return envBuf;
  }

  /**
   * Remove comments and blank lines from environment file.
   * @param oldEnv
   * @return Cleaned contents of the environment file.
   */
  public static ByteBuffer cleanEnvBuf(ByteBuffer oldEnv)
  {
    ByteBuffer newEnv = ByteBuffer.allocate(oldEnv.limit()+1);
    newEnv.clear();
    oldEnv.mark();
    while (oldEnv.hasRemaining()) {
      // Process each line
      int length;
      length = 0;
      byte b = oldEnv.get();
      // Chew up whitespace
      while (Character.isWhitespace(b) && oldEnv.hasRemaining()) {
        b = oldEnv.get();
      }
      boolean inComment = false;
      while (oldEnv.hasRemaining() && b != '\n' && b != '\r') {
        if (b == '#') {
          inComment = true;
        } else if (!inComment) {
          length ++;
          newEnv.put(b);
        }
        b = oldEnv.get();
      }
      if (length > 0) {
        newEnv.put((byte)'\n');
      }
    }
    oldEnv.reset();
    newEnv.flip();
    return newEnv;
  }
  
  /**
   * Add a String to an existing String array.
   * @param old
   * @param newString
   * @return the new String array
   */
  public static String[] addStringArray(String old[], String newString)
  {
    String newArray[] = new String[old.length + 1];
    System.arraycopy(old, 0, newArray, 0, old.length);
    newArray[old.length] = newString; 
    return newArray;
  }
} // class PMPacket


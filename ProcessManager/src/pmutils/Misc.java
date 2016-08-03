package pmutils;
import java.util.zip.*;
import java.io.*;
import java.nio.ByteBuffer;
public class Misc
{

  // Added to support Java Service Launcher
  // The containing class must not reference log4j or any other external
  // (non java-core) classes in a static manner
  public static void confirmRunning()
  {
    System.out.println( "Service waiting 5 s to confirm running" );
    try {
          Thread.sleep(5000);
    } catch (Exception e )
    {
      e.printStackTrace();
    }
  }
  public enum procState
  {
    PS_INIT, PS_RUN, PS_SUSPEND, PS_DEAD, PS_ERROR
  }
  
  public boolean createZipDir(String dirName, String zipName)
  {
    //Create a buffer for reading the files
    byte[] buf = new byte[1024];
    File dirFile = new File(dirName);
    String [] filenames = dirFile.list();
    try {
      // Create the ZIP file
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName));

      // Compress the files
      for (int i=0; i<filenames.length; i++) {
        FileInputStream in = new FileInputStream(filenames[i]);

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(filenames[i]));

        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }

        // Complete the entry
        out.closeEntry();
        in.close();
      }

      // Complete the ZIP file
      out.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static void writeHistory(File histFile, String outString)
  {
    try {
      FileOutputStream writeStream = new FileOutputStream(histFile, true);
      ByteBuffer buf = PMPacket.convertBuf(outString + ";\n");
      writeStream.write(buf.array(), buf.position(), buf.limit() - buf.position());
      writeStream.close();
    } catch (Exception e) { }
    return;
  }

}



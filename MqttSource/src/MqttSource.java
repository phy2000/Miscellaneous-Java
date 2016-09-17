

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.*;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSource {
	static String _progname = "MqttSource";

    public static void main(String[] args) throws Exception {

        String topic        = "SENSOR/TURBINE/BLADE";
        int qos             = 2;
        String broker       = "tcp://localhost:1883";
        String clientId     = "DemoSource";
        String filename = "UMData.xlsx";
        MemoryPersistence persistence = new MemoryPersistence();

		int argId;
		LongOpt[] longopts = new LongOpt[3];
		longopts[0] = new LongOpt("excel", LongOpt.REQUIRED_ARGUMENT, null, 'x');
		longopts[1] = new LongOpt("topic", LongOpt.REQUIRED_ARGUMENT, null, 't');
		longopts[2] = new LongOpt("broker", LongOpt.NO_ARGUMENT, null, 'b');
		longopts[2] = new LongOpt("clientId", LongOpt.NO_ARGUMENT, null, 'c');

		Getopt g = new Getopt(_progname, args, "+:h:s:p:L:vV", longopts);
		g.setOpterr(false); // We'll do our own error handling

		while ((argId = g.getopt()) != -1) {
			switch (argId) {
			case 'x':
				// Host name
				filename = g.getOptarg();
				break;
			case 't':
				// Session ID
				topic = g.getOptarg();
				break;
			case 'b':
				// Port number
				broker = g.getOptarg();
				break;
			case 'c':
				clientId = g.getOptarg();;
				break;
			case ':':
				// Missing option arg
				Usage.print(_progname);
				throw new Exception("Missing argument");
				//break;
			case '?':
				// Unknown option
				Usage.print(_progname);
				throw new Exception("Unknown option");
				//break;
			default:
				System.err.println("Unexpected getopt error: <" + argId + ">");
				System.exit(1);
				break;
			} // switch (argId)
		} // while (argId)            

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to MQTT broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            /*
             * Read rows from Excel and send via UM as JSON formatted messages.
             */            
            try {
                FileInputStream file = new FileInputStream(new File(filename));
                XSSFWorkbook wb = new XSSFWorkbook(file);
                XSSFSheet sheet = wb.getSheetAt(0);
                XSSFRow row;
                XSSFCell cell;

                int rows; // No of rows
                rows = sheet.getPhysicalNumberOfRows();

                int cols = 0; // No of columns
                int tmp = 0;

                // This trick ensures that we get the data properly even if it doesn't start from first few rows
                for(int i = 0; i < 10 || i < rows; i++) {
                    row = sheet.getRow(i);
                    if(row != null) {
                        tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                        if(tmp > cols) cols = tmp;
                    }
                }
                String[] header = new String[cols];
                String message;
                boolean skip = false;

                for(int r = 0; r < rows; r++) {
                    row = sheet.getRow(r);
                    if(row != null) {
                    	message = "{ ";
                        for(int c = 0; c < cols; c++) {
                            cell = row.getCell(c);
                            if(cell != null) {
    							// Your code here
                            	if (header[c] == null) {
                            		header[c] = cell.toString();
                            		skip = true;
                            	}
                            	else {
                            		if (c > 0)
                            			message = message + ", ";
                            		switch (cell.getCellType())
                            		{
                            		case Cell.CELL_TYPE_FORMULA:

                            			if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_STRING)
                                			message = message + "\"" + header[c] + "\": \"" + cell.getRichStringCellValue().getString() + "\"";
                            			else if (cell.getCachedFormulaResultType() == Cell.CELL_TYPE_NUMERIC)
                                			message = message + "\"" + header[c] + "\": \"" + (int)cell.getNumericCellValue() + "\""; // hack
                            			break;
                            		case Cell.CELL_TYPE_NUMERIC:

                                        if (DateUtil.isCellDateFormatted(cell))
                                        	message = message + "\"" + header[c] + "\": \"" + cell.getDateCellValue() + "\"";
                                       	else if ((header[c]).matches("ID"))
                                       		message = message + "\"" + header[c] + "\": \"" + (int)cell.getNumericCellValue() + "\""; // hack
                                       	else
                                       		message = message + "\"" + header[c] + "\": " + cell.getNumericCellValue();
                                    	break;
                            		case Cell.CELL_TYPE_STRING:

                            			message = message + "\"" + header[c] + "\": \"" + cell.getRichStringCellValue().getString() + "\"";
                            			break;
                            		case Cell.CELL_TYPE_BOOLEAN:
                            			message = message + "\"" + header[c] + "\": \"" + cell.getBooleanCellValue() + "\"";
                            			break;
                            		case Cell.CELL_TYPE_ERROR:
                                    case Cell.CELL_TYPE_BLANK:
                            			message = message + "\"" + header[c] + "\": \"" + cell + "\"";
                            			break;
                            		}
                            	}
                            }
                        }
                        if (skip == true)
                        	skip = false;
                        else {
                        	message = message + " }";

//                            System.out.println("Publishing message: "+message);
                            MqttMessage payload = new MqttMessage(message.getBytes());
                            System.out.println("Published message: "+payload.toString());
                            payload.setQos(qos);
                            sampleClient.publish(topic, payload);
                            Thread.sleep(100);
                        }
                    }
                }
                System.out.println("Finished");                
            } catch(Exception ioe) {
                ioe.printStackTrace();
            }
            
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }  /* main */
}  /* class MinSrc */

class Usage
{
  final static String message = 
		  "Usage: %s [-x|--excel=<excelfile> -b|--broker=<broker uri> -t|--topic=<topic> -c|clientId=<clientID>\n"
      + "\t<excelfile> is the path to and XLS or XLSX file (UMData.xlsx)\n"
      + "\t<broker> is the URI to the MQTT broker (tcp://localhost:1883)\n"
      + "\t<topic> is the MQTT topic name (SENSOR/TURBINE/BLADE)\n"
      + "\t<clientId> is the unique MQTT client ID (DemoSource)\n"
      + "\n"
      + "";
  static void print(String progname)
  {
    System.err.printf(String.format(Usage.message, progname, progname, progname));
    System.exit(1);
  }
}

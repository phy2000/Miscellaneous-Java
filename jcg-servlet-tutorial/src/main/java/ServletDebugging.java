import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletDebugging extends HttpServlet {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

@Override
      protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
     
      // parameter "name"  
      String strpm = request.getParameter("name");
  
      ServletContext c = getServletContext( );

        if (strpm == null || strpm.equals(""))
             c.log("No message received:", new IllegalStateException("Sorry, the parameter is missing."));
       else
            c.log("Here is the visitor's message: " +strpm);
     }
}

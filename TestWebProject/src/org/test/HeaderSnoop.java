/**
 * 
 */
package org.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * @author pyoung
 *
 */
@SuppressWarnings("serial")
public class HeaderSnoop extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();
		Enumeration<String> names = req.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String val = req.getHeader(name);
			if (val != null) {
				out.println(name + ":" + val);
			}
		}
	}
}

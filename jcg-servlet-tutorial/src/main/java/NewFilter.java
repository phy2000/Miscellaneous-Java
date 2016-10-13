import java.io.*;
import javax.servlet.*;

public class NewFilter implements Filter {

	public void init(FilterConfig filterConfig) {
		// init parameter
		String value = filterConfig.getInitParameter("newParam");

		// Displaying init parameter value
		System.out.println("The Parameter value: " + value);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		// The IP address of client machine.
		String remoteAddress = request.getRemoteAddr();

		// It will return the remote address
		System.out.println("Remote Internet Protocl Address: " + remoteAddress);

		chain.doFilter(request, response);
	}

	public void destroy() {

	}
}

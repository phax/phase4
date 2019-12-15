package com.helger.phase4.peppol.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.xservlet.AbstractXServlet;

/**
 * AS4 receiving servlet.<br>
 * Use a configuration like the following in your <code>WEB-INF/web.xm</code>
 * file:
 *
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;Phase4PeppolServlet&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.helger.phase4.peppol.servlet.Phase4PeppolServlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;Phase4PeppolServlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Philip Helger
 */
public class Phase4PeppolServlet extends AbstractXServlet
{
  public Phase4PeppolServlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);
    final AS4XServletHandler aHdl = new AS4XServletHandler ();
    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST, aHdl);
  }
}

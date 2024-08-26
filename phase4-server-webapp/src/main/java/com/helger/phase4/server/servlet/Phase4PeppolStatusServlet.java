package com.helger.phase4.server.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

import jakarta.servlet.annotation.WebServlet;

/**
 * The servlet to show the application status.
 *
 * @author Philip Helger
 */
@WebServlet (name = "peppol-status", urlPatterns = "/peppol-status")
public class Phase4PeppolStatusServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "peppol-status";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  public Phase4PeppolStatusServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new Phase4PeppolStatusXServletHandler ());
    handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}

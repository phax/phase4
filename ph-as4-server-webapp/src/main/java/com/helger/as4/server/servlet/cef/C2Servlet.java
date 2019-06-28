package com.helger.as4.server.servlet.cef;

import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.servlet.AS4XServletHandler;
import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

@WebServlet (value = "/c2")
public class C2Servlet extends AbstractXServlet
{
  private static final Logger LOGGER = LoggerFactory.getLogger (C2Servlet.class);

  public C2Servlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);

    final AS4XServletHandler aServletHandler = new AS4XServletHandler ();
    aServletHandler.setHandlerCustomizer ( (req, resp, hdl) -> {
      LOGGER.info ("Got /c2 request: " + req.getURL ());
    });
    handlerRegistry ().registerHandler (EHttpMethod.POST, aServletHandler);
  }
}

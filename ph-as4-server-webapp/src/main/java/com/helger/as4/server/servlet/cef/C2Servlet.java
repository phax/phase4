package com.helger.as4.server.servlet.cef;

import javax.servlet.annotation.WebServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.servlet.AS4XServletHandler;
import com.helger.as4.servlet.IAS4MessageState;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.xservlet.AbstractXServlet;

@WebServlet (value = "/c2")
public class C2Servlet extends AbstractXServlet
{
  private static final Logger LOGGER = LoggerFactory.getLogger (C2Servlet.class);

  private static final class C2MessageProcessor implements IAS4ServletMessageProcessorSPI
  {
    public AS4MessageProcessorResult processAS4UserMessage (final HttpHeaderMap aHttpHeaders,
                                                            final Ebms3UserMessage aUserMessage,
                                                            final IPMode aPMode,
                                                            final Node aPayload,
                                                            final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                            final IAS4MessageState aState)
    {
      LOGGER.info ("Invoking /c2 processAS4UserMessage");
      return AS4MessageProcessorResult.createSuccess ();
    }

    public AS4SignalMessageProcessorResult processAS4SignalMessage (final HttpHeaderMap aHttpHeaders,
                                                                    final Ebms3SignalMessage aSignalMessage,
                                                                    final IPMode aPMode,
                                                                    final IAS4MessageState aState)
    {
      LOGGER.info ("Invoking /c2 processAS4SignalMessage");
      return AS4SignalMessageProcessorResult.createSuccess ();
    }
  }

  public C2Servlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);

    final AS4XServletHandler aServletHandler = new AS4XServletHandler ();
    aServletHandler.setHandlerCustomizer ( (req, resp, hdl) -> {
      LOGGER.info ("Got /c2 request: " + req.getURL ());
      // Do not use the SPI registered handler
      hdl.setProcessorSupplier ( () -> new CommonsArrayList <> (new C2MessageProcessor ()));
    });
    handlerRegistry ().registerHandler (EHttpMethod.POST, aServletHandler);
  }
}

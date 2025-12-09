package com.helger.phase4.client;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.http.EHttpMethod;
import com.helger.mime.IMimeType;
import com.helger.mime.parse.MimeTypeParser;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;

/**
 * Special servlet only used from {@link AS4ClientUserMessageTestWithCrappyReceiver}
 *
 * @author Philip Helger
 */
public class MockAS4Servlet extends AbstractXServlet
{
  public MockAS4Servlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);
    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST,
                                        (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                         @NonNull final UnifiedResponse aUnifiedResponse) -> {
                                          final int nResponseCode = aRequestScope.params ()
                                                                                 .getAsInt ("statuscode", 200);
                                          final String sResponseContent = aRequestScope.params ()
                                                                                       .getAsString ("content",
                                                                                                     "Plain Text");
                                          final IMimeType aMimeType = MimeTypeParser.parseMimeType (aRequestScope.params ()
                                                                                                                 .getAsString ("mimetype",
                                                                                                                               "text/plain"));
                                          aUnifiedResponse.disableCaching ()
                                                          .setStatus (nResponseCode)
                                                          .setContentAndCharset (sResponseContent,
                                                                                 StandardCharsets.UTF_8)
                                                          .setMimeType (aMimeType);
                                        });
  }
}

/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.client;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.http.EHttpMethod;
import com.helger.io.file.SimpleFileIO;
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
                                          String sResponseContent = aRequestScope.params ()
                                                                                 .getAsString ("content", "Plain Text");
                                          final String sContentID = aRequestScope.params ().getAsString ("contentid");
                                          final IMimeType aMimeType = MimeTypeParser.parseMimeType (aRequestScope.params ()
                                                                                                                 .getAsString ("mimetype",
                                                                                                                               "text/plain"));

                                          // Hack to get long data
                                          if ("receipt12".equals (sContentID))
                                            sResponseContent = SimpleFileIO.getFileAsString (new File ("src/test/resources/testfiles/TestReceipt12.xml"),
                                                                                             StandardCharsets.UTF_8);

                                          aUnifiedResponse.disableCaching ()
                                                          .setStatus (nResponseCode)
                                                          .setContentAndCharset (sResponseContent,
                                                                                 StandardCharsets.UTF_8)
                                                          .setMimeType (aMimeType);
                                        });
  }
}

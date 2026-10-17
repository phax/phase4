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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertEquals;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.helger.phase4.incoming.soap.CAS4Soap;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.message.AbstractUserMessageTestSetUp;

/**
 * Test that the HTTP status code of a {@link com.helger.phase4.util.Phase4IncomingException} is
 * used by the AS4 servlet (SOAP 1.2 Part 2, Table 20).
 *
 * @author Philip Helger
 */
public final class AS4ServletStatusCodeTest extends AbstractUserMessageTestSetUp
{
  private static int _post (final String sContentType, final String sBody) throws Exception
  {
    final HttpPost aPost = new HttpPost (MockJettySetup.getServerAddressFromSettings ());
    aPost.setEntity (new StringEntity (sBody, ContentType.parse (sContentType)));
    try (final CloseableHttpClient aClient = HttpClients.createDefault ())
    {
      return aClient.execute (aPost, aResponse -> Integer.valueOf (aResponse.getCode ())).intValue ();
    }
  }

  @Test
  public void testMissingSoapHeaderIsMustUnderstandHttp500 () throws Exception
  {
    // A SOAP envelope without a Header cannot be processed -> MustUnderstand -> 500
    final String sEnvelope = "<env:Envelope xmlns:env=\"" +
                             ESoapVersion.SOAP_12.getNamespaceURI () +
                             "\"><env:Body/></env:Envelope>";
    assertEquals (CAS4Soap.HTTP_STATUS_CODE_MUST_UNDERSTAND,
                  _post (ESoapVersion.SOAP_12.getMimeType ().getAsString (), sEnvelope));
  }
}

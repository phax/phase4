/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.httpclient.HttpMimeMessageEntity;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.message.MessageHelperMethods;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4server.AbstractClientSetUp;
import com.helger.as4server.constants.AS4ServerTestHelper;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The test classes for the usermessage, are split up for a better overview.
 * Since alle these classes need the same setup and a helpermethod, this class
 * was created. Also with the help of Parameterized.class, each test will be
 * done for both SOAP Versions.
 *
 * @author bayerlma
 */
public abstract class AbstractUserMessageSetUp extends AbstractClientSetUp
{
  protected void sendMimeMessage (@Nonnull final HttpMimeMessageEntity aHttpEntity,
                                  final boolean bSuccess,
                                  @Nullable final String sErrorCode) throws IOException, MessagingException
  {
    MessageHelperMethods.moveMIMEHeadersToHTTPHeader (aHttpEntity.getMimeMessage (), m_aPost);
    sendPlainMessage (aHttpEntity, bSuccess, sErrorCode);
  }

  /**
   * @param aHttpEntity
   *        the entity to send to the server
   * @param bExpectSuccess
   *        specifies if the test case expects a positive or negative response
   *        from the server
   * @param sStatusCode
   *        if you expect a negative response, you must give the expected error
   *        code as it will get searched for in the response.
   * @throws IOException
   */
  protected void sendPlainMessage (@Nonnull final HttpEntity aHttpEntity,
                                   final boolean bExpectSuccess,
                                   @Nullable final String sErrorCode) throws IOException
  {
    m_aPost.setEntity (aHttpEntity);

    try
    {
      final CloseableHttpResponse aHttpResponse = m_aClient.execute (m_aPost);

      m_nStatusCode = aHttpResponse.getStatusLine ().getStatusCode ();
      final HttpEntity aEntity = aHttpResponse.getEntity ();
      m_sResponse = aEntity == null ? "" : EntityUtils.toString (aEntity);

      if (bExpectSuccess)
      {
        assertTrue ("Server responded with an error.\nResponse: " + m_sResponse, !m_sResponse.contains ("Error"));
        assertTrue ("Server responded with an error code (" +
                    m_nStatusCode +
                    ").",
                    m_nStatusCode == HttpServletResponse.SC_OK || m_nStatusCode == HttpServletResponse.SC_NO_CONTENT);
      }
      else
      {
        if (sErrorCode.equals ("500"))
        {
          // Expecting Internal Servlet error
          assertEquals ("Server responded with internal servlet error", 500, m_nStatusCode);
        }
        else
        {
          // Status code may by 20x but may be an error anyway
          assertTrue ("Server responded with success or different error message but failure was expected." +
                      "StatusCode: " +
                      m_nStatusCode +
                      "\nResponse: " +
                      m_sResponse,
                      m_sResponse.contains (sErrorCode));
        }
      }
    }
    catch (final HttpHostConnectException ex)
    {
      // No such server running
      fail ("No target AS4 server reachable: " + ex.getMessage () + " \n Check your properties!");
    }
  }

  /**
   * Modify the standard user message to try special cases or provoke failure
   * messages.
   *
   * @param sAnotherOrWrongPModeID
   * @param sAnotherOrWrongPartyIdInitiator
   * @param sAnotherOrWrongPartyIdResponder
   * @param aEbms3MessageProperties
   *        Default should be with _defaultProperties(), only if you do not want
   *        them change this
   * @return
   * @throws Exception
   */
  @Nonnull
  protected Document _modifyUserMessage (@Nullable final String sAnotherOrWrongPModeID,
                                         @Nullable final String sAnotherOrWrongPartyIdInitiator,
                                         @Nullable final String sAnotherOrWrongPartyIdResponder,
                                         @Nullable final Ebms3MessageProperties aEbms3MessageProperties) throws Exception
  {
    // If argument is set replace the default one
    final String sSetPartyIDInitiator = sAnotherOrWrongPartyIdInitiator == null ? AS4ServerTestHelper.DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdInitiator;
    final String sSetPartyIDResponder = sAnotherOrWrongPartyIdResponder == null ? AS4ServerTestHelper.DEFAULT_PARTY_ID
                                                                                : sAnotherOrWrongPartyIdResponder;

    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo (CAS4.LIB_NAME);
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      sAnotherOrWrongPModeID,
                                                                                                      AS4ServerTestHelper.DEFAULT_AGREEMENT);
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo (AS4ServerTestHelper.DEFAULT_INITIATOR_ROLE,
                                                                              sSetPartyIDInitiator,
                                                                              AS4ServerTestHelper.DEFAULT_RESPONDER_ROLE,
                                                                              sSetPartyIDResponder);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                ESOAPVersion.AS4_DEFAULT)
                                            .setMustUnderstand (true);

    return aDoc.getAsSOAPDocument (aPayload);
  }

  @Nonnull
  protected static Predicate <IPMode> _getFirstPModeWithID (@Nonnull final String sID)
  {
    return p -> p.getConfigID ().equals (sID);
  }

  protected Ebms3MessageProperties _defaultProperties ()
  {
    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = AS4ServerTestHelper.getEBMSProperties ();
    final Ebms3MessageProperties aEbms3MessageProperties = new Ebms3MessageProperties ();
    aEbms3MessageProperties.setProperty (aEbms3Properties);
    return aEbms3MessageProperties;
  }
}

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
package com.helger.phase4.hredelivery.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.io.stream.HasInputStream;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.CMimeType;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3Service;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.spi.AS4MessageProcessorResult;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Test class for the attachment count check of {@link Phase4HREDeliveryServletMessageProcessorSPI}.
 *
 * @author Philip Helger
 */
public final class Phase4HREDeliveryServletMessageProcessorSPIFuncTest
{
  private static final String MESSAGE_ID = "test@phase4";
  private static final String COUNT_DETAIL = "exactly one payload attachment";
  private static final String READ_DETAIL = "Failed to decompress the payload";

  @NonNull
  private static IAS4IncomingMessageState _createState ()
  {
    return (IAS4IncomingMessageState) Proxy.newProxyInstance (IAS4IncomingMessageState.class.getClassLoader (),
                                                              new Class <?> [] { IAS4IncomingMessageState.class },
                                                              (aProxy, aMethod, aArgs) -> switch (aMethod.getName ())
                                                              {
                                                                case "isSoapDecrypted" -> Boolean.FALSE;
                                                                case "isSoapSignatureChecked" -> Boolean.TRUE;
                                                                case "getLocale" -> Locale.ROOT;
                                                                case "getMessageID" -> MESSAGE_ID;
                                                                default -> null;
                                                              });
  }

  @NonNull
  private static Ebms3UserMessage _createUserMessage ()
  {
    final Ebms3MessageInfo aMessageInfo = new Ebms3MessageInfo ();
    aMessageInfo.setMessageId (MESSAGE_ID);
    final Ebms3Service aService = new Ebms3Service ();
    aService.setValue ("service");
    final Ebms3CollaborationInfo aCollaborationInfo = new Ebms3CollaborationInfo ();
    aCollaborationInfo.setService (aService);
    aCollaborationInfo.setAction ("action");
    final Ebms3UserMessage ret = new Ebms3UserMessage ();
    ret.setMessageInfo (aMessageInfo);
    ret.setCollaborationInfo (aCollaborationInfo);
    return ret;
  }

  @NonNull
  private static AS4ErrorList _process (final int nAttachments) throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
      for (int i = 0; i < nAttachments; ++i)
      {
        final WSS4JAttachment aAttachment = new WSS4JAttachment (aResHelper, CMimeType.APPLICATION_XML.getAsString ());
        // Reading the attachment fails, which shows if it was tried at all
        aAttachment.setSourceStreamProvider (HasInputStream.multiple ( () -> {
          throw new AS4DecompressException (new IOException ("Attachment was read"));
        }));
        aAttachments.add (aAttachment);
      }

      final Phase4HREDeliveryServletMessageProcessorSPI aSPI = new Phase4HREDeliveryServletMessageProcessorSPI ();
      aSPI.setReceiverCheckData (Phase4HREDeliveryReceiverConfiguration.builder (Phase4HREDeliveryDefaultReceiverConfiguration.getAsReceiverCheckData ())
                                                                  .receiverCheckEnabled (false)
                                                                  .checkSigningCertificateRevocation (false)
                                                                  .build ());
      final AS4ErrorList ret = new AS4ErrorList ();
      final AS4MessageProcessorResult aResult = aSPI.processAS4UserMessage (null,
                                                                            new HttpHeaderMap (),
                                                                            _createUserMessage (),
                                                                            null,
                                                                            null,
                                                                            aAttachments,
                                                                            _createState (),
                                                                            ret);
      assertTrue (aResult.isFailure ());
      return ret;
    }
  }

  @Test
  public void testNoAttachmentIsRejected () throws Exception
  {
    final AS4ErrorList aErrors = _process (0);
    assertEquals (1, aErrors.size ());
    assertTrue (aErrors.toString ().contains (COUNT_DETAIL));
  }

  @Test
  public void testSingleAttachmentPassesTheCountCheck () throws Exception
  {
    final String sErrors = _process (1).toString ();
    assertFalse (sErrors.contains (COUNT_DETAIL));
    assertTrue (sErrors.contains (READ_DETAIL));
  }

  @Test
  public void testTwoAttachmentsAreRejectedWithoutReadingThem () throws Exception
  {
    final AS4ErrorList aErrors = _process (2);
    assertEquals (1, aErrors.size ());
    assertTrue (aErrors.toString ().contains (COUNT_DETAIL));
    assertFalse (aErrors.toString ().contains (READ_DETAIL));
  }
}

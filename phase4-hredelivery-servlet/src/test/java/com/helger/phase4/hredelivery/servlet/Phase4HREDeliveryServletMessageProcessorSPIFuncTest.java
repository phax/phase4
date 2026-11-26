/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
import java.security.cert.X509Certificate;
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
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.security.revocation.ERevocationCheckMode;

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
  /** Self-signed test CA certificate, valid from 2026-01-01 for 100 years */
  private static final String TEST_CERT_PEM = "MIIDRzCCAi+gAwIBAgIJAK76lVYRCJweMA0GCSqGSIb3DQEBCwUAMEAxCzAJBgNV" +
                                              "BAYTAkFUMQ8wDQYDVQQKEwZwaGFzZTQxIDAeBgNVBAMTF3BoYXNlNCBzZXJ2bGV0" +
                                              "IGZ1bmN0ZXN0MCAXDTI2MDEwMTA2MTY0M1oYDzIxMjUxMjA4MDYxNjQzWjBAMQsw" +
                                              "CQYDVQQGEwJBVDEPMA0GA1UEChMGcGhhc2U0MSAwHgYDVQQDExdwaGFzZTQgc2Vy" +
                                              "dmxldCBmdW5jdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALCJ" +
                                              "x8RD4fC8moGOkN8DOLj8sFj2soOK9w627zhADf/ot7nHjUIcgZoH+C2I0ryiPIsu" +
                                              "05WThwKA+UTobiZaEAtXk0ewvP8vjJcXvRTx3FCQl5aI9NFdGJqzl10/sjoL9674" +
                                              "hxRgMelq4t+TchYqMbbbUIlqHcKlb9B+OIVUUcgNlxOoe6PaDqi03bs7BPqGJE6Q" +
                                              "l3JQHr7dCKTXoJI9njdaz95qAqAU0G990bFtogL+QsRvPjBwGz94lkxLv7U+jL+w" +
                                              "+gODs5KvEGS//3blf2KRVu5eCoY5zrk3wMOOwkdED0VjiBHpkcgntH4A0jYpkTnN" +
                                              "G2HG+x4ZAXpj3S4y5tsCAwEAAaNCMEAwHQYDVR0OBBYEFJN65agv7jwKG4lAocKh" +
                                              "vfy0fE0GMA4GA1UdDwEB/wQEAwIChDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3" +
                                              "DQEBCwUAA4IBAQCt+h+u8l9b2WjeNyysOVXcG8KJnZ8mMWnQtMBDlUW4Rd3gAKtV" +
                                              "2VJwWtndT5opDvQeTsinrEDzM/GLdfo0TSWkqG6cV8PA4rwfWrt2SFnlvuXqDLlG" +
                                              "b5HBzPDBNkUb56aBo8ET3vBzSvsSKtxapb6yIJq7Xeotzx9XcRJ5gh/Bd0EK8ud7" +
                                              "JMoPerRuh4IzD1/VSlcbot3jkrV/IiKij/Ex3D4P5hOcASPLLarxpedsy2jhu/WL" +
                                              "sK5ZqaN40lNdqelCouRRsgBQDKyF/MKlWIluqnwNBsof3phYOLdL1378UWiZRATT" +
                                              "CFHrUeXK5i8nfjlP7T+U5mpzPtKIXslrCk6Z";
  private static final X509Certificate TEST_CERT = new CertificateDecodeHelper ().source (TEST_CERT_PEM)
                                                   .pemEncoded (true)
                                                   .getDecodedOrNull ();

  @NonNull
  private static IAS4IncomingMessageState _createState ()
  {
    return (IAS4IncomingMessageState) Proxy.newProxyInstance (IAS4IncomingMessageState.class.getClassLoader (),
                                                              new Class <?> [] { IAS4IncomingMessageState.class },
                                                              (aProxy, aMethod, aArgs) -> switch (aMethod.getName ())
                                                              {
                                                                case "getSigningCertificate" -> TEST_CERT;
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
                                                                  .apCAChecker (TrustedCAChecker.builder ().trustedCACertificates (TEST_CERT).build ())
                                                                  .apRevocationCheckMode (ERevocationCheckMode.NONE)
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

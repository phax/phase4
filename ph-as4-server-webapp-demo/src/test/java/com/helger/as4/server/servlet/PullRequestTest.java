/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.servlet;

import static org.junit.Assert.assertTrue;

import org.apache.http.HttpEntity;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.http.HttpXMLEntity;
import com.helger.as4.messaging.domain.CreatePullRequestMessage;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.mpc.MPC;
import com.helger.as4.server.spi.MockMessageProcessorSPI;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.xml.serialize.read.DOMReader;

public final class PullRequestTest extends AbstractUserMessageTestSetUpExt
{
  @Test
  public void sendPullRequestSuccess () throws Exception
  {
    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                                                                             null)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    final String sResponse = sendPlainMessage (aEntity, true, null);

    assertTrue (sResponse.contains ("UserMessage"));
  }

  @Test
  public void sendPullRequestEmpty () throws Exception
  {
    // Special MPC name handled in MockMessageProcessorSPI
    final String sFailure = MockMessageProcessorSPI.MPC_EMPTY;
    final MPC aMPC = new MPC (sFailure);
    if (MetaAS4Manager.getMPCMgr ().getMPCOfID (sFailure) == null)
      MetaAS4Manager.getMPCMgr ().createMPC (aMPC);

    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             sFailure,
                                                                             null)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_EMPTY_MESSAGE_PARTITION_CHANNEL.getErrorCode ());
  }

  @Test
  public void sendPullRequestFailure () throws Exception
  {
    // Special MPC name handled in MockMessageProcessorSPI
    final String sFailure = MockMessageProcessorSPI.MPC_FAILURE;
    final MPC aMPC = new MPC (sFailure);
    if (MetaAS4Manager.getMPCMgr ().getMPCOfID (sFailure) == null)
      MetaAS4Manager.getMPCMgr ().createMPC (aMPC);

    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             sFailure,
                                                                             null)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_OTHER.getErrorCode ());
  }

  @Test
  public void sendPullRequestTwoSPIsFailure () throws Exception
  {
    final String sMPC = "TWO-SPI";
    final MPC aMPC = new MPC (sMPC);
    if (MetaAS4Manager.getMPCMgr ().getMPCOfID (sMPC) == null)
      MetaAS4Manager.getMPCMgr ().createMPC (aMPC);

    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             sMPC,
                                                                             null)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_INCONSISTENT.getErrorCode ());
  }

  @Test
  public void sendPullRequestSuccessTwoWayPushPull () throws Exception
  {
    // Depending on the payload a different EMEPBinding get chosen by
    // @MockPullRequestProcessorSPI
    // To Test the pull request part of the EMEPBinding
    final Document aPayload = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/PushPull.xml"));
    final ICommonsList <Object> aAny = new CommonsArrayList <> ();
    aAny.add (aPayload.getDocumentElement ());

    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                                                                             aAny)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    final String sResponse = sendPlainMessage (aEntity, true, null);

    assertTrue (sResponse.contains ("UserMessage"));
  }

  @Test
  public void sendPullRequestSuccessTwoWayPullPush () throws Exception
  {
    // Depending on the payload a different EMEPBinding get chosen by
    // @MockPullRequestProcessorSPI
    // To Test the pull request part of the EMEPBinding
    final Document aPayload = DOMReader.readXMLDOM (new ClassPathResource ("testfiles/PullPush.xml"));
    final ICommonsList <Object> aAny = new CommonsArrayList <> ();
    aAny.add (aPayload.getDocumentElement ());

    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC",
                                                                             aAny)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    final String sResponse = sendPlainMessage (aEntity, true, null);

    assertTrue (sResponse.contains ("UserMessage"));
  }

  @Test
  public void sendPullRequestWithNoMPC () throws Exception
  {
    final Document aDoc = CreatePullRequestMessage.createPullRequestMessage (ESOAPVersion.AS4_DEFAULT,
                                                                             MessageHelperMethods.createEbms3MessageInfo (),
                                                                             null,
                                                                             null)
                                                  .getAsSOAPDocument ();
    final HttpEntity aEntity = new HttpXMLEntity (aDoc);
    sendPlainMessage (aEntity, false, EEbmsError.EBMS_VALUE_NOT_RECOGNIZED.getErrorCode ());
  }
}

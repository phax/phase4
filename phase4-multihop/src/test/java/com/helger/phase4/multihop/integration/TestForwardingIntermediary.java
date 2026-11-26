/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.integration;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.phase4.CAS4;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.xml.serialize.read.DOMReader;

/**
 * A <b>test only</b> ebMS intermediary (I-Cloud edge). It is deliberately minimal and is NOT an
 * implementation of the ebMS3 Part 2 intermediary conformance clause - that is explicitly out of
 * scope for this module.
 * <p>
 * The one property that matters for the test is <b>byte transparency</b>: the intermediary reads
 * the DOM only to find out where a message has to go, and then forwards the <b>identical</b>
 * bytes. It never re-serializes XML, because that would break the XML signature.
 * </p>
 *
 * @author Philip Helger
 */
public final class TestForwardingIntermediary
{
  /** One message as it travels through the I-Cloud */
  public static final class StoredMessage
  {
    private final byte [] m_aBytes;
    private final String m_sContentType;

    StoredMessage (final byte @NonNull [] aBytes, @NonNull final String sContentType)
    {
      m_aBytes = aBytes;
      m_sContentType = sContentType;
    }

    public byte @NonNull [] getBytes ()
    {
      return m_aBytes;
    }

    @NonNull
    public String getContentType ()
    {
      return m_sContentType;
    }
  }

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (TestForwardingIntermediary.class);

  private final ICommonsMap <String, MultiHopTestEndpoint> m_aEndpointsByPartyID = new CommonsHashMap <> ();
  private final ICommonsMap <String, ICommonsList <StoredMessage>> m_aMPCQueues = new CommonsHashMap <> ();
  private final ICommonsList <StoredMessage> m_aForwardedMessages = new CommonsArrayList <> ();

  public TestForwardingIntermediary ()
  {}

  /**
   * Register the endpoint that is reachable under the provided party ID.
   *
   * @param sPartyID
   *        The To/PartyId value routed to this endpoint.
   * @param aEndpoint
   *        The endpoint.
   */
  public void registerEndpoint (@NonNull final String sPartyID, @NonNull final MultiHopTestEndpoint aEndpoint)
  {
    m_aEndpointsByPartyID.put (sPartyID, aEndpoint);
  }

  /**
   * @return All messages that were forwarded so far, in order. Never <code>null</code>.
   */
  @NonNull
  public ICommonsList <StoredMessage> getAllForwardedMessages ()
  {
    return m_aForwardedMessages.getClone ();
  }

  /**
   * @param sMPC
   *        The MPC to look at.
   * @return All messages currently queued on that MPC. Never <code>null</code>.
   */
  @NonNull
  public ICommonsList <StoredMessage> getQueue (@NonNull final String sMPC)
  {
    return m_aMPCQueues.computeIfAbsent (sMPC, k -> new CommonsArrayList <> ());
  }

  @Nullable
  private static Element _getFirstElement (@NonNull final Document aDoc,
                                           @NonNull final String sNamespace,
                                           @NonNull final String sLocalName)
  {
    final NodeList aList = aDoc.getElementsByTagNameNS (sNamespace, sLocalName);
    return aList.getLength () == 0 ? null : (Element) aList.item (0);
  }

  /**
   * Extract the destination party ID, read-only. R4 / ebMS3 Part 2 section 2.5.5 - for a User
   * Message it is in eb:Messaging, for a routed signal in ebint:RoutingInput.
   *
   * @param aDoc
   *        The parsed message. Never modified.
   * @return <code>null</code> if no destination could be determined.
   */
  @Nullable
  private static String _extractToPartyID (@NonNull final Document aDoc)
  {
    // A routed signal carries the reversed User Message in the RoutingInput
    Element aScope = _getFirstElement (aDoc, CAS4MultiHop.EBINT_NS, "RoutingInput");
    if (aScope == null)
      aScope = aDoc.getDocumentElement ();

    final NodeList aTos = aScope.getElementsByTagNameNS (CAS4.EBMS_NS, "To");
    if (aTos.getLength () == 0)
      return null;

    final NodeList aPartyIDs = ((Element) aTos.item (0)).getElementsByTagNameNS (CAS4.EBMS_NS, "PartyId");
    return aPartyIDs.getLength () == 0 ? null : aPartyIDs.item (0).getTextContent ();
  }

  /**
   * Extract the MPC of the routed signal, so it can be queued for pulling.
   *
   * @param aDoc
   *        The parsed message. Never modified.
   * @return <code>null</code> if the message carries no RoutingInput MPC.
   */
  @Nullable
  private static String _extractRoutingInputMPC (@NonNull final Document aDoc)
  {
    final Element aRI = _getFirstElement (aDoc, CAS4MultiHop.EBINT_NS, "RoutingInput");
    if (aRI == null)
      return null;

    final NodeList aUMs = aRI.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "UserMessage");
    if (aUMs.getLength () == 0)
      return null;

    final String sMPC = ((Element) aUMs.item (0)).getAttribute ("mpc");
    return sMPC.isEmpty () ? null : sMPC;
  }

  /**
   * Push a message into the I-Cloud and forward it synchronously to the destination endpoint.
   *
   * @param aBytes
   *        The raw message bytes. Forwarded unchanged.
   * @param sContentType
   *        The Content-Type. Forwarded unchanged.
   * @return The synchronous response of the destination endpoint. Never <code>null</code>.
   * @throws Exception
   *         on error
   */
  public MultiHopTestEndpoint.@NonNull Response pushSync (final byte @NonNull [] aBytes,
                                                 @NonNull final String sContentType) throws Exception
  {
    m_aForwardedMessages.add (new StoredMessage (aBytes, sContentType));

    final Document aDoc = DOMReader.readXMLDOM (aBytes);
    if (aDoc == null)
      throw new IllegalStateException ("The I-Cloud received something that is not XML");

    final String sToPartyID = _extractToPartyID (aDoc);
    final MultiHopTestEndpoint aTarget = sToPartyID == null ? null : m_aEndpointsByPartyID.get (sToPartyID);
    if (aTarget == null)
      throw new IllegalStateException ("The I-Cloud cannot route to the party '" + sToPartyID + "'");

    LOGGER.info ("I-Cloud forwards " + aBytes.length + " bytes to '" + aTarget.getName () + "'");

    // Byte transparent - the very same array and Content-Type
    return aTarget.receive (aBytes, sContentType);
  }

  /**
   * Queue a message on the MPC of its RoutingInput, so that it can be pulled later.
   *
   * @param aBytes
   *        The raw message bytes.
   * @param sContentType
   *        The Content-Type.
   * @return The MPC the message was queued on, or <code>null</code> if it carries no RoutingInput.
   */
  @Nullable
  public String queueForPull (final byte @NonNull [] aBytes, @NonNull final String sContentType)
  {
    final Document aDoc = DOMReader.readXMLDOM (aBytes);
    if (aDoc == null)
      return null;

    final String sMPC = _extractRoutingInputMPC (aDoc);
    if (sMPC == null)
      return null;

    getQueue (sMPC).add (new StoredMessage (aBytes, sContentType));
    LOGGER.info ("I-Cloud queued " + aBytes.length + " bytes on the MPC '" + sMPC + "'");
    return sMPC;
  }

  /**
   * Take the oldest message of the provided MPC.
   *
   * @param sMPC
   *        The MPC to pull from.
   * @return <code>null</code> if the MPC is empty.
   */
  @Nullable
  public StoredMessage pull (@NonNull final String sMPC)
  {
    final ICommonsList <StoredMessage> aQueue = m_aMPCQueues.get (sMPC);
    return aQueue == null || aQueue.isEmpty () ? null : aQueue.remove (0);
  }

  /**
   * @return All MPCs that currently hold at least one message. Never <code>null</code>.
   */
  @NonNull
  public ICommonsList <String> getAllNonEmptyMPCs ()
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final Map.Entry <String, ICommonsList <StoredMessage>> aEntry : m_aMPCQueues.entrySet ())
      if (aEntry.getValue ().isNotEmpty ())
        ret.add (aEntry.getKey ());
    return ret;
  }
}

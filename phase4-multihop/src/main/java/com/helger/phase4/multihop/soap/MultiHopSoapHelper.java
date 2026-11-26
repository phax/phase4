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
package com.helger.phase4.multihop.soap;

import java.util.Map;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.model.MultiHopRoutingInput;
import com.helger.phase4.multihop.model.MultiHopRoutingInputMarshaller;
import com.helger.xml.XMLHelper;
import com.helger.xsds.wsaddr.CWSAddr;

/**
 * Helper methods for the SOAP level handling of multi-hop messages.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class MultiHopSoapHelper
{
  private MultiHopSoapHelper ()
  {}

  /**
   * @param eSoapVersion
   *        The SOAP version. May not be <code>null</code>.
   * @return The QName of the attribute that carries the SOAP role (1.2) or actor (1.1). Never
   *         <code>null</code>.
   */
  @NonNull
  public static QName getRoleOrActorQName (@NonNull final ESoapVersion eSoapVersion)
  {
    switch (eSoapVersion)
    {
      case SOAP_11:
        return new QName (eSoapVersion.getNamespaceURI (), "actor");
      case SOAP_12:
        return new QName (eSoapVersion.getNamespaceURI (), "role");
      default:
        throw new IllegalStateException ("Unsupported SOAP version " + eSoapVersion);
    }
  }

  /**
   * Check if the provided Ebms3 Messaging header is targeted to the next intermediary MSH. This is
   * the only detection mechanism on the responding side - AS4 Profile section 4.2 requires it to be
   * driven by the incoming message, not by configuration (D6).
   *
   * @param aMessaging
   *        The Ebms3 Messaging object of the incoming message. May be <code>null</code>.
   * @param eSoapVersion
   *        The SOAP version of the incoming message. May not be <code>null</code>.
   * @return <code>true</code> if the role (SOAP 1.2) or actor (SOAP 1.1) attribute equals the
   *         <code>nextmsh</code> URI.
   */
  public static boolean isTargetedToNextMSH (@Nullable final Ebms3Messaging aMessaging,
                                             @NonNull final ESoapVersion eSoapVersion)
  {
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");

    if (aMessaging == null)
      return false;

    final Map <QName, String> aOther = aMessaging.getOtherAttributes ();
    if (aOther == null)
      return false;

    final String sValue = aOther.get (getRoleOrActorQName (eSoapVersion));
    return sValue != null && CAS4MultiHop.NEXT_MSH_ROLE.equals (sValue.trim ());
  }

  /**
   * Set the <code>nextmsh</code> role (SOAP 1.2) or actor (SOAP 1.1) attribute on the provided
   * Ebms3 Messaging object. R1.
   *
   * @param aMessaging
   *        The Ebms3 Messaging object to modify. May not be <code>null</code>.
   * @param eSoapVersion
   *        The SOAP version to be used. May not be <code>null</code>.
   */
  public static void setNextMSHRole (@NonNull final Ebms3Messaging aMessaging,
                                     @NonNull final ESoapVersion eSoapVersion)
  {
    ValueEnforcer.notNull (aMessaging, "Messaging");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");

    aMessaging.getOtherAttributes ().put (getRoleOrActorQName (eSoapVersion), CAS4MultiHop.NEXT_MSH_ROLE);
  }

  @NonNull
  private static Element _getSoapHeader (@NonNull final Document aDoc, @NonNull final ESoapVersion eSoapVersion)
  {
    final Element aHeader = XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                                  eSoapVersion.getNamespaceURI (),
                                                                  eSoapVersion.getHeaderElementName ());
    if (aHeader == null)
      throw new IllegalStateException ("The provided SOAP document contains no " +
                                       eSoapVersion.getHeaderElementName () +
                                       " element");
    return aHeader;
  }

  @NonNull
  private static String _appendWsaElement (@NonNull final Document aDoc,
                                           @NonNull final Element aHeader,
                                           @NonNull final String sLocalName,
                                           @NonNull final String sValue,
                                           final boolean bWithRole,
                                           @NonNull final ESoapVersion eSoapVersion)
  {
    final Element aElement = aDoc.createElementNS (CWSAddr.NAMESPACE_URI,
                                                   CWSAddr.DEFAULT_PREFIX + ":" + sLocalName);
    aElement.setTextContent (sValue);

    /*
     * Declare every namespace that is used by a prefixed attribute of this element explicitly on
     * the element itself. Without that the prefix has no declaration in scope, the serializer has
     * to invent one, and the canonical form after serialization no longer matches the one the
     * signature was computed over - the signature then fails to verify at the receiver.
     */
    aElement.setAttributeNS (XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                             XMLConstants.XMLNS_ATTRIBUTE + ":" + CWSAddr.DEFAULT_PREFIX,
                             CWSAddr.NAMESPACE_URI);
    aElement.setAttributeNS (XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                             XMLConstants.XMLNS_ATTRIBUTE + ":wsu",
                             CAS4.WSU_NS);

    // R8 - wsa:To carries the role/actor, wsa:Action does not.
    // Neither carries mustUnderstand.
    if (bWithRole)
    {
      final QName aRoleQName = getRoleOrActorQName (eSoapVersion);
      aElement.setAttributeNS (XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                               XMLConstants.XMLNS_ATTRIBUTE + ":" + eSoapVersion.getNamespacePrefix (),
                               eSoapVersion.getNamespaceURI ());
      aElement.setAttributeNS (aRoleQName.getNamespaceURI (),
                               eSoapVersion.getNamespacePrefix () + ":" + aRoleQName.getLocalPart (),
                               CAS4MultiHop.NEXT_MSH_ROLE);
    }

    // D3 - signing references use wsu:Id
    final String sID = CAS4MultiHop.createID (UUID.randomUUID ().toString ());
    aElement.setAttributeNS (CAS4.WSU_NS, "wsu:Id", sID);

    aHeader.appendChild (aElement);
    return sID;
  }

  /**
   * Append the three SOAP header elements that a routed Receipt or Error must carry - R4, R7 and
   * R8:
   * <ol>
   * <li><code>wsa:To</code> with the I-Cloud URI and the <code>nextmsh</code> role</li>
   * <li><code>wsa:Action</code> with the provided one-way action, without a role</li>
   * <li><code>ebint:RoutingInput</code> as a reference parameter</li>
   * </ol>
   *
   * @param aDoc
   *        The response SOAP document to modify. May not be <code>null</code>.
   * @param eSoapVersion
   *        The SOAP version of the response. May not be <code>null</code>.
   * @param sWsaAction
   *        The <code>wsa:Action</code> value. Either
   *        {@link CAS4MultiHop#WSA_ACTION_ONEWAY_RECEIPT} or
   *        {@link CAS4MultiHop#WSA_ACTION_ONEWAY_ERROR}. May neither be <code>null</code> nor
   *        empty.
   * @param aRoutingInput
   *        The routing input to add. May not be <code>null</code>. Its standard attributes are set
   *        by this method.
   * @return The list of all <code>wsu:Id</code> values that were created, in document order. Use
   *         them to have the added elements covered by the signature (R9). Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> addResponseAddressingHeaders (@NonNull final Document aDoc,
                                                                    @NonNull final ESoapVersion eSoapVersion,
                                                                    @NonNull @Nonempty final String sWsaAction,
                                                                    @NonNull final MultiHopRoutingInput aRoutingInput)
  {
    ValueEnforcer.notNull (aDoc, "Doc");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notEmpty (sWsaAction, "WsaAction");
    ValueEnforcer.notNull (aRoutingInput, "RoutingInput");

    final Element aHeader = _getSoapHeader (aDoc, eSoapVersion);
    final ICommonsList <String> ret = new CommonsArrayList <> ();

    // 1. wsa:To - with role, no mustUnderstand (R8)
    ret.add (_appendWsaElement (aDoc, aHeader, "To", CAS4MultiHop.ICLOUD_URI, true, eSoapVersion));

    // 2. wsa:Action - no role, no mustUnderstand (R8)
    ret.add (_appendWsaElement (aDoc, aHeader, "Action", sWsaAction, false, eSoapVersion));

    // 3. ebint:RoutingInput (R7)
    final String sRoutingInputID = CAS4MultiHop.createID (UUID.randomUUID ().toString ());
    aRoutingInput.setStandardAttributes (eSoapVersion, sRoutingInputID);

    final Element aRIElement = new MultiHopRoutingInputMarshaller ().getAsElement (aRoutingInput);
    if (aRIElement == null)
      throw new IllegalStateException ("Failed to marshal the multi-hop RoutingInput");

    aHeader.appendChild (aDoc.importNode (aRIElement, true));
    ret.add (sRoutingInputID);

    return ret;
  }
}

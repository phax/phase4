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
package com.helger.phase4.multihop.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.string.StringHelper;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.multihop.CAS4MultiHop;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * The <code>ebint:RoutingInput</code> SOAP header element. See ebMS3 Part 2 section 2.5.5,
 * appendix C and the multi-hop XSD.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement (name = "RoutingInput", namespace = CAS4MultiHop.EBINT_NS)
@XmlType (name = "RoutingInput", namespace = CAS4MultiHop.EBINT_NS, propOrder = { "userMessage" })
public class MultiHopRoutingInput
{
  @XmlElement (name = "UserMessage", namespace = CAS4MultiHop.EBINT_NS, required = true)
  private MultiHopRoutingUserMessage userMessage;

  @XmlAnyAttribute
  private Map <QName, String> otherAttributes = new HashMap <> ();

  @Nullable
  public MultiHopRoutingUserMessage getUserMessage ()
  {
    return userMessage;
  }

  public void setUserMessage (@Nullable final MultiHopRoutingUserMessage a)
  {
    userMessage = a;
  }

  /**
   * @return The mutable map of all other attributes. Never <code>null</code>. This is where
   *         <code>wsa:IsReferenceParameter</code>, the SOAP role/actor, mustUnderstand and
   *         <code>wsu:Id</code> live.
   */
  @NonNull
  @ReturnsMutableObject
  public Map <QName, String> getOtherAttributes ()
  {
    return otherAttributes;
  }

  /**
   * Set all attributes that ebMS3 Part 2 section 2.5.5 and appendix C require on the RoutingInput
   * header element, for the provided SOAP version.
   *
   * @param eSoapVersion
   *        The SOAP version of the message. May not be <code>null</code>.
   * @param sWsuID
   *        The <code>wsu:Id</code> to set, so that the element can be referenced from the
   *        signature. May be <code>null</code> to not set it.
   * @return this for chaining
   */
  @NonNull
  public MultiHopRoutingInput setStandardAttributes (@NonNull final ESoapVersion eSoapVersion,
                                                     @Nullable final String sWsuID)
  {
    otherAttributes.put (CAS4MultiHop.QNAME_WSA_IS_REFERENCE_PARAMETER, "true");

    switch (eSoapVersion)
    {
      case SOAP_11:
        otherAttributes.put (new QName (eSoapVersion.getNamespaceURI (), "actor"), CAS4MultiHop.NEXT_MSH_ROLE);
        otherAttributes.put (new QName (eSoapVersion.getNamespaceURI (), "mustUnderstand"),
                             eSoapVersion.getMustUnderstandValue (true));
        break;
      case SOAP_12:
        otherAttributes.put (new QName (eSoapVersion.getNamespaceURI (), "role"), CAS4MultiHop.NEXT_MSH_ROLE);
        otherAttributes.put (new QName (eSoapVersion.getNamespaceURI (), "mustUnderstand"),
                             eSoapVersion.getMustUnderstandValue (true));
        break;
      default:
        throw new IllegalStateException ("Unsupported SOAP version " + eSoapVersion);
    }

    if (StringHelper.isNotEmpty (sWsuID))
      otherAttributes.put (CAS4MultiHop.QNAME_WSU_ID, sWsuID);

    return this;
  }
}

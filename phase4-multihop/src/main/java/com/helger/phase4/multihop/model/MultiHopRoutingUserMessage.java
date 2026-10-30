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

import org.jspecify.annotations.Nullable;

import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3MessageProperties;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.ebms3header.Ebms3PayloadInfo;
import com.helger.phase4.multihop.CAS4MultiHop;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * The <code>ebint:UserMessage</code> child of <code>ebint:RoutingInput</code>. See ebMS3 Part 2
 * section 2.5.5 and the multi-hop XSD.<br>
 * The children deliberately reuse the generated <code>Ebms3*</code> types of phase4-lib, so that no
 * duplicate set of ebMS3 core classes is created.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@XmlAccessorType (XmlAccessType.FIELD)
@XmlType (name = "RoutingInputUserMessage",
          namespace = CAS4MultiHop.EBINT_NS,
          propOrder = { "messageInfo", "partyInfo", "collaborationInfo", "messageProperties", "payloadInfo" })
public class MultiHopRoutingUserMessage
{
  @XmlElement (name = "MessageInfo", namespace = CAS4.EBMS_NS)
  private Ebms3MessageInfo messageInfo;

  @XmlElement (name = "PartyInfo", namespace = CAS4.EBMS_NS, required = true)
  private Ebms3PartyInfo partyInfo;

  @XmlElement (name = "CollaborationInfo", namespace = CAS4.EBMS_NS, required = true)
  private Ebms3CollaborationInfo collaborationInfo;

  @XmlElement (name = "MessageProperties", namespace = CAS4.EBMS_NS)
  private Ebms3MessageProperties messageProperties;

  @XmlElement (name = "PayloadInfo", namespace = CAS4.EBMS_NS)
  private Ebms3PayloadInfo payloadInfo;

  @XmlAttribute (name = "mpc")
  private String mpc;

  @Nullable
  public Ebms3MessageInfo getMessageInfo ()
  {
    return messageInfo;
  }

  public void setMessageInfo (@Nullable final Ebms3MessageInfo a)
  {
    messageInfo = a;
  }

  @Nullable
  public Ebms3PartyInfo getPartyInfo ()
  {
    return partyInfo;
  }

  public void setPartyInfo (@Nullable final Ebms3PartyInfo a)
  {
    partyInfo = a;
  }

  @Nullable
  public Ebms3CollaborationInfo getCollaborationInfo ()
  {
    return collaborationInfo;
  }

  public void setCollaborationInfo (@Nullable final Ebms3CollaborationInfo a)
  {
    collaborationInfo = a;
  }

  @Nullable
  public Ebms3MessageProperties getMessageProperties ()
  {
    return messageProperties;
  }

  public void setMessageProperties (@Nullable final Ebms3MessageProperties a)
  {
    messageProperties = a;
  }

  @Nullable
  public Ebms3PayloadInfo getPayloadInfo ()
  {
    return payloadInfo;
  }

  public void setPayloadInfo (@Nullable final Ebms3PayloadInfo a)
  {
    payloadInfo = a;
  }

  @Nullable
  public String getMpc ()
  {
    return mpc;
  }

  public void setMpc (@Nullable final String s)
  {
    mpc = s;
  }
}

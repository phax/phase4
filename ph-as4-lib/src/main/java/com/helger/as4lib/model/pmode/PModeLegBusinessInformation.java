/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;

/**
 * Business information - This set of parameters only applies to user messages.
 *
 * @author Philip Helger
 */
public class PModeLegBusinessInformation
{
  /**
   * Name of the service to which the User message is intended to be delivered.
   * Its content should map to the element
   * <code>eb:Messaging/eb:UserMessage/eb:CollaborationInfo/eb:Service</code>.
   */
  private String m_sService;

  /**
   * Name of the action the User message is intended to invoke. Its content
   * should map to the element
   * <code>eb:Messaging/eb:UserMessage/eb:CollaborationInfo/eb:Action</code>.
   */
  private String m_sAction;

  /**
   * The value of this parameter is a list of properties.
   */
  private ICommonsOrderedMap <String, PModeProperty> m_aProperties = new CommonsLinkedHashMap<> ();

  /**
   * This parameter allows for specifying some constraint or profile on the
   * payload. It specifies a list of payload parts.
   */
  private ICommonsOrderedMap <String, PModePayloadProfile> m_aPayloadProfile = new CommonsLinkedHashMap<> ();

  /**
   * This parameter allows for specifying a maximum size in kilobytes for the
   * entire payload, i.e. for the total of all payload parts.
   */
  private Integer m_nPayloadProfileMaxKB;

  /**
   * The value of this parameter is the identifier of the MPC (Message Partition
   * Channel) to which the message is assigned. It maps to the attribute
   * <code>eb:Messaging/eb:UserMessage/@mpc</code>.
   */
  private String m_sMPCID;

  public PModeLegBusinessInformation (final String sService,
                                      final String sAction,
                                      final Integer nPayloadProfileMaxKB,
                                      final String sMPCID)
  {
    m_sService = sService;
    m_sAction = sAction;
    m_nPayloadProfileMaxKB = nPayloadProfileMaxKB;
    m_sMPCID = sMPCID;
  }

  public PModeLegBusinessInformation (final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfile,
                                      final ICommonsOrderedMap <String, PModeProperty> aProperties,
                                      final Integer nPayloadProfileMaxKB,
                                      final String sAction,
                                      final String sMPCID,
                                      final String sService)
  {
    m_sService = sService;
    m_sAction = sAction;
    m_aProperties = aProperties;
    m_aPayloadProfile = aPayloadProfile;
    m_nPayloadProfileMaxKB = nPayloadProfileMaxKB;
    m_sMPCID = sMPCID;
  }

  public PModeLegBusinessInformation ()
  {}

  @Nullable
  public String getService ()
  {
    return m_sService;
  }

  public void setService (@Nullable final String sService)
  {
    m_sService = sService;
  }

  @Nullable
  public String getAction ()
  {
    return m_sAction;
  }

  public void setAction (@Nullable final String sAction)
  {
    m_sAction = sAction;
  }

  public ICommonsOrderedMap <String, PModeProperty> getProperties ()
  {
    return m_aProperties;
  }

  public void setProperties (final ICommonsOrderedMap <String, PModeProperty> aProperties)
  {
    m_aProperties = aProperties;
  }

  public ICommonsOrderedMap <String, PModePayloadProfile> getPayloadProfile ()
  {
    return m_aPayloadProfile;
  }

  public void setPayloadProfile (final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfile)
  {
    m_aPayloadProfile = aPayloadProfile;
  }

  public Integer getPayloadProfileMaxKB ()
  {
    return m_nPayloadProfileMaxKB;
  }

  public void setPayloadProfileMaxKB (final Integer nPayloadProfileMaxKB)
  {
    m_nPayloadProfileMaxKB = nPayloadProfileMaxKB;
  }

  public String getMPCID ()
  {
    return m_sMPCID;
  }

  public void setMPCID (final String sMPCID)
  {
    m_sMPCID = sMPCID;
  }

  public void addProperty (@Nonnull final PModeProperty aProperty)
  {
    ValueEnforcer.notNull (aProperty, "Property");
    final String sKey = aProperty.getName ();
    if (m_aProperties.containsKey (sKey))
      throw new IllegalArgumentException ("A property with the name '" + sKey + "' is already registered!");
    m_aProperties.put (sKey, aProperty);
  }

  public void addPayloadProfile (@Nonnull final PModePayloadProfile aPayloadProfile)
  {
    ValueEnforcer.notNull (aPayloadProfile, "PayloadProfile");
    final String sKey = aPayloadProfile.getName ();
    if (m_aPayloadProfile.containsKey (sKey))
      throw new IllegalArgumentException ("A payload profile with the name '" + sKey + "' is already registered!");
    m_aPayloadProfile.put (sKey, aPayloadProfile);
  }
}

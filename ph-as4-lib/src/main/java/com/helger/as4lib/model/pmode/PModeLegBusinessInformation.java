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

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;

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
  private final ICommonsOrderedMap <String, PModeProperty> m_aProperties = new CommonsLinkedHashMap<> ();

  /**
   * This parameter allows for specifying some constraint or profile on the
   * payload. It specifies a list of payload parts.
   */
  private final ICommonsOrderedMap <String, PModePayloadProfile> m_aPayloadProfiles = new CommonsLinkedHashMap<> ();

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

  public PModeLegBusinessInformation (@Nullable final String sService,
                                      @Nullable final String sAction,
                                      @Nullable final Integer nPayloadProfileMaxKB,
                                      @Nullable final String sMPCID)
  {
    this (sService, sAction, null, null, nPayloadProfileMaxKB, sMPCID);
  }

  PModeLegBusinessInformation (@Nullable final String sService,
                               @Nullable final String sAction,
                               @Nullable final ICommonsOrderedMap <String, PModeProperty> aProperties,
                               @Nullable final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles,
                               @Nullable final Integer nPayloadProfileMaxKB,
                               @Nullable final String sMPCID)
  {
    setService (sService);
    setAction (sAction);
    setAllProperties (aProperties);
    setAllPayloadProfiles (aPayloadProfiles);
    setPayloadProfileMaxKB (nPayloadProfileMaxKB);
    setMPCID (sMPCID);
  }

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

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, PModeProperty> getAllProperties ()
  {
    return m_aProperties.getClone ();
  }

  public void forAllProperties (@Nonnull final Consumer <? super PModeProperty> aConsumer)
  {
    m_aProperties.forEachValue (aConsumer);
  }

  public void setAllProperties (@Nullable final ICommonsOrderedMap <String, PModeProperty> aProperties)
  {
    m_aProperties.setAll (aProperties);
  }

  public void addProperty (@Nonnull final PModeProperty aProperty)
  {
    ValueEnforcer.notNull (aProperty, "Property");
    final String sKey = aProperty.getName ();
    if (m_aProperties.containsKey (sKey))
      throw new IllegalArgumentException ("A property with the name '" + sKey + "' is already registered!");
    m_aProperties.put (sKey, aProperty);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, PModePayloadProfile> getAllPayloadProfiles ()
  {
    return m_aPayloadProfiles.getClone ();
  }

  public void forAllPayloadProfiles (@Nonnull final Consumer <? super PModePayloadProfile> aConsumer)
  {
    m_aPayloadProfiles.forEachValue (aConsumer);
  }

  public void setAllPayloadProfiles (@Nullable final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles)
  {
    m_aPayloadProfiles.setAll (aPayloadProfiles);
  }

  public void addPayloadProfile (@Nonnull final PModePayloadProfile aPayloadProfile)
  {
    ValueEnforcer.notNull (aPayloadProfile, "PayloadProfile");
    final String sKey = aPayloadProfile.getName ();
    if (m_aPayloadProfiles.containsKey (sKey))
      throw new IllegalArgumentException ("A payload profile with the name '" + sKey + "' is already registered!");
    m_aPayloadProfiles.put (sKey, aPayloadProfile);
  }

  @Nullable
  public Integer getPayloadProfileMaxKB ()
  {
    return m_nPayloadProfileMaxKB;
  }

  public void setPayloadProfileMaxKB (final Integer nPayloadProfileMaxKB)
  {
    m_nPayloadProfileMaxKB = nPayloadProfileMaxKB;
  }

  @Nullable
  public String getMPCID ()
  {
    return m_sMPCID;
  }

  public void setMPCID (@Nullable final String sMPCID)
  {
    m_sMPCID = sMPCID;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLegBusinessInformation rhs = (PModeLegBusinessInformation) o;
    return m_aPayloadProfiles.equals (rhs.m_aPayloadProfiles) &&
           EqualsHelper.equals (m_aProperties, rhs.m_aProperties) &&
           EqualsHelper.equals (m_nPayloadProfileMaxKB, rhs.m_nPayloadProfileMaxKB) &&
           EqualsHelper.equals (m_sAction, rhs.m_sAction) &&
           EqualsHelper.equals (m_sMPCID, rhs.m_sMPCID) &&
           EqualsHelper.equals (m_sService, rhs.m_sService);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aPayloadProfiles)
                                       .append (m_aProperties)
                                       .append (m_nPayloadProfileMaxKB)
                                       .append (m_sAction)
                                       .append (m_sMPCID)
                                       .append (m_sService)
                                       .getHashCode ();
  }
}

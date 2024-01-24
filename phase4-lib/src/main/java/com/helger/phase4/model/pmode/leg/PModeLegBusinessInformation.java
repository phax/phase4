/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Business information - This set of parameters only applies to user messages.
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeLegBusinessInformation implements Serializable
{
  /**
   * Name of the service to which the User message is intended to be delivered.
   * Its content should map to the element
   * <code>eb:Messaging/eb:UserMessage/eb:CollaborationInfo/eb:Service</code>.
   */
  private String m_sServiceValue;

  /**
   * <code>type</code> attribute of
   * <code>eb:Messaging/eb:UserMessage/eb:CollaborationInfo/eb:Service</code>.
   */
  private String m_sServiceType;

  /**
   * Name of the action the User message is intended to invoke. Its content
   * should map to the element
   * <code>eb:Messaging/eb:UserMessage/eb:CollaborationInfo/eb:Action</code>.
   */
  private String m_sAction;

  /**
   * The value of this parameter is a list of properties.
   */
  private final ICommonsOrderedMap <String, PModeProperty> m_aProperties = new CommonsLinkedHashMap <> ();

  /**
   * This parameter allows for specifying some constraint or profile on the
   * payload. It specifies a list of payload parts.
   */
  private final ICommonsOrderedMap <String, PModePayloadProfile> m_aPayloadProfiles = new CommonsLinkedHashMap <> ();

  /**
   * This parameter allows for specifying a maximum size in kilobytes for the
   * entire payload, i.e. for the total of all payload parts.
   */
  private Long m_aPayloadProfileMaxKB;

  /**
   * The value of this parameter is the identifier of the MPC (Message Partition
   * Channel) to which the message is assigned. It maps to the attribute
   * <code>eb:Messaging/eb:UserMessage/@mpc</code>.
   */
  private String m_sMPCID;

  public PModeLegBusinessInformation ()
  {}

  public PModeLegBusinessInformation (@Nullable final String sServiceValue,
                                      @Nullable final String sServiceType,
                                      @Nullable final String sAction,
                                      @Nullable final ICommonsOrderedMap <String, PModeProperty> aProperties,
                                      @Nullable final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles,
                                      @Nullable final Long nPayloadProfileMaxKB,
                                      @Nullable final String sMPCID)
  {
    setService (sServiceValue);
    setServiceType (sServiceType);
    setAction (sAction);
    if (aProperties != null)
      properties ().putAll (aProperties);
    if (aPayloadProfiles != null)
      payloadProfiles ().putAll (aPayloadProfiles);
    setPayloadProfileMaxKB (nPayloadProfileMaxKB);
    setMPCID (sMPCID);
  }

  @Nullable
  public final String getService ()
  {
    return m_sServiceValue;
  }

  public final boolean hasService ()
  {
    return StringHelper.hasText (m_sServiceValue);
  }

  @Nonnull
  public final EChange setService (@Nullable final String sService)
  {
    if (EqualsHelper.equals (sService, m_sServiceValue))
      return EChange.UNCHANGED;
    m_sServiceValue = sService;
    return EChange.CHANGED;
  }

  @Nullable
  public final String getServiceType ()
  {
    return m_sServiceType;
  }

  public final boolean hasServiceType ()
  {
    return StringHelper.hasText (m_sServiceType);
  }

  @Nonnull
  public final EChange setServiceType (@Nullable final String sServiceType)
  {
    if (EqualsHelper.equals (sServiceType, m_sServiceType))
      return EChange.UNCHANGED;
    m_sServiceType = sServiceType;
    return EChange.CHANGED;
  }

  @Nullable
  public final String getAction ()
  {
    return m_sAction;
  }

  public final boolean hasAction ()
  {
    return StringHelper.hasText (m_sAction);
  }

  @Nonnull
  public final EChange setAction (@Nullable final String sAction)
  {
    if (EqualsHelper.equals (sAction, m_sAction))
      return EChange.UNCHANGED;
    m_sAction = sAction;
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsOrderedMap <String, PModeProperty> properties ()
  {
    return m_aProperties;
  }

  public final void addProperty (@Nonnull final PModeProperty aProperty)
  {
    ValueEnforcer.notNull (aProperty, "Property");
    final String sKey = aProperty.getName ();
    if (m_aProperties.containsKey (sKey))
      throw new IllegalArgumentException ("A property with the name '" + sKey + "' is already registered!");
    m_aProperties.put (sKey, aProperty);
  }

  @Nonnull
  @ReturnsMutableObject
  public final ICommonsOrderedMap <String, PModePayloadProfile> payloadProfiles ()
  {
    return m_aPayloadProfiles;
  }

  public final void addPayloadProfile (@Nonnull final PModePayloadProfile aPayloadProfile)
  {
    ValueEnforcer.notNull (aPayloadProfile, "PayloadProfile");
    final String sKey = aPayloadProfile.getName ();
    if (m_aPayloadProfiles.containsKey (sKey))
      throw new IllegalArgumentException ("A payload profile with the name '" + sKey + "' is already registered!");
    m_aPayloadProfiles.put (sKey, aPayloadProfile);
  }

  @Nullable
  public final Long getPayloadProfileMaxKB ()
  {
    return m_aPayloadProfileMaxKB;
  }

  public final boolean hasPayloadProfileMaxKB ()
  {
    return m_aPayloadProfileMaxKB != null;
  }

  @Nonnull
  public final EChange setPayloadProfileMaxKB (@Nullable final Long nPayloadProfileMaxKB)
  {
    if (EqualsHelper.equals (nPayloadProfileMaxKB, m_aPayloadProfileMaxKB))
      return EChange.UNCHANGED;
    m_aPayloadProfileMaxKB = nPayloadProfileMaxKB;
    return EChange.CHANGED;
  }

  @Nonnull
  public final EChange setPayloadProfileMaxKB (final long nPayloadProfileMaxKB)
  {
    return setPayloadProfileMaxKB (Long.valueOf (nPayloadProfileMaxKB));
  }

  @Nullable
  public final String getMPCID ()
  {
    return m_sMPCID;
  }

  public final boolean hasMPCID ()
  {
    return StringHelper.hasText (m_sMPCID);
  }

  @Nonnull
  public final EChange setMPCID (@Nullable final String sMPCID)
  {
    if (EqualsHelper.equals (sMPCID, m_sMPCID))
      return EChange.UNCHANGED;
    m_sMPCID = sMPCID;
    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeLegBusinessInformation rhs = (PModeLegBusinessInformation) o;
    return EqualsHelper.equals (m_sServiceValue, rhs.m_sServiceValue) &&
           EqualsHelper.equals (m_sServiceType, rhs.m_sServiceType) &&
           EqualsHelper.equals (m_sAction, rhs.m_sAction) &&
           m_aProperties.equals (rhs.m_aProperties) &&
           m_aPayloadProfiles.equals (rhs.m_aPayloadProfiles) &&
           EqualsHelper.equals (m_aPayloadProfileMaxKB, rhs.m_aPayloadProfileMaxKB) &&
           EqualsHelper.equals (m_sMPCID, rhs.m_sMPCID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sServiceValue)
                                       .append (m_sServiceType)
                                       .append (m_sAction)
                                       .append (m_aProperties)
                                       .append (m_aPayloadProfiles)
                                       .append (m_aPayloadProfileMaxKB)
                                       .append (m_sMPCID)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).appendIfNotNull ("Service", m_sServiceValue)
                                       .appendIfNotNull ("ServiceType", m_sServiceType)
                                       .appendIfNotNull ("Action", m_sAction)
                                       .appendIf ("Properties", m_aProperties, ICommonsMap::isNotEmpty)
                                       .appendIf ("PayloadProfiles", m_aPayloadProfiles, ICommonsMap::isNotEmpty)
                                       .appendIfNotNull ("PayloadProfileMaxMB", m_aPayloadProfileMaxKB)
                                       .appendIfNotNull ("MPCID", m_sMPCID)
                                       .getToString ();
  }

  @Nonnull
  public static PModeLegBusinessInformation create (@Nullable final String sServiceValue,
                                                    @Nullable final String sAction,
                                                    @Nullable final Long nPayloadProfileMaxKB,
                                                    @Nullable final String sMPCID)
  {
    return new PModeLegBusinessInformation (sServiceValue, null, sAction, null, null, nPayloadProfileMaxKB, sMPCID);
  }
}

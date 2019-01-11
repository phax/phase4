/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.model.pmode.leg;

import java.io.Serializable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
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

  public PModeLegBusinessInformation (@Nullable final String sService,
                                      @Nullable final String sAction,
                                      @Nullable final Long nPayloadProfileMaxKB,
                                      @Nullable final String sMPCID)
  {
    this (sService, sAction, null, null, nPayloadProfileMaxKB, sMPCID);
  }

  public PModeLegBusinessInformation (@Nullable final String sService,
                                      @Nullable final String sAction,
                                      @Nullable final ICommonsOrderedMap <String, PModeProperty> aProperties,
                                      @Nullable final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles,
                                      @Nullable final Long nPayloadProfileMaxKB,
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

  public boolean hasService ()
  {
    return StringHelper.hasText (m_sService);
  }

  @Nonnull
  public final EChange setService (@Nullable final String sService)
  {
    if (EqualsHelper.equals (sService, m_sService))
      return EChange.UNCHANGED;
    m_sService = sService;
    return EChange.CHANGED;
  }

  @Nullable
  public String getAction ()
  {
    return m_sAction;
  }

  public boolean hasAction ()
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
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, PModeProperty> getAllProperties ()
  {
    return m_aProperties.getClone ();
  }

  public void forAllProperties (@Nonnull final Consumer <? super PModeProperty> aConsumer)
  {
    m_aProperties.forEachValue (aConsumer);
  }

  @Nonnull
  public final EChange setAllProperties (@Nullable final ICommonsOrderedMap <String, PModeProperty> aProperties)
  {
    // Ensure same type
    final CommonsLinkedHashMap <String, PModeProperty> aRealMap = new CommonsLinkedHashMap <> (aProperties);
    if (aRealMap.equals (m_aProperties))
      return EChange.UNCHANGED;
    m_aProperties.setAll (aProperties);
    return EChange.CHANGED;
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
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, PModePayloadProfile> getAllPayloadProfiles ()
  {
    return m_aPayloadProfiles.getClone ();
  }

  public void forAllPayloadProfiles (@Nonnull final Consumer <? super PModePayloadProfile> aConsumer)
  {
    m_aPayloadProfiles.forEachValue (aConsumer);
  }

  @Nonnull
  public final EChange setAllPayloadProfiles (@Nullable final ICommonsOrderedMap <String, PModePayloadProfile> aPayloadProfiles)
  {
    // Ensure same type
    final CommonsLinkedHashMap <String, PModePayloadProfile> aRealMap = new CommonsLinkedHashMap <> (aPayloadProfiles);
    if (aRealMap.equals (m_aPayloadProfiles))
      return EChange.UNCHANGED;
    m_aPayloadProfiles.setAll (aPayloadProfiles);
    return EChange.CHANGED;
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
  public Long getPayloadProfileMaxKB ()
  {
    return m_aPayloadProfileMaxKB;
  }

  public boolean hasPayloadProfileMaxKB ()
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

  @Nullable
  public String getMPCID ()
  {
    return m_sMPCID;
  }

  public boolean hasMPCID ()
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
    return EqualsHelper.equals (m_sService, rhs.m_sService) &&
           EqualsHelper.equals (m_sAction, rhs.m_sAction) &&
           m_aProperties.equals (rhs.m_aProperties) &&
           m_aPayloadProfiles.equals (rhs.m_aPayloadProfiles) &&
           EqualsHelper.equals (m_aPayloadProfileMaxKB, rhs.m_aPayloadProfileMaxKB) &&
           EqualsHelper.equals (m_sMPCID, rhs.m_sMPCID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sService)
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
    return new ToStringGenerator (this).appendIfNotNull ("Service", m_sService)
                                       .appendIfNotNull ("Action", m_sAction)
                                       .appendIf ("Properties", m_aProperties, ICommonsMap::isNotEmpty)
                                       .appendIf ("PayloadProfiles", m_aPayloadProfiles, ICommonsMap::isNotEmpty)
                                       .appendIfNotNull ("PayloadProfileMaxMB", m_aPayloadProfileMaxKB)
                                       .appendIfNotNull ("MPCID", m_sMPCID)
                                       .getToString ();
  }
}

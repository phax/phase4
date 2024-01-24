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
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.name.IHasName;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.IMandatoryIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * A payload part is a data structure that consists of five properties: name (or
 * Content-ID) that is the part identifier, and can be used as an index in the
 * notation PayloadProfile[]; MIME data type (<code>text/xml</code>,
 * <code>application/pdf</code>, etc.); name of the applicable XML Schema file
 * if the MIME data type is text/xml; maximum size in kilobytes; and a Boolean
 * value indicating whether the part is expected or optional, within the User
 * message. The message payload(s) must match this profile.
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModePayloadProfile implements IHasName, IMandatoryIndicator, Serializable
{
  public static final boolean DEFAULT_MANDATORY = false;

  private String m_sName;
  private IMimeType m_aMimeType;
  private String m_sXSDFilename;
  private Integer m_aMaxSizeKB;
  private EMandatory m_eMandatory;

  public PModePayloadProfile ()
  {}

  public PModePayloadProfile (@Nonnull @Nonempty final String sName,
                              @Nonnull final IMimeType aMimeType,
                              @Nullable final String sXSDFilename,
                              @Nullable final Integer aMaxSizeKB,
                              @Nonnull final EMandatory eMandatory)
  {
    setName (sName);
    setMimeType (aMimeType);
    setXSDFilename (sXSDFilename);
    setMaxSizeKB (aMaxSizeKB);
    setMandatory (eMandatory);
  }

  /**
   * The PMode payload profile name.
   */
  @Nonnull
  @Nonempty
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * Set the name.
   *
   * @param sName
   *        The new name. May neither be <code>null</code> nor empty.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setName (@Nonnull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    if (sName.equals (m_sName))
      return EChange.UNCHANGED;
    m_sName = sName;
    return EChange.CHANGED;
  }

  /**
   * @return The MIME type. Never <code>null</code>.
   */
  @Nonnull
  public final IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  /**
   * Set the MIME type.
   *
   * @param aMimeType
   *        The new MIME type. May not be <code>null</code>.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setMimeType (@Nonnull final IMimeType aMimeType)
  {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    if (aMimeType.equals (m_aMimeType))
      return EChange.UNCHANGED;
    m_aMimeType = aMimeType;
    return EChange.CHANGED;
  }

  /**
   * @return The name of the XML Schema filename to apply. May be
   *         <code>null</code>.
   */
  @Nullable
  public final String getXSDFilename ()
  {
    return m_sXSDFilename;
  }

  /**
   * @return <code>true</code> if an XML Schema filename is present,
   *         <code>false</code> if not.
   */
  public final boolean hasXSDFilename ()
  {
    return StringHelper.hasText (m_sXSDFilename);
  }

  /**
   * Set the XML Schema filename.
   *
   * @param sXSDFilename
   *        The new XML Schema filename. May be <code>null</code>.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setXSDFilename (@Nullable final String sXSDFilename)
  {
    if (EqualsHelper.equals (sXSDFilename, m_sXSDFilename))
      return EChange.UNCHANGED;
    m_sXSDFilename = sXSDFilename;
    return EChange.CHANGED;
  }

  /**
   * @return The maximum size in kilobyte or <code>null</code>.
   */
  @Nullable
  public final Integer getMaxSizeKB ()
  {
    return m_aMaxSizeKB;
  }

  /**
   * @return <code>true</code> if a maximum size in kilobyte is present,
   *         <code>false</code> if not.
   */
  public final boolean hasMaxSizeKB ()
  {
    return m_aMaxSizeKB != null;
  }

  /**
   * Set the maximum size in kilobytes.
   *
   * @param aMaxSizeKB
   *        The maximum size in kilobytes. May be <code>null</code>.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setMaxSizeKB (@Nullable final Integer aMaxSizeKB)
  {
    if (EqualsHelper.equals (aMaxSizeKB, m_aMaxSizeKB))
      return EChange.UNCHANGED;
    m_aMaxSizeKB = aMaxSizeKB;
    return EChange.CHANGED;
  }

  /**
   * Set the maximum size in kilobytes.
   *
   * @param nMaxSizeKB
   *        The maximum size in kilobytes.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setMaxSizeKB (final int nMaxSizeKB)
  {
    return setMaxSizeKB (Integer.valueOf (nMaxSizeKB));
  }

  /**
   * @return <code>true</code> if the part is mandatory, <code>false</code> if
   *         it is optional.
   */
  public final boolean isMandatory ()
  {
    return m_eMandatory.isMandatory ();
  }

  /**
   * @return <code>true</code> if the part is optional, <code>false</code> if it
   *         is mandatory.
   */
  @Override
  public final boolean isOptional ()
  {
    return m_eMandatory.isOptional ();
  }

  /**
   * Set the payload mandatory or optional.
   *
   * @param eMandatory
   *        Payload mandatory state. May not be <code>null</code>.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setMandatory (@Nonnull final EMandatory eMandatory)
  {
    ValueEnforcer.notNull (eMandatory, "Mandatory");
    if (eMandatory.equals (m_eMandatory))
      return EChange.UNCHANGED;
    m_eMandatory = eMandatory;
    return EChange.CHANGED;
  }

  /**
   * Set the payload mandatory or optional.
   *
   * @param bMandatory
   *        <code>true</code> for mandatory, <code>false</code> for optional.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setMandatory (final boolean bMandatory)
  {
    return setMandatory (EMandatory.valueOf (bMandatory));
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModePayloadProfile rhs = (PModePayloadProfile) o;
    return m_sName.equals (rhs.m_sName) &&
           m_aMimeType.equals (rhs.m_aMimeType) &&
           EqualsHelper.equals (m_sXSDFilename, rhs.m_sXSDFilename) &&
           EqualsHelper.equals (m_aMaxSizeKB, rhs.m_aMaxSizeKB) &&
           m_eMandatory.equals (rhs.m_eMandatory);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName)
                                       .append (m_aMimeType)
                                       .append (m_sXSDFilename)
                                       .append (m_aMaxSizeKB)
                                       .append (m_eMandatory)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Name", m_sName)
                                       .append ("MimeType", m_aMimeType)
                                       .append ("XSDFilename", m_sXSDFilename)
                                       .append ("MaxSizeKB", m_aMaxSizeKB)
                                       .append ("Mandatory", m_eMandatory)
                                       .getToString ();
  }
}

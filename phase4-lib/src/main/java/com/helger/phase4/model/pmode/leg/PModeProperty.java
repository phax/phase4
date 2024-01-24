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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.name.IHasName;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.IMandatoryIndicator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.text.IHasDescription;

/**
 * A property is a data structure that consists of four values: the property
 * name, which can be used as an identifier of the property (e.g. a required
 * property named "messagetype" can be noted as:
 * <code>Properties[messagetype].required="true"</code>); the property
 * description; the property data type; and a Boolean value, indicating whether
 * the property is expected or optional, within the User message. This parameter
 * controls the contents of the element
 * <code>eb:Messaging/eb:UserMessage/eb:MessageProperties.</code>
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeProperty implements IHasName, IHasDescription, IMandatoryIndicator, Serializable
{
  public static final String DATA_TYPE_STRING = "string";
  public static final boolean DEFAULT_MANDATORY = false;

  private static final Logger LOGGER = LoggerFactory.getLogger (PModeProperty.class);

  private String m_sName;
  private String m_sDescription;
  private String m_sDataType;
  private EMandatory m_eMandatory;

  private static void _checkDataType (@Nonnull final String sDataType)
  {
    if (!DATA_TYPE_STRING.equals (sDataType))
      LOGGER.warn ("A non-standard data type (everything besides '" + DATA_TYPE_STRING + "') is used: " + sDataType);
  }

  public PModeProperty ()
  {}

  public PModeProperty (@Nonnull @Nonempty final String sName,
                        @Nullable final String sDescription,
                        @Nonnull @Nonempty final String sDataType,
                        @Nonnull final EMandatory eMandatory)
  {
    setName (sName);
    setDescription (sDescription);
    setDataType (sDataType);
    setMandatory (eMandatory);
  }

  /**
   * The PMode property name.
   */
  @Nonnull
  @Nonempty
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * Set the property name.
   *
   * @param sName
   *        Property name. May neither be <code>null</code> nor empty.
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
   * The PMode property description.
   */
  @Nullable
  public final String getDescription ()
  {
    return m_sDescription;
  }

  /**
   * Set the description.
   *
   * @param sDescription
   *        The description. May be <code>null</code>.
   * @return {@link EChange}
   */
  @Nonnull
  public final EChange setDescription (@Nullable final String sDescription)
  {
    if (EqualsHelper.equals (sDescription, m_sDescription))
      return EChange.UNCHANGED;
    m_sDescription = sDescription;
    return EChange.CHANGED;
  }

  /**
   * @return The PMode property data type. May neither be <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public final String getDataType ()
  {
    return m_sDataType;
  }

  /**
   * Set the property data type.
   *
   * @param sDataType
   *        Property data type. May neither be <code>null</code> nor empty.
   * @return {@link EChange}
   * @since 0.12.0
   */
  @Nonnull
  public final EChange setDataType (@Nonnull @Nonempty final String sDataType)
  {
    ValueEnforcer.notEmpty (sDataType, "DataType");
    if (sDataType.equals (m_sDataType))
      return EChange.UNCHANGED;
    m_sDataType = sDataType;
    _checkDataType (sDataType);
    return EChange.CHANGED;
  }

  /**
   * @return <code>true</code> if the PMode property is mandatory,
   *         <code>false</code> if it is optional.
   */
  public final boolean isMandatory ()
  {
    return m_eMandatory.isMandatory ();
  }

  /**
   * @return <code>true</code> if the PMode property is optional,
   *         <code>false</code> if it is mandatory.
   */
  @Override
  public final boolean isOptional ()
  {
    return m_eMandatory.isOptional ();
  }

  /**
   * Set the property mandatory or optional.
   *
   * @param eMandatory
   *        Property mandatory state. May not be <code>null</code>.
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
   * Set the property mandatory or optional.
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
    final PModeProperty rhs = (PModeProperty) o;
    return m_sName.equals (rhs.m_sName) &&
           EqualsHelper.equals (m_sDescription, rhs.m_sDescription) &&
           m_sDataType.equals (rhs.m_sDataType) &&
           m_eMandatory.equals (rhs.m_eMandatory);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName)
                                       .append (m_sDescription)
                                       .append (m_sDataType)
                                       .append (m_eMandatory)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Name", m_sName)
                                       .append ("Description", m_sDescription)
                                       .append ("DataType", m_sDataType)
                                       .append ("Mandatory", m_eMandatory)
                                       .getToString ();
  }
}

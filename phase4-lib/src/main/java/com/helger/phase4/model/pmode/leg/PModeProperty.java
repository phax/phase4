/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.name.IHasName;
import com.helger.base.state.EChange;
import com.helger.base.state.EMandatory;
import com.helger.base.state.IMandatoryIndicator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.text.IHasDescription;

/**
 * A property is a data structure that consists of four values: the property name, which can be used
 * as an identifier of the property (e.g. a required property named "messagetype" can be noted as:
 * <code>Properties[messagetype].required="true"</code>); the property description; the property
 * data type; and a Boolean value, indicating whether the property is expected or optional, within
 * the User message. This parameter controls the contents of the element
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

  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (PModeProperty.class);

  private String m_sName;
  private String m_sDescription;
  private String m_sDataType;
  private EMandatory m_eMandatory;

  private static void _checkDataType (@NonNull final String sDataType)
  {
    if (!DATA_TYPE_STRING.equals (sDataType))
      LOGGER.warn ("A non-standard data type (everything besides '" + DATA_TYPE_STRING + "') is used: " + sDataType);
  }

  public PModeProperty ()
  {}

  public PModeProperty (@NonNull @Nonempty final String sName,
                        @Nullable final String sDescription,
                        @NonNull @Nonempty final String sDataType,
                        @NonNull final EMandatory eMandatory)
  {
    setName (sName);
    setDescription (sDescription);
    setDataType (sDataType);
    setMandatory (eMandatory);
  }

  /**
   * The PMode property name.
   */
  @NonNull
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
  @NonNull
  public final EChange setName (@NonNull @Nonempty final String sName)
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
  @NonNull
  public final EChange setDescription (@Nullable final String sDescription)
  {
    if (EqualsHelper.equals (sDescription, m_sDescription))
      return EChange.UNCHANGED;
    m_sDescription = sDescription;
    return EChange.CHANGED;
  }

  /**
   * @return The PMode property data type. May neither be <code>null</code> nor empty.
   */
  @NonNull
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
  @NonNull
  public final EChange setDataType (@NonNull @Nonempty final String sDataType)
  {
    ValueEnforcer.notEmpty (sDataType, "DataType");
    if (sDataType.equals (m_sDataType))
      return EChange.UNCHANGED;
    m_sDataType = sDataType;
    _checkDataType (sDataType);
    return EChange.CHANGED;
  }

  /**
   * @return <code>true</code> if the PMode property is mandatory, <code>false</code> if it is
   *         optional.
   */
  public final boolean isMandatory ()
  {
    return m_eMandatory.isMandatory ();
  }

  /**
   * @return <code>true</code> if the PMode property is optional, <code>false</code> if it is
   *         mandatory.
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
  @NonNull
  public final EChange setMandatory (@NonNull final EMandatory eMandatory)
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
  @NonNull
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

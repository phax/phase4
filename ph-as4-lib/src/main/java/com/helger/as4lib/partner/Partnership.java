/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.partner;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.attachment.EAS4CompressionMode;
import com.helger.as4lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.util.IStringMap;
import com.helger.as4lib.util.StringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents a single partnership. It has a unique name, a set of
 * sender and receiver specific attributes (like AS2 ID, Email and key alias)
 * and a set of generic attributes that are interpreted depending on the
 * context.
 *
 * @author Philip Helger
 */
public class Partnership implements Serializable
{
  public static final String DEFAULT_NAME = "auto-created-dummy";

  private String m_sName;
  private final StringMap m_aSenderIDs = new StringMap ();
  private final StringMap m_aReceiverIDs = new StringMap ();
  private final StringMap m_aAttributes = new StringMap ();

  public Partnership (@Nonnull final String sName)
  {
    setName (sName);
  }

  public void setName (@Nonnull final String sName)
  {
    m_sName = ValueEnforcer.notNull (sName, "Name");
  }

  /**
   * @return The partnership name. Never <code>null</code>.
   */
  @Nonnull
  public String getName ()
  {
    return m_sName;
  }

  /**
   * Set an arbitrary sender ID.
   *
   * @param sKey
   *        The name of the ID. May not be <code>null</code>.
   * @param sValue
   *        The value to be set. It may be <code>null</code> in which case the
   *        attribute is removed.
   */
  public void setSenderID (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aSenderIDs.setAttribute (sKey, sValue);
  }

  /**
   * Set the senders AS2 ID.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getSenderAS2ID()
   * @see #containsSenderAS2ID()
   */
  public void setSenderAS2ID (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_AS2, sValue);
  }

  /**
   * Set the senders X509 alias.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getSenderX509Alias()
   * @see #containsSenderX509Alias()
   */
  public void setSenderX509Alias (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_X509_ALIAS, sValue);
  }

  /**
   * Set the senders email address.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getSenderEmail()
   * @see #containsSenderEmail()
   */
  public void setSenderEmail (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_EMAIL, sValue);
  }

  /**
   * Add all sender IDs provided in the passed map. Existing sender IDs are not
   * altered.
   *
   * @param aMap
   *        The map to use. May be <code>null</code>.
   */
  public void addSenderIDs (@Nullable final Map <String, String> aMap)
  {
    m_aSenderIDs.setAttributes (aMap);
  }

  /**
   * Get the value of an arbitrary sender ID
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return The contained value if the name is not <code>null</code> and
   *         contained in the sender IDs.
   */
  @Nullable
  public String getSenderID (@Nullable final String sKey)
  {
    return m_aSenderIDs.getAttributeAsString (sKey);
  }

  /**
   * @return the sender's AS2 ID or <code>null</code> if it is not set
   * @see #setSenderAS2ID(String)
   * @see #containsSenderAS2ID()
   */
  @Nullable
  public String getSenderAS2ID ()
  {
    return getSenderID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return the sender's X509 alias or <code>null</code> if it is not set
   * @see #setSenderX509Alias(String)
   * @see #containsSenderX509Alias()
   */
  @Nullable
  public String getSenderX509Alias ()
  {
    return getSenderID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return the sender's email address or <code>null</code> if it is not set.
   * @see #setSenderEmail(String)
   * @see #containsSenderEmail()
   */
  @Nullable
  public String getSenderEmail ()
  {
    return getSenderID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * Check if an arbitrary sender ID is present.
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return <code>true</code> if the name is not <code>null</code> and
   *         contained in the sender IDs.
   */
  public boolean containsSenderID (@Nullable final String sKey)
  {
    return m_aSenderIDs.containsAttribute (sKey);
  }

  /**
   * @return <code>true</code> if the sender's AS2 ID is present,
   *         <code>false</code> otherwise.
   * @see #setSenderAS2ID(String)
   * @see #getSenderAS2ID()
   */
  public boolean containsSenderAS2ID ()
  {
    return containsSenderID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return <code>true</code> if the sender's X509 alias is present,
   *         <code>false</code> otherwise.
   * @see #setSenderX509Alias(String)
   * @see #getSenderX509Alias()
   */
  public boolean containsSenderX509Alias ()
  {
    return containsSenderID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return <code>true</code> if the sender's email address is present,
   *         <code>false</code> otherwise.
   * @see #setSenderEmail(String)
   * @see #getSenderEmail()
   */
  public boolean containsSenderEmail ()
  {
    return containsSenderID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * @return All sender IDs. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllSenderIDs ()
  {
    return m_aSenderIDs.getClone ();
  }

  /**
   * Set an arbitrary receiver ID.
   *
   * @param sKey
   *        The name of the ID. May not be <code>null</code>.
   * @param sValue
   *        The value to be set. It may be <code>null</code> in which case the
   *        attribute is removed.
   */
  public void setReceiverID (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aReceiverIDs.setAttribute (sKey, sValue);
  }

  /**
   * Set the receivers AS2 ID.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverAS2ID()
   * @see #containsReceiverAS2ID()
   */
  public void setReceiverAS2ID (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_AS2, sValue);
  }

  /**
   * Set the receivers X509 alias.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverX509Alias()
   * @see #containsReceiverX509Alias()
   */
  public void setReceiverX509Alias (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_X509_ALIAS, sValue);
  }

  /**
   * Set the receivers email address.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverEmail()
   * @see #containsReceiverEmail()
   */
  public void setReceiverEmail (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_EMAIL, sValue);
  }

  /**
   * Add all receiver IDs provided in the passed map. Existing receiver IDs are
   * not altered.
   *
   * @param aMap
   *        The map to use. May be <code>null</code>.
   */
  public void addReceiverIDs (@Nullable final Map <String, String> aMap)
  {
    m_aReceiverIDs.setAttributes (aMap);
  }

  /**
   * Get the value of an arbitrary receiver ID
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return The contained value if the name is not <code>null</code> and
   *         contained in the receiver IDs.
   */
  @Nullable
  public String getReceiverID (@Nullable final String sKey)
  {
    return m_aReceiverIDs.getAttributeAsString (sKey);
  }

  /**
   * @return the receiver's AS2 ID or <code>null</code> if it is not set
   * @see #setReceiverAS2ID(String)
   * @see #containsReceiverAS2ID()
   */
  @Nullable
  public String getReceiverAS2ID ()
  {
    return getReceiverID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return the receiver's X509 alias or <code>null</code> if it is not set
   * @see #setReceiverX509Alias(String)
   * @see #containsReceiverX509Alias()
   */
  @Nullable
  public String getReceiverX509Alias ()
  {
    return getReceiverID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return the receiver's email address or <code>null</code> if it is not set.
   * @see #setReceiverEmail(String)
   * @see #containsReceiverEmail()
   */
  @Nullable
  public String getReceiverEmail ()
  {
    return getReceiverID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * Check if an arbitrary receiver ID is present.
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return <code>true</code> if the name is not <code>null</code> and
   *         contained in the receiver IDs.
   */
  public boolean containsReceiverID (@Nullable final String sKey)
  {
    return m_aReceiverIDs.containsAttribute (sKey);
  }

  /**
   * @return <code>true</code> if the receiver's AS2 ID is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverAS2ID(String)
   * @see #getReceiverAS2ID()
   */
  public boolean containsReceiverAS2ID ()
  {
    return containsReceiverID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return <code>true</code> if the receiver's X509 alias is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverX509Alias(String)
   * @see #getReceiverX509Alias()
   */
  public boolean containsReceiverX509Alias ()
  {
    return containsReceiverID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return <code>true</code> if the receiver's email address is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverEmail(String)
   * @see #getReceiverEmail()
   */
  public boolean containsReceiverEmail ()
  {
    return containsReceiverID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * @return All receiver IDs. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllReceiverIDs ()
  {
    return m_aReceiverIDs.getClone ();
  }

  /**
   * Set an arbitrary partnership attribute.
   *
   * @param sKey
   *        The key to be used. May not be <code>null</code>.
   * @param sValue
   *        The value to be used. If <code>null</code> an existing attribute
   *        with the provided name will be removed.
   * @return {@link EChange#CHANGED} if something changed. Never
   *         <code>null</code>.
   */
  @Nonnull
  public EChange setAttribute (@Nonnull final String sKey, @Nullable final String sValue)
  {
    return m_aAttributes.setAttribute (sKey, sValue);
  }

  /**
   * Get the value associated with the given attribute name.
   *
   * @param sKey
   *        Attribute name to search. May be <code>null</code>.
   * @return <code>null</code> if the attribute name was <code>null</code> or if
   *         no such attribute is contained.
   * @see #getAttribute(String, String)
   */
  @Nullable
  public String getAttribute (@Nullable final String sKey)
  {
    return m_aAttributes.getAttributeAsString (sKey);
  }

  /**
   * Get the value associated with the given attribute name or the default
   * values.
   *
   * @param sKey
   *        Attribute name to search. May be <code>null</code>.
   * @param sDefault
   *        Default value to be returned if no such attribute is present.
   * @return The provided default value if the attribute name was
   *         <code>null</code> or if no such attribute is contained.
   * @see #getAttribute(String)
   */
  @Nullable
  public String getAttribute (@Nullable final String sKey, @Nullable final String sDefault)
  {
    return m_aAttributes.getAttributeAsString (sKey, sDefault);
  }

  @Nullable
  public String getAS2URL ()
  {
    return getAttribute (CPartnershipIDs.PA_AS2_URL);
  }

  @Nonnull
  public EChange setAS2URL (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_AS2_URL, sValue);
  }

  @Nullable
  public String getAS2MDNTo ()
  {
    return getAttribute (CPartnershipIDs.PA_AS2_MDN_TO);
  }

  @Nonnull
  public EChange setAS2MDNTo (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_AS2_MDN_TO, sValue);
  }

  @Nullable
  public String getAS2MDNOptions ()
  {
    return getAttribute (CPartnershipIDs.PA_AS2_MDN_OPTIONS);
  }

  @Nonnull
  public EChange setAS2MDNOptions (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_AS2_MDN_OPTIONS, sValue);
  }

  @Nullable
  public String getAS2ReceiptOption ()
  {
    return getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION);
  }

  @Nonnull
  public EChange setAS2ReceiptOption (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION, sValue);
  }

  @Nullable
  public String getMessageIDFormat (@Nullable final String sDefault)
  {
    return getAttribute (CPartnershipIDs.PA_MESSAGEID_FORMAT, sDefault);
  }

  @Nonnull
  public EChange setMessageIDFormat (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_MESSAGEID_FORMAT, sValue);
  }

  @Nullable
  public String getMDNSubject ()
  {
    return getAttribute (CPartnershipIDs.PA_MDN_SUBJECT);
  }

  @Nonnull
  public EChange setMDNSubject (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_MDN_SUBJECT, sValue);
  }

  public boolean isBlockErrorMDN ()
  {
    return m_aAttributes.containsAttribute (CPartnershipIDs.PA_BLOCK_ERROR_MDN);
  }

  @Nonnull
  public EChange setBlockErrorMDN (final boolean bBlock)
  {
    return setAttribute (CPartnershipIDs.PA_BLOCK_ERROR_MDN, bBlock ? "true" : null);
  }

  @Nullable
  public String getDateFormat (@Nullable final String sDefault)
  {
    return getAttribute (CPartnershipIDs.PA_DATE_FORMAT, sDefault);
  }

  @Nonnull
  public EChange setDateFormat (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_DATE_FORMAT, sValue);
  }

  @Nullable
  public String getEncryptAlgorithm ()
  {
    return getAttribute (CPartnershipIDs.PA_ENCRYPT);
  }

  @Nonnull
  public EChange setEncryptAlgorithm (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_ENCRYPT, sValue);
  }

  @Nonnull
  public EChange setEncryptAlgorithm (@Nullable final ECryptoAlgorithmCrypt eValue)
  {
    return setEncryptAlgorithm (eValue == null ? null : eValue.getID ());
  }

  @Nullable
  public String getSigningAlgorithm ()
  {
    return getAttribute (CPartnershipIDs.PA_SIGN);
  }

  @Nonnull
  public EChange setSigningAlgorithm (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_SIGN, sValue);
  }

  @Nonnull
  public EChange setSigningAlgorithm (@Nullable final ECryptoAlgorithmSign eValue)
  {
    return setSigningAlgorithm (eValue == null ? null : eValue.getID ());
  }

  @Nullable
  public String getProtocol ()
  {
    return getAttribute (CPartnershipIDs.PA_PROTOCOL);
  }

  @Nonnull
  public EChange setProtocol (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_PROTOCOL, sValue);
  }

  @Nullable
  public String getSubject ()
  {
    return getAttribute (CPartnershipIDs.PA_SUBJECT);
  }

  @Nonnull
  public EChange setSubject (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_SUBJECT, sValue);
  }

  @Nullable
  public String getContentTransferEncoding (@Nullable final String sDefault)
  {
    return getAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING, sDefault);
  }

  @Nonnull
  public EChange setContentTransferEncoding (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING, sValue);
  }

  @Nullable
  public String getContentTransferEncodingReceive (@Nullable final String sDefault)
  {
    return getAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING_RECEIVE, sDefault);
  }

  @Nonnull
  public EChange setContentTransferEncodingReceive (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING_RECEIVE, sValue);
  }

  @Nullable
  public String getCompressionType ()
  {
    return getAttribute (CPartnershipIDs.PA_COMPRESSION_TYPE);
  }

  @Nonnull
  public EChange setCompressionType (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_COMPRESSION_TYPE, sValue);
  }

  @Nonnull
  public EChange setCompressionType (@Nullable final EAS4CompressionMode eValue)
  {
    return setCompressionType (eValue == null ? null : eValue.getID ());
  }

  @Nullable
  public String getCompressionMode ()
  {
    return getAttribute (CPartnershipIDs.PA_COMPRESSION_MODE);
  }

  @Nonnull
  public EChange setCompressionMode (@Nullable final String sValue)
  {
    return setAttribute (CPartnershipIDs.PA_COMPRESSION_MODE, sValue);
  }

  public boolean isCompressBeforeSign ()
  {
    return !CPartnershipIDs.COMPRESS_AFTER_SIGNING.equals (getCompressionMode ());
  }

  @Nonnull
  public EChange setCompressionModeCompressAfterSigning ()
  {
    return setCompressionMode (CPartnershipIDs.COMPRESS_AFTER_SIGNING);
  }

  @Nonnull
  public EChange setCompressionModeCompressBeforeSigning ()
  {
    return setCompressionMode (CPartnershipIDs.COMPRESS_BEFORE_SIGNING);
  }

  public boolean isForceDecrypt ()
  {
    return "true".equals (getAttribute (CPartnershipIDs.PA_FORCE_DECRYPT));
  }

  @Nonnull
  public EChange setForceDecrypt (final boolean bValue)
  {
    return setAttribute (CPartnershipIDs.PA_FORCE_DECRYPT, Boolean.toString (bValue));
  }

  public boolean isDisableDecrypt ()
  {
    return "true".equals (getAttribute (CPartnershipIDs.PA_DISABLE_DECRYPT));
  }

  @Nonnull
  public EChange setDisableDecrypt (final boolean bValue)
  {
    return setAttribute (CPartnershipIDs.PA_DISABLE_DECRYPT, Boolean.toString (bValue));
  }

  public boolean isForceVerify ()
  {
    return "true".equals (getAttribute (CPartnershipIDs.PA_FORCE_VERIFY));
  }

  @Nonnull
  public EChange setForceVerify (final boolean bValue)
  {
    return setAttribute (CPartnershipIDs.PA_FORCE_VERIFY, Boolean.toString (bValue));
  }

  public boolean isDisableVerify ()
  {
    return "true".equals (getAttribute (CPartnershipIDs.PA_DISABLE_VERIFY));
  }

  @Nonnull
  public EChange setDisableVerify (final boolean bValue)
  {
    return setAttribute (CPartnershipIDs.PA_DISABLE_VERIFY, Boolean.toString (bValue));
  }

  @Nonnull
  private static ETriState _getAsTriState (@Nullable final String sValue)
  {
    if ("true".equals (sValue))
      return ETriState.TRUE;
    if ("false".equals (sValue))
      return ETriState.FALSE;
    return ETriState.UNDEFINED;
  }

  @Nonnull
  public ETriState getIncludeCertificateInSignedContent ()
  {
    final String sValue = getAttribute (CPartnershipIDs.PA_SIGN_INCLUDE_CERT_IN_BODY_PART);
    return _getAsTriState (sValue);
  }

  @Nonnull
  public EChange setIncludeCertificateInSignedContent (@Nonnull final ETriState eValue)
  {
    return setAttribute (CPartnershipIDs.PA_SIGN_INCLUDE_CERT_IN_BODY_PART,
                         eValue.isUndefined () ? null : Boolean.toString (eValue.getAsBooleanValue ()));
  }

  @Nonnull
  public ETriState getVerifyUseCertificateInBodyPart ()
  {
    final String sValue = getAttribute (CPartnershipIDs.PA_VERIFY_USE_CERT_IN_BODY_PART);
    return _getAsTriState (sValue);
  }

  @Nonnull
  public EChange setVerifyUseCertificateInBodyPart (@Nonnull final ETriState eValue)
  {
    return setAttribute (CPartnershipIDs.PA_VERIFY_USE_CERT_IN_BODY_PART,
                         eValue.isUndefined () ? null : Boolean.toString (eValue.getAsBooleanValue ()));
  }

  public boolean isDisableDecompress ()
  {
    return "true".equals (getAttribute (CPartnershipIDs.PA_DISABLE_DECOMPRESS));
  }

  @Nonnull
  public EChange setDisableDecompress (final boolean bValue)
  {
    return setAttribute (CPartnershipIDs.PA_DISABLE_DECOMPRESS, Boolean.toString (bValue));
  }

  /**
   * @return A copy of all contained attributes. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllAttributes ()
  {
    return m_aAttributes.getClone ();
  }

  /**
   * Add all provided attributes. existing attributes are not altered.
   *
   * @param aAttributes
   *        The attributes to be added. May be <code>null</code>. If a
   *        <code>null</code> value is contained in the map, the respective
   *        attribute will be removed.
   */
  public void addAllAttributes (@Nullable final Map <String, String> aAttributes)
  {
    m_aAttributes.setAttributes (aAttributes);
  }

  /**
   * Check if sender and receiver IDs of this partnership match the ones of the
   * provided partnership.
   *
   * @param aPartnership
   *        The partnership to compare to. May not be <code>null</code>.
   * @return <code>true</code> if sender and receiver IDs of this partnership
   *         are present in the sender and receiver IDs of the provided
   *         partnership.
   */
  public boolean matches (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    return compareIDs (m_aSenderIDs, aPartnership.m_aSenderIDs) &&
           compareIDs (m_aReceiverIDs, aPartnership.m_aReceiverIDs);
  }

  /**
   * Check if all values from the left side are also present on the right side.
   *
   * @param aIDs
   *        The source map which must be fully contained in the aCompareTo map
   * @param aCompareTo
   *        The map to compare to. May not be <code>null</code>. It may contain
   *        more attributes than aIDs but must at least contain the same ones.
   * @return <code>true</code> if aIDs is not empty and all values from aIDs are
   *         also present in aCompareTo, <code>false</code> otherwise.
   */
  protected boolean compareIDs (@Nonnull final IStringMap aIDs, @Nonnull final IStringMap aCompareTo)
  {
    if (aIDs.isEmpty ())
      return false;

    for (final Map.Entry <String, String> aEntry : aIDs)
    {
      final String sCurrentValue = aEntry.getValue ();
      final String sCompareValue = aCompareTo.getAttributeAsString (aEntry.getKey ());
      if (!EqualsHelper.equals (sCurrentValue, sCompareValue))
        return false;
    }
    return true;
  }

  /**
   * Set all fields of this partnership with the data from the provided
   * partnership. Name, sender IDs, receiver IDs and attributes are fully
   * overwritten!
   *
   * @param aPartnership
   *        The partnership to copy the data from. May not be <code>null</code>.
   */
  public void copyFrom (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");

    // Avoid doing something
    if (aPartnership != this)
    {
      m_sName = aPartnership.getName ();
      m_aSenderIDs.setAttributes (aPartnership.m_aSenderIDs);
      m_aReceiverIDs.setAttributes (aPartnership.m_aReceiverIDs);
      m_aAttributes.setAttributes (aPartnership.m_aAttributes);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("name", m_sName)
                                       .append ("senderIDs", m_aSenderIDs)
                                       .append ("receiverIDs", m_aReceiverIDs)
                                       .append ("attributes", m_aAttributes)
                                       .toString ();
  }

  @Nonnull
  public static Partnership createPlaceholderPartnership ()
  {
    return new Partnership (DEFAULT_NAME);
  }
}

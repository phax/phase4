package com.helger.as4lib.model.pmode;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.ETriState;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.util.MicroHelper;

public class PModeLegErrorHandlingMicroTypeConverter extends AbstractPModeMicroTypeConverter
{
  private static final String ELEMENT_X509_ENCRYPTION_ENCRYPT = "X509EncryptionEncrypt";
  private static final String ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH = "X509EncryptionMinimumStrength";
  private static final String ELEMENT_X509_SIGN = "X509Sign";
  private static final String ATTR_PMODE_AUTHORIZE = "PModeAuthorize";
  private static final String ATTR_SEND_RECEIPT = "SendReceipt";
  private static final String ATTR_USERNAME_TOKEN_CREATED = "UsernameTokenCreated";
  private static final String ATTR_USERNAME_TOKEN_DIGEST = "UsernameTokenDigest";
  private static final String ATTR_USERNAME_TOKEN_NONCE = "UsernameTokenNonce";
  private static final String ATTR_SEND_RECEIPT_REPLY_PATTERN = "SendReceiptReplyPattern";
  private static final String ATTR_USERNAME_TOKEN_PASSWORD = "UsernameTokenPassword";
  private static final String ATTR_USERNAME_TOKEN_USERNAME = "UsernameTokenUsername";
  private static final String ATTR_WSS_VERSION = "WSSVersion";
  private static final String ATTR_X509_ENCRYPTION_ALGORITHM = "X509EncryptionAlgorithm";
  private static final String ELEMENT_X509_ENCRYPTION_CERTIFICATE = "X509EncryptionCertificate";
  private static final String ATTR_X509_SIGNATURE_ALGORITHM = "X509SignatureAlgorithm";
  private static final String ELEMENT_X509_SIGNATURE_CERTIFICATE = "X509SignatureCertificate";
  private static final String ATTR_X509_SIGNATURE_HASH_FUNCTION = "X509SignatureHashFunction";

  public IMicroElement convertToMicroElement (final Object aObject, final String sNamespaceURI, final String sTagName)
  {
    final PModeLegSecurity aValue = (PModeLegSecurity) aObject;
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    for (final String sEncrypt : aValue.getX509EncryptionEncrypt ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_ENCRYPTION_ENCRYPT).appendText (sEncrypt);
    }
    if (aValue.hasX509EncryptionMinimumStrength ())
      ret.setAttribute (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH, aValue.getX509EncryptionMinimumStrength ().intValue ());
    for (final String sSign : aValue.getX509Sign ())
    {
      ret.appendElement (sNamespaceURI, ELEMENT_X509_SIGN).appendText (sSign);
    }
    if (aValue.isPModeAuthorizeDefined ())
      ret.setAttribute (ATTR_PMODE_AUTHORIZE, aValue.isPModeAuthorize ());
    if (aValue.isSendReceiptDefined ())
      ret.setAttribute (ATTR_SEND_RECEIPT, aValue.isSendReceipt ());
    if (aValue.isUsernameTokenCreatedDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_CREATED, aValue.isUsernameTokenCreated ());
    if (aValue.isUsernameTokenDigestDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_DIGEST, aValue.isUsernameTokenDigest ());
    if (aValue.isUsernameTokenNonceDefined ())
      ret.setAttribute (ATTR_USERNAME_TOKEN_NONCE, aValue.isUsernameTokenNonce ());
    ret.setAttribute (ATTR_SEND_RECEIPT_REPLY_PATTERN, aValue.getSendReceiptReplyPattern ());
    ret.setAttribute (ATTR_USERNAME_TOKEN_PASSWORD, aValue.getUsernameTokenPassword ());
    ret.setAttribute (ATTR_USERNAME_TOKEN_USERNAME, aValue.getUsernameTokenUsername ());
    ret.setAttribute (ATTR_WSS_VERSION, aValue.getWSSVersion ());
    ret.setAttribute (ATTR_X509_ENCRYPTION_ALGORITHM, aValue.getX509EncryptionAlgorithm ());
    ret.appendElement (sNamespaceURI, ELEMENT_X509_ENCRYPTION_CERTIFICATE)
       .appendText (aValue.getX509EncryptionCertificate ());
    ret.setAttribute (ATTR_X509_SIGNATURE_ALGORITHM, aValue.getX509SignatureAlgorithm ());
    ret.appendElement (sNamespaceURI, ELEMENT_X509_SIGNATURE_CERTIFICATE)
       .appendText (aValue.getX509SignatureCertificate ());
    ret.setAttribute (ATTR_X509_SIGNATURE_HASH_FUNCTION, aValue.getX509SignatureHashFunction ());
    return ret;
  }

  public Object convertToNative (final IMicroElement aElement)
  {
    final ICommonsList <String> aX509EncryptionEncryptStrings = new CommonsArrayList<> ();
    for (final IMicroElement aEncryptElement : aElement.getAllChildElements (ELEMENT_X509_ENCRYPTION_ENCRYPT))
    {
      aX509EncryptionEncryptStrings.add (aEncryptElement.getTextContentTrimmed ());
    }

    final Integer aX509EncryptionMinimumStrength = aElement.getAttributeValueWithConversion (ATTR_X509_ENCRYPTION_MINIMUM_STRENGTH,
                                                                                             Integer.class);
    final ICommonsList <String> aX509SignStrings = new CommonsArrayList<> ();
    for (final IMicroElement aSignElement : aElement.getAllChildElements (ELEMENT_X509_SIGN))
    {
      aX509SignStrings.add (aSignElement.getTextContentTrimmed ());
    }

    final ETriState ePModeAuthorize = getTriState (aElement.getAttributeValue (ATTR_PMODE_AUTHORIZE),
                                                   PModeLegSecurity.DEFAULT_PMODE_AUTHORIZE);
    final ETriState eSendReceipt = getTriState (aElement.getAttributeValue (ATTR_SEND_RECEIPT),
                                                PModeLegSecurity.DEFAULT_SEND_RECEIPT);
    final ETriState eUsernameTokenCreated = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_CREATED),
                                                         PModeLegSecurity.DEFAULT_USERNAME_TOKEN_CREATED);
    final ETriState eUsernameTokenDigest = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_DIGEST),
                                                        PModeLegSecurity.DEFAULT_USERNAME_TOKEN_DIGEST);
    final ETriState eUsernameTokenNonce = getTriState (aElement.getAttributeValue (ATTR_USERNAME_TOKEN_NONCE),
                                                       PModeLegSecurity.DEFAULT_USERNAME_TOKEN_NONCE);
    final String sSendReceiptReplyPattern = aElement.getAttributeValue (ATTR_SEND_RECEIPT_REPLY_PATTERN);
    final String sUsernameTokenPassword = aElement.getAttributeValue (ATTR_USERNAME_TOKEN_PASSWORD);
    final String sUsernameTokenUsername = aElement.getAttributeValue (ATTR_USERNAME_TOKEN_USERNAME);
    final String sWSSVersion = aElement.getAttributeValue (ATTR_WSS_VERSION);
    final String sX509EncryptionAlgorithm = aElement.getAttributeValue (ATTR_X509_ENCRYPTION_ALGORITHM);
    final String sX509EncryptionCertificate = MicroHelper.getChildTextContentTrimmed (aElement,
                                                                                      ELEMENT_X509_ENCRYPTION_CERTIFICATE);
    final String sX509SignatureAlgorithm = aElement.getAttributeValue (ATTR_X509_SIGNATURE_ALGORITHM);
    final String sX509SignatureCertificate = MicroHelper.getChildTextContentTrimmed (aElement,
                                                                                     ELEMENT_X509_SIGNATURE_CERTIFICATE);
    final String sX509SignatureHashFunction = aElement.getAttributeValue (ATTR_X509_SIGNATURE_HASH_FUNCTION);
    return new PModeLegSecurity (aX509EncryptionEncryptStrings,
                                 aX509EncryptionMinimumStrength,
                                 aX509SignStrings,
                                 ePModeAuthorize,
                                 eSendReceipt,
                                 eUsernameTokenCreated,
                                 eUsernameTokenDigest,
                                 eUsernameTokenNonce,
                                 sSendReceiptReplyPattern,
                                 sUsernameTokenPassword,
                                 sUsernameTokenUsername,
                                 sWSSVersion,
                                 sX509EncryptionAlgorithm,
                                 sX509EncryptionCertificate,
                                 sX509SignatureAlgorithm,
                                 sX509SignatureCertificate,
                                 sX509SignatureHashFunction);
  }

}

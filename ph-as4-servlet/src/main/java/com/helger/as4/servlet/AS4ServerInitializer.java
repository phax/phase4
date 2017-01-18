package com.helger.as4.servlet;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.CryptoType.TYPE;
import org.apache.wss4j.common.ext.WSSecurityException;

import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.partner.Partner;
import com.helger.as4.partner.PartnerManager;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorWSS4J;
import com.helger.as4.util.StringMap;
import com.helger.commons.collection.ArrayHelper;
import com.helger.security.certificate.CertificateHelper;

@Immutable
public final class AS4ServerInitializer
{
  private AS4ServerInitializer ()
  {}

  private static void _createDefaultResponder (@Nonnull final String sDefaultPartnerID)
  {
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    if (!aPartnerMgr.containsWithID (sDefaultPartnerID))
    {
      final StringMap aStringMap = new StringMap ();
      aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, sDefaultPartnerID);
      try
      {
        final CryptoType aCT = new CryptoType (TYPE.ALIAS);
        aCT.setAlias (AS4CryptoFactory.getKeyAlias ());
        final X509Certificate [] aCertList = AS4CryptoFactory.getCrypto ().getX509Certificates (aCT);
        if (ArrayHelper.isEmpty (aCertList))
          throw new IllegalStateException ("Failed to find default partner certificate from alias '" +
                                           aCT.getAlias () +
                                           "'");
        aStringMap.setAttribute (Partner.ATTR_CERT, CertificateHelper.getPEMEncodedCertificate (aCertList[0]));
        aPartnerMgr.createOrUpdatePartner (sDefaultPartnerID, aStringMap);
      }
      catch (final WSSecurityException ex)
      {
        throw new IllegalStateException ("Error retrieving certificate", ex);
      }
    }
  }

  public static void initAS4Server ()
  {

    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry aReg = SOAPHeaderElementProcessorRegistry.getInstance ();
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
                                                    "Messaging"),
                                         new SOAPHeaderElementProcessorExtractEbms3Messaging ());
    // WSS4J must be after Ebms3Messaging handler!
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                                                    "Security"),
                                         new SOAPHeaderElementProcessorWSS4J ());

    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();
    _createDefaultResponder (AS4ServerSettings.getDefaultResponderID ());
  }
}

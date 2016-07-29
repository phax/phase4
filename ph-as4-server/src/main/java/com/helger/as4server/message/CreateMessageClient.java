package com.helger.as4server.message;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

public class CreateMessageClient
{
  public static final ConfigFile CF = new ConfigFileBuilder ().addPath ("crypto.properties").build ();

  public Document createSignedMessage (@Nonnull final Document aDocument) throws WSSecurityException
  {
    WSSConfig.init ();
    // Uses crypto.properties => needs exact name crypto.properties
    final Crypto aCrypto = CryptoFactory.getInstance ();
    final WSSecSignature aBuilder = new WSSecSignature ();
    aBuilder.setUserInfo (CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.alias"),
                          CF.getAsString ("org.apache.wss4j.crypto.merlin.keystore.password"));
    aBuilder.setKeyIdentifierType (WSConstants.BST_DIRECT_REFERENCE);
    aBuilder.setSignatureAlgorithm ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    // TODO DONT FORGET: PMode indicates the DigestAlgorithmen as Hash Function
    aBuilder.setDigestAlgo ("http://www.w3.org/2001/04/xmlenc#sha256");
    final Document aDoc = aDocument;
    final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
    aSecHeader.insertSecurityHeader ();
    // TODO if you set attribute with NS it adds the same namespace again since
    // it does not take the one from envelope => Change NS between S11 and S12
    final Attr aMustUnderstand = aSecHeader.getSecurityHeader ().getAttributeNode ("S11:mustUnderstand");
    // TODO Needs to be set to 0 (equals false) since holodeck currently throws
    // a exception he does not understand mustUnderstand
    aMustUnderstand.setValue ("0");

    final Document aSignedDoc = aBuilder.build (aDoc, aCrypto, aSecHeader);
    return aSignedDoc;
  }

}

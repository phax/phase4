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
package com.helger.phase4.wss;

import java.util.List;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.str.STRParser;
import org.w3c.dom.Document;

import com.helger.io.resource.ClassPathResource;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.crypto.ECryptoMode;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.scope.mock.ScopeTestRule;
import com.helger.xml.serialize.read.DOMReader;

/** Invoked through an isolated class loader by {@link AS4SignatureWithoutBCTest}. */
public final class AS4SignatureWithoutBCProbe
{
  private AS4SignatureWithoutBCProbe ()
  {}

  public static String verify () throws Exception
  {
    final ScopeTestRule aScopeRule = new ScopeTestRule ();
    aScopeRule.before ();
    try
    {
      final IAS4CryptoFactory aCryptoFactory = AS4CryptoFactoryConfiguration.getDefaultInstance ();
      final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("UserMessageWithoutWSSE.xml"));
      if (aDoc == null)
        throw new IllegalStateException ("Failed to read the test SOAP envelope");

      final WSSecHeader aSecHeader = new WSSecHeader (aDoc);
      aSecHeader.insertSecurityHeader ();

      final WSSecSignature aBuilder = new WSSecSignature (aSecHeader);
      aBuilder.setUserInfo (aCryptoFactory.getKeyAlias (),
                            aCryptoFactory.getKeyPasswordPerAlias (aCryptoFactory.getKeyAlias ()));
      aBuilder.setKeyIdentifierType (ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE.getTypeID ());
      aBuilder.setSignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256.getAlgorithmURI ());
      aBuilder.setDigestAlgo (ECryptoAlgorithmSignDigest.DIGEST_SHA_256.getAlgorithmURI ());
      final Document aSignedDoc = aBuilder.build (aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN));

      final WSSecurityEngine aSecEngine = new WSSecurityEngine ();
      aSecEngine.setWssConfig (WSSConfigManager.getInstance ().createWSSConfig ());
      final WSHandlerResult aResults = aSecEngine.processSecurityHeader (aSignedDoc,
                                                                         null,
                                                                         null,
                                                                         aCryptoFactory.getCrypto (ECryptoMode.ENCRYPT_SIGN));

      final List <WSSecurityEngineResult> aSignResults = aResults.getActionResults ()
                                                                .get (Integer.valueOf (WSConstants.SIGN));
      if (aSignResults == null || aSignResults.size () != 1)
        throw new IllegalStateException ("Expected exactly one verified signature result");

      final STRParser.REFERENCE_TYPE eReferenceType = (STRParser.REFERENCE_TYPE) aSignResults.get (0)
                                                                                             .get (WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
      return eReferenceType.name ();
    }
    finally
    {
      aScopeRule.after ();
    }
  }
}

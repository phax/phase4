/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.callback;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.client.AS4ClientBuiltMessage;
import com.helger.phase4.client.AS4ClientReceiptMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.ECryptoAlgorithmC14N;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.crypto.MultiHopSignatureCustomizer;
import com.helger.phase4.multihop.model.MultiHopRoutingInput;
import com.helger.phase4.multihop.model.RoutingInputInferrer;
import com.helger.phase4.multihop.soap.MultiHopSoapHelper;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * Builds a <b>callback</b> Receipt for a User Message that was received via Pulling.
 * <p>
 * In the "First-push-last-pull" edge binding (ebMS3 Part 2 section 2.4.7.1 case 2) the receiving
 * endpoint pulls the User Message from the I-Cloud. It cannot answer synchronously, because there
 * is no request to answer - the Receipt has to be sent as a separate message, carrying the same
 * three routing headers as a synchronous one (R4).
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class AS4MultiHopReceiptSender
{
  private AS4MultiHopReceiptSender ()
  {}

  /**
   * Build a signed callback Receipt for the provided pulled User Message.
   *
   * @param aUserMsg
   *        The User Message the Receipt relates to. May not be <code>null</code> and is never
   *        modified.
   * @param aSourceSoapDoc
   *        The SOAP document of the received User Message, needed for the non-repudiation
   *        information. May be <code>null</code>.
   * @param sMessageID
   *        The message ID of the Receipt to be created. May neither be <code>null</code> nor
   *        empty.
   * @param eSoapVersion
   *        The SOAP version to be used. May not be <code>null</code>.
   * @param aCryptoFactory
   *        The crypto factory used for signing. May not be <code>null</code>.
   * @param aResHelper
   *        The resource helper. May not be <code>null</code>.
   * @param aCfg
   *        The multi-hop configuration. May not be <code>null</code>.
   * @return The signed and built Receipt, ready to be pushed to the I-Cloud. Never
   *         <code>null</code>.
   * @throws Exception
   *         on signing errors
   */
  @NonNull
  public static AS4ClientBuiltMessage createCallbackReceipt (@NonNull final Ebms3UserMessage aUserMsg,
                                                @Nullable final Document aSourceSoapDoc,
                                                @NonNull @Nonempty final String sMessageID,
                                                @NonNull final ESoapVersion eSoapVersion,
                                                @NonNull final IAS4CryptoFactory aCryptoFactory,
                                                @NonNull final AS4ResourceHelper aResHelper,
                                                @NonNull final AS4MultiHopConfig aCfg) throws Exception
  {
    ValueEnforcer.notNull (aUserMsg, "UserMessage");
    ValueEnforcer.notEmpty (sMessageID, "MessageID");
    ValueEnforcer.notNull (eSoapVersion, "SoapVersion");
    ValueEnforcer.notNull (aCryptoFactory, "CryptoFactory");
    ValueEnforcer.notNull (aResHelper, "ResHelper");
    ValueEnforcer.notNull (aCfg, "Config");

    // R6 - the inferred reverse RoutingInput
    final MultiHopRoutingInput aRoutingInput = RoutingInputInferrer.inferReverse (aUserMsg,
                                                                                  EAS4MessageType.RECEIPT,
                                                                                  aCfg);

    final AS4ClientReceiptMessage aClient = new AS4ClientReceiptMessage (aResHelper);
    aClient.setSoapVersion (eSoapVersion);
    aClient.setEbms3UserMessage (aUserMsg);
    aClient.setSoapDocument (aSourceSoapDoc);
    aClient.setNonRepudiation (aSourceSoapDoc != null);
    aClient.setReceiptShouldBeSigned (true);
    aClient.setCryptoFactory (aCryptoFactory);
    // Use the AS4 default signing algorithms. Deliberately NOT setFromPMode(null) - that clears
    // the algorithms and therefore silently disables signing altogether.
    aClient.signingParams ()
           .setAlgorithmSign (ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT)
           .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT)
           .setAlgorithmC14N (ECryptoAlgorithmC14N.C14N_ALGORITHM_DEFAULT);

    /*
     * The callback below runs on the very same Document that is signed afterwards
     * (AS4ClientReceiptMessage builds the SOAP document, hands it to onSoapDocument and then signs
     * exactly that instance). This is the only place where the routing headers can be added and
     * still end up inside the signature (R4, R9).
     */
    final IAS4ClientBuildMessageCallback aCallback = new IAS4ClientBuildMessageCallback ()
    {
      @Override
      public void onSoapDocument (@NonNull final Document aDoc)
      {
        final ICommonsList <String> aIDsToSign = MultiHopSoapHelper.addResponseAddressingHeaders (aDoc,
                                                                                                   eSoapVersion,
                                                                                                   CAS4MultiHop.WSA_ACTION_ONEWAY_RECEIPT,
                                                                                                   aRoutingInput);
        if (aCfg.isSignAddressingHeaders ())
          aClient.signingParams ()
                 .setWSSecSignatureCustomizer (new MultiHopSignatureCustomizer (aClient.signingParams ()
                                                                                       .getWSSecSignatureCustomizer (),
                                                                                 aIDsToSign));
      }
    };

    return aClient.buildMessage (sMessageID, aCallback);
  }
}

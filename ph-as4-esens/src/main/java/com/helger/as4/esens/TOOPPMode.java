package com.helger.as4.esens;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4.CAS4;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeParty;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.model.pmode.PModeReceptionAwareness;
import com.helger.as4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.as4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.as4.model.pmode.leg.PModeLegProtocol;
import com.helger.as4.model.pmode.leg.PModeLegReliability;
import com.helger.as4.model.pmode.leg.PModeLegSecurity;
import com.helger.as4.wss.EWSSVersion;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ETriState;

public class TOOPPMode
{
  private static final String DEFAULT_AGREEMENT_ID = "urn:as4:agreement";

  private TOOPPMode ()
  {}

  @Nonnull
  private static PModeLegSecurity _generatePModeLegSecurity ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (Integer.valueOf (128));
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  /**
   * One-Way TOOP pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode <code>false</code> to have it
   *        only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createTOOPMode (@Nonnull @Nonempty final String sInitiatorID,
                                      @Nonnull @Nonempty final String sResponderID,
                                      @Nullable final String sResponderAddress,
                                      @Nonnull final IPModeIDProvider aPModeIDProvider,
                                      final boolean bPersist)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, "http://www.toop.eu/edelivery/backend");
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, "http://www.toop.eu/edelivery/gateway");

    final PMode aPMode = new PMode (aPModeIDProvider,
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    new PModeLeg (PModeLegProtocol.createForDefaultSOAPVersion (sResponderAddress),
                                                  new PModeLegBusinessInformation (null,
                                                                                   CAS4.DEFAULT_ACTION_URL,
                                                                                   null,
                                                                                   CAS4.DEFAULT_MPC_ID),
                                                  new PModeLegErrorHandling (null,
                                                                             null,
                                                                             ETriState.TRUE,
                                                                             ETriState.TRUE,
                                                                             ETriState.UNDEFINED,
                                                                             ETriState.TRUE),
                                                  (PModeLegReliability) null,
                                                  _generatePModeLegSecurity ()),
                                    (PModeLeg) null,
                                    (PModePayloadService) null,
                                    PModeReceptionAwareness.createDefault ());
    // Leg 2 stays null, because we only use one-way

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

}

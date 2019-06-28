package com.helger.as4.server.servlet.cef;

import java.nio.charset.StandardCharsets;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.profile.cef.CEFPMode;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.io.streamprovider.StringInputStreamProvider;
import com.helger.commons.mime.CMimeType;

/**
 * CEF Conformance Testing constants
 *
 * @author Philip Helger
 */
public class CCEFCT
{
  public static final String ROLE_MINDER = "http://www.esens.eu/as4/conformancetest/testdriver";
  public static final String ROLE_SUT = "http://www.esens.eu/as4/conformancetest/sut";

  public static final String SERVICE_VALUE = "http://www.esens.eu/as4/conformancetest";
  private static final WSS4JAttachment aAttachment1;
  private static final WSS4JAttachment aAttachment2;
  private static final WSS4JAttachment aAttachment3;

  static
  {
    final AS4ResourceManager aResMgr = new AS4ResourceManager ();
    aAttachment1 = new WSS4JAttachment (aResMgr, CMimeType.APPLICATION_XML.getAsString ());
    aAttachment1.setId ("xmlpayload@minder");
    aAttachment1.setCharset (StandardCharsets.UTF_8);
    aAttachment1.setSourceStreamProvider (new StringInputStreamProvider ("<ph><as4>payload</as4></ph>",
                                                                         StandardCharsets.UTF_8));

    aAttachment2 = new WSS4JAttachment (aResMgr, CMimeType.APPLICATION_XML.getAsString ());
    aAttachment2.setId ("xmlpayload2@minder");
    aAttachment2.setCharset (StandardCharsets.UTF_8);
    aAttachment2.setSourceStreamProvider (new StringInputStreamProvider ("<ph><as4>payload2</as4></ph>",
                                                                         StandardCharsets.UTF_8));

    aAttachment3 = new WSS4JAttachment (aResMgr, CMimeType.APPLICATION_OCTET_STREAM.getAsString ());
    aAttachment3.setId ("custompayload@minder");
    aAttachment3.setSourceStreamProvider (new StringInputStreamProvider ("I am custom binary payload!",
                                                                         StandardCharsets.ISO_8859_1));
  }

  private CCEFCT ()
  {}

  public static void registerPModes ()
  {
    // CEF conformance testing PModes
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    {
      // SIMPLE_ONEWAY
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_SIMPLE_ONEWAY
      // 7. Action: ACT_SIMPLE_ONEWAY
      final PMode aPMode = CEFPMode.createCEFPMode ("AnyInitiatorID",
                                                    "AnyResponderID",
                                                    "AnyResponderAddress",
                                                    (i, r) -> "SIMPLE_ONEWAY",
                                                    false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_SIMPLE_ONEWAY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_SIMPLE_ONEWAY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // SIMPLE_TWOWAY
      // 1. MEP: Two way push-and-push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_SIMPLE_TWOWAY
      // 7. Action: ACT_SIMPLE_TWOWAY
      final PMode aPMode = CEFPMode.createCEFPModeTwoWay ("AnyInitiatorID",
                                                          "AnyResponderID",
                                                          "AnyResponderAddress",
                                                          (i, r) -> "SIMPLE_TWOWAY",
                                                          false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_SIMPLE_TWOWAY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_SIMPLE_TWOWAY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // ONEWAY_RETRY
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: 5 (the interval between retries must be less than 3 minutes)
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_ONEWAY_RETRY
      // 7. Action: ACT_ONEWAY_RETRY
      final PMode aPMode = CEFPMode.createCEFPMode ("AnyInitiatorID",
                                                    "AnyResponderID",
                                                    "AnyResponderAddress",
                                                    (i, r) -> "ONEWAY_RETRY",
                                                    false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (true);
      aPMode.getReceptionAwareness ().setMaxRetries (5);
      aPMode.getReceptionAwareness ().setRetryIntervalMS (10_000);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_ONEWAY_RETRY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_ONEWAY_RETRY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // ONEWAY_ONLY_SIGN
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: No
      // 6. Service: SRV_ONEWAY_SIGNONLY
      // 7. Action: ACT_ONEWAY_SIGNONLY
      final PMode aPMode = CEFPMode.createCEFPMode ("AnyInitiatorID",
                                                    "AnyResponderID",
                                                    "AnyResponderAddress",
                                                    (i, r) -> "ONEWAY_ONLY_SIGN",
                                                    false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getSecurity ().setX509EncryptionAlgorithm (null);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_ONEWAY_SIGNONLY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_ONEWAY_SIGNONLY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
    {
      // PING
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service:
      // http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service
      // 7. Action:
      // http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test
      final PMode aPMode = CEFPMode.createCEFPMode ("AnyInitiatorID",
                                                    "AnyResponderID",
                                                    "AnyResponderAddress",
                                                    (i, r) -> "PING",
                                                    false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService (CAS4.DEFAULT_SERVICE_URL);
      aPMode.getLeg1 ().getBusinessInfo ().setAction (CAS4.DEFAULT_ACTION_URL);
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
  }
}

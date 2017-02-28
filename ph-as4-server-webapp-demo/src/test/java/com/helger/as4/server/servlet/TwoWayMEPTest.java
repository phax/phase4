package com.helger.as4.server.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.mail.internet.MimeMessage;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.client.HttpMimeMessageEntity;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.messaging.mime.MimeMessageCreator;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.EMEP;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.config.PModeConfig;
import com.helger.as4.model.pmode.leg.PModeLeg;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mime.CMimeType;

public class TwoWayMEPTest extends AbstractUserMessageTestSetUpExt
{
  private static PMode s_aPMode;

  @BeforeClass
  public static void createTwoWayPMode ()
  {
    s_aPMode = ESENSPMode.createESENSPMode (AS4ServerConfiguration.getSettings ()
                                                                  .getAsString ("server.address",
                                                                                "http://localhost:8080/as4"));
    // ESENS PMode is One Way on default settings need to change to two way
    final PModeConfig aPModeConfig = new PModeConfig ("esens-two-way");
    aPModeConfig.setMEP (EMEP.TWO_WAY);
    aPModeConfig.setMEPBinding (EMEPBinding.SYNC);
    aPModeConfig.setAgreement (s_aPMode.getConfig ().getAgreement ());
    aPModeConfig.setLeg1 (s_aPMode.getConfig ().getLeg1 ());
    // Setting second leg to the same as first
    final PModeLeg aLeg2 = s_aPMode.getConfig ().getLeg1 ();
    aLeg2.getSecurity ().setX509EncryptionAlgorithm (null);
    aPModeConfig.setLeg2 (aLeg2);
    aPModeConfig.setPayloadService (s_aPMode.getConfig ().getPayloadService ());
    aPModeConfig.setReceptionAwareness (s_aPMode.getConfig ().getReceptionAwareness ());
    MetaAS4Manager.getPModeConfigMgr ().createOrUpdatePModeConfig (aPModeConfig);
    s_aPMode = new PMode (s_aPMode.getInitiator (), s_aPMode.getResponder (), aPModeConfig);
    MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (s_aPMode);
  }

  @AfterClass
  public static void destroyTwoWayPMode ()
  {
    MetaAS4Manager.getPModeMgr ().deletePMode (s_aPMode.getID ());
  }

  @Test
  public void receiveUserMessageAsResponseSuccess () throws Exception
  {
    final Document aDoc = _modifyUserMessage (s_aPMode.getConfigID (), null, null, _defaultProperties ());
    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
    assertTrue (sResponse.contains ("UserMessage"));
    assertFalse (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains (s_aPMode.getConfig ()
                                            .getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
  }

  @Test
  public void receiveUserMessageWithMimeAsResponseSuccess () throws Exception
  {
    final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList<> ();
    final AS4ResourceManager aResMgr = s_aResMgr;
    aAttachments.add (WSS4JAttachment.createOutgoingFileAttachment (ClassPathResource.getAsFile ("attachment/shortxml.xml"),
                                                                    CMimeType.APPLICATION_XML,
                                                                    null,
                                                                    aResMgr));

    final Document aDoc = _modifyUserMessage (s_aPMode.getConfigID (), null, null, _defaultProperties (), aAttachments);
    final MimeMessage aMimeMsg = new MimeMessageCreator (ESOAPVersion.SOAP_12).generateMimeMessage (aDoc, aAttachments);
    final String sResponse = sendMimeMessage (new HttpMimeMessageEntity (aMimeMsg), true, null);
    assertTrue (sResponse.contains ("UserMessage"));
    assertFalse (sResponse.contains ("Receipt"));
    assertTrue (sResponse.contains (s_aPMode.getConfig ()
                                            .getLeg2 ()
                                            .getSecurity ()
                                            .getX509SignatureAlgorithm ()
                                            .getAlgorithmURI ()));
    // Checking if he adds the attachment to the response message, the mock spi
    // just adds the xml that gets send to the response
    assertTrue (sResponse.contains ("<dummy>This is a test XML</dummy>"));

  }
}

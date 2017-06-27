package com.helger.as4;

public class AS4TestConstants
{
  // Default values
  public static final String DEFAULT_SERVER_ADDRESS = "http://localhost:8080/as4";
  public static final String DEFAULT_MPC = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC";

  // Test value that can be used
  public static final String TEST_RESPONDER = "TestResponder";
  public static final String TEST_INITIATOR = "TestInitiator";
  public static final String TEST_ACTION = "NewPurchaseOrder";
  public static final String TEST_CONVERSATION_ID = "4321";
  public static final String TEST_SERVICE_TYPE = "MyServiceTypes";
  public static final String TEST_SERVICE = "QuoteToCollect";
  public static final String TEST_SOAP_BODY_PAYLOAD_XML = "SOAPBodyPayload.xml";
  public static final String TEST_PAYLOAD_XML = "PayloadXML.xml";

  // Attachments
  public static final String ATTACHMENT_SHORTXML2_XML = "attachment/shortxml2.xml";
  public static final String ATTACHMENT_TEST_IMG_JPG = "attachment/test-img.jpg";
  public static final String ATTACHMENT_SHORTXML_XML = "attachment/shortxml.xml";
  public static final String ATTACHMENT_TEST_XML_GZ = "attachment/test.xml.gz";
  public static final String ATTACHMENT_TEST_IMG2_JPG = "attachment/test-img2.jpg";

  // CEF
  public static final String CEF_INITIATOR_ID = "CEF-Initiator";
  public static final String CEF_RESPONDER_ID = "CEF-Responder";

  // Common Asserts
  public static final String NON_REPUDIATION_INFORMATION = "NonRepudiationInformation";
  public static final String RECEIPT_ASSERTCHECK = "Receipt";
  public static final String USERMESSAGE_ASSERTCHECK = "UserMessage";
}

package com.helger.as4.client;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

public class AS4Client
{
  private final ICommonsList <WSS4JAttachment> aAttachments = new CommonsArrayList <> ();
  private Node aPayload;
  private Document aDoc;
}

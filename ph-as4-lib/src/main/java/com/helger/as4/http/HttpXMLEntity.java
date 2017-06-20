package com.helger.as4.http;

import javax.annotation.Nonnull;

import org.apache.http.entity.StringEntity;
import org.w3c.dom.Node;

import com.helger.as4.util.AS4XMLHelper;

public class HttpXMLEntity extends StringEntity
{
  public HttpXMLEntity (@Nonnull final Node Node)
  {
    super (AS4XMLHelper.serializeXML (Node), AS4XMLHelper.XWS.getCharsetObj ());
  }
}

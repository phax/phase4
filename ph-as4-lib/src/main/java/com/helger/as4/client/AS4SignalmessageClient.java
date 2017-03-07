package com.helger.as4.client;

import org.w3c.dom.Element;

import com.helger.commons.collection.ext.CommonsArrayList;

public abstract class AS4SignalmessageClient extends AbstractAS4Client
{
  private CommonsArrayList <Element> any;

  public CommonsArrayList <Element> getAny ()
  {
    return any;
  }

  public void setAny (final CommonsArrayList <Element> any)
  {
    this.any = any;
  }
}

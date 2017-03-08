package com.helger.as4.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Element;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

public abstract class AbstractAS4ClientSignalMessage extends AbstractAS4Client
{
  private final ICommonsList <Element> m_aAny = new CommonsArrayList<> ();

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Element> getAny ()
  {
    return m_aAny.getClone ();
  }

  public void setAny (@Nullable final Iterable <? extends Element> aAny)
  {
    m_aAny.setAll (aAny);
  }
}

package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringParser;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

public abstract class AbstractPModeMicroTypeConverter implements IMicroTypeConverter
{
  @Nonnull
  protected static ETriState getTriState (@Nullable final String sAttrValue, final boolean bDefault)
  {
    return sAttrValue == null ? ETriState.UNDEFINED : ETriState.valueOf (StringParser.parseBool (sAttrValue, bDefault));
  }

}

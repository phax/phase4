package com.helger.as4server.spi;

import javax.annotation.Nonnull;

import com.helger.as4server.attachment.IIncomingAttachment;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.ext.ICommonsList;

@IsSPIImplementation
public class TestMessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{

  @Nonnull
  public AS4MessageProcessorResponse processAS4Message (final byte [] aPayload,
                                                        final ICommonsList <IIncomingAttachment> aIncomingAttachments)
  {
    System.out.println ("HERE!Q!!!!!!");
    return new AS4MessageProcessorResponse ();
  }

}

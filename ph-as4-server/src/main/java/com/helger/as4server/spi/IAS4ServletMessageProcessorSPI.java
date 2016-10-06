package com.helger.as4server.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4server.attachment.IIncomingAttachment;
import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.collection.ext.ICommonsList;

@IsSPIInterface
public interface IAS4ServletMessageProcessorSPI
{
  /**
   * Process incoming AS4 message
   *
   * @param aPayload
   *        Extracted, decrypted and verified payload. May be <code>null</code>.
   * @param aIncomingAttachments
   *        Extracted, decrypted and verified attachments. May be
   *        <code>null</code> or empty if no attachments are present.
   * @return A non-<code>null</code> response object.
   */
  @Nonnull
  AS4MessageProcessorResponse processAS4Message (@Nullable byte [] aPayload,
                                                 @Nullable ICommonsList <IIncomingAttachment> aIncomingAttachments);
}

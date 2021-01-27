/**
 * Copyright (C) 2015-2021 Pavel Rotek (www.helger.com)
 * pavel[dot]rotek[at]gmail[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.entsog;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.phase4.attachment.Phase4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.sender.AS4BidirectionalClientHelper;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;

/**
 * This class contains all the specifics to send AS4 messages with the ENTSOG
 * profile. See <code>sendAS4Message</code> as the main method to trigger the
 * sending, with all potential customization.
 *
 * @author Pavel Rotek
 * @since 0.13.3
 */
@Immutable
public final class Phase4ENTSOGSender {
	private static final Logger LOGGER = LoggerFactory.getLogger(Phase4ENTSOGSender.class);

	private Phase4ENTSOGSender() {
	}

	/**
	 * @return Create a new Builder for AS4 messages if the payload is present.
	 *         Never <code>null</code>.
	 */
	@Nonnull
	public static ENTSOGUserMessageBuilder builder() {
		return new ENTSOGUserMessageBuilder();
	}

	/**
	 * Abstract ENTSOG UserMessage builder class with sanity methods
	 *
	 * @author Philip Helger
	 * @param <IMPLTYPE> The implementation type
	 */
	public abstract static class AbstractENTSOGUserMessageBuilder<IMPLTYPE extends AbstractENTSOGUserMessageBuilder<IMPLTYPE>>
			extends AbstractAS4UserMessageBuilder<IMPLTYPE> {

		private ECryptoKeyIdentifierType m_signingKeyIdentifierType = ECryptoKeyIdentifierType.ISSUER_SERIAL;
		private ECryptoKeyIdentifierType m_encryptionKeyIdentifierType = ECryptoKeyIdentifierType.ISSUER_SERIAL;

		private Phase4OutgoingAttachment m_aPayload;
		private ENTSOGPayloadParams m_aPayloadParams;

		protected AbstractENTSOGUserMessageBuilder() {
			// Override default values
			try {
				httpClientFactory(new Phase4ENTSOGHttpClientSettings());
			} catch (final Exception ex) {
				throw new IllegalStateException("Failed to init AS4 Client builder", ex);
			}
		}

		/**
		 * Encryption Key Identifier Type.
		 *
		 * @param keyIdentifier ECryptoKeyIdentifierType. Defaults to ISSUER_SERIAL
		 * @return this for chaining
		 */
		public final IMPLTYPE setEncryptionKeyIdentifierType(ECryptoKeyIdentifierType encryptionKeyIdentifierType) {
			m_encryptionKeyIdentifierType = encryptionKeyIdentifierType;
			return thisAsT();
		}

		/**
		 * Signing Key Identifier Type.
		 *
		 * @param keyIdentifier ECryptoKeyIdentifierType. Defaults to ISSUER_SERIAL
		 * @return this for chaining
		 */
		public final IMPLTYPE setSigningKeyIdentifierType(ECryptoKeyIdentifierType signingKeyIdentifierType) {
			m_signingKeyIdentifierType = signingKeyIdentifierType;
			return thisAsT();
		}

		/**
		 * Set the payload to be send out.
		 *
		 * @param aBuilder The payload builder to be used.
		 * @return this for chaining
		 */
		@Nonnull
		public final IMPLTYPE payload(@Nullable final Phase4OutgoingAttachment.Builder aBuilder,
				@Nullable ENTSOGPayloadParams aPayloadParams) {
			m_aPayload = aBuilder.compressionGZIP().build();
			m_aPayloadParams = aPayloadParams;
			return thisAsT();
		}

		@Override
		@OverridingMethodsMustInvokeSuper
		public boolean isEveryRequiredFieldSet() {
			if (!super.isEveryRequiredFieldSet())
				return false;

			if (m_aPayload == null) {
				return false;
			}

			return true;
		}

		@Override
		protected final void mainSendMessage() throws Phase4Exception {
			// Temporary file manager
			try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper()) {
				// Start building AS4 User Message
				final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage(aResHelper);
				applyToUserMessage(aUserMsg);

				aUserMsg.cryptParams().setKeyIdentifierType(m_encryptionKeyIdentifierType);
				aUserMsg.signingParams().setKeyIdentifierType(m_signingKeyIdentifierType);
				aUserMsg.setConversationID("");

				// No payload - only one attachment
				aUserMsg.setPayload(null);

				// Add main attachment
				WSS4JAttachment payloadAttachment = WSS4JAttachment.createOutgoingFileAttachment(m_aPayload,
						aResHelper);

				if (m_aPayloadParams != null) {
					payloadAttachment.setCharset(m_aPayloadParams.getCharset());
					if (m_aPayloadParams.getDocumentType() != null) {
						payloadAttachment.customPartProperties().put("EDIGASDocumentType",
								m_aPayloadParams.getDocumentType());
					}
				}
				aUserMsg.addAttachment(payloadAttachment);

				// Add other attachments
				for (final Phase4OutgoingAttachment aAttachment : m_aAttachments)
					aUserMsg.addAttachment(WSS4JAttachment.createOutgoingFileAttachment(aAttachment, aResHelper));

				// Main sending
				AS4BidirectionalClientHelper.sendAS4UserMessageAndReceiveAS4SignalMessage(m_aCryptoFactory,
						pmodeResolver(), incomingAttachmentFactory(), incomingProfileSelector(), aUserMsg, m_aLocale,
						m_sEndpointURL, m_aBuildMessageCallback, m_aOutgoingDumper, m_aIncomingDumper, m_aRetryCallback,
						m_aResponseConsumer, m_aSignalMsgConsumer);
			} catch (final Phase4Exception ex) {
				// Re-throw
				throw ex;
			} catch (final Exception ex) {
				// wrap
				throw new Phase4Exception("Wrapped Phase4Exception", ex);
			}
		}

	}

	/**
	 * The builder class for sending AS4 messages using ENTSOG profile specifics.
	 * Use {@link #sendMessage()} to trigger the main transmission.
	 *
	 * @author Philip Helger
	 */
	public static class ENTSOGUserMessageBuilder extends AbstractENTSOGUserMessageBuilder<ENTSOGUserMessageBuilder> {
		public ENTSOGUserMessageBuilder() {
		}
	}

	/**
	 * Additional parameters to add in the PayloadInfo part of AS4 UserMessage
	 * 
	 * @author Pavel Rotek
	 *
	 */
	public static class ENTSOGPayloadParams {

		@Nullable
		private Charset m_aCharset;
		@Nullable
		private String m_sDocumentType;

		/**
		 * Payload charset, usually UTF-8.
		 * 
		 * @return
		 */
		public Charset getCharset() {
			return m_aCharset;
		}

		/**
		 * Payload charset
		 * 
		 * @param aCharset
		 */
		public void setCharset(Charset aCharset) {
			this.m_aCharset = aCharset;
		}

		/**
		 * ENTSOG payload document type accoding to EDIG@S. Eg. "01G" for EDIG@S
		 * Nomination document.
		 * 
		 * @return
		 */
		public String getDocumentType() {
			return m_sDocumentType;
		}

		/**
		 * ENTSOG payload document type
		 * 
		 * @param sDocumentType
		 */
		public void setDocumentType(String sDocumentType) {
			this.m_sDocumentType = sDocumentType;
		}
	}

}

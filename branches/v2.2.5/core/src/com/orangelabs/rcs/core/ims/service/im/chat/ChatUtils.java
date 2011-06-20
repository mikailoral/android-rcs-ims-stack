/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright © 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.im.chat;

import java.util.List;
import javax.sip.header.ExtensionHeader;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Chat utility functions
 * 
 * @author jexa7410
 */
public class ChatUtils {
	/**
	 * OMA IM feature tag
	 */
	public final static String FEATURE_OMA_IM = "+g.oma.sip-im";

	/**
	 * Get asserted identity
	 * 
	 * @param request SIP request
	 * @param groupChat Is group chat
	 * @return SIP URI
	 */
	public static String getAssertedIdentity(SipRequest request, boolean groupChat) {
		if (groupChat) {
			ExtensionHeader referredBy = (ExtensionHeader)request.getHeader(SipUtils.HEADER_REFERRED_BY);
			if (referredBy != null) {
				// Use the Referred-By header
				return referredBy.getValue();
			} else {
				// Use the From header
				return request.getFromUri();
			}
		} else {
			// Use the P-Asserted-Identity header
			return SipUtils.getAssertedIdentity(request);
		}
	}

	/**
     * Is a plain text type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextPlainType(String mime) {
    	if ((mime != null) && mime.equalsIgnoreCase(InstantMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is a composing event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
    	if ((mime != null) && mime.equalsIgnoreCase(IsComposingInfo.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }    

    /**
     * Is a CPIM message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
    	if ((mime != null) && mime.equalsIgnoreCase(CpimMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is an IMDN message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
    	if ((mime != null) && mime.equalsIgnoreCase(ImdnDocument.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Generate a unique message ID
     * 
     * @return Message ID
     */
    public static String generateMessageId() {
    	return "Msg" + IdGenerator.getIdentifier();
    }

    /**
     * Generate resource-list document for a list of participants
     * 
     * @param participants List of participants
     * @return XML document
     */
    public static String generateResourceListForParticipants(List<String> participants) {
		String uriList = "";
		for(int i=0; i < participants.size(); i++) {
			String contact = participants.get(i);
			uriList += " <entry uri=\"" + PhoneUtils.formatNumberToSipAddress(contact) + "\"/>" + SipUtils.CRLF;
		}
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SipUtils.CRLF +
			"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">" +
			"<list>" + SipUtils.CRLF +
			uriList +
			"</list></resource-lists>";
		return xml;
    }    
    
    /**
     * Is IMDN service
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnService(SipRequest request) {
    	ExtensionHeader hd = (ExtensionHeader)request.getHeader(CpimMessage.HEADER_NS);
    	if ((hd != null) && hd.getValue().contains("<urn:ietf:params:imdn>")) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is IMDN notification "delivered" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDeliveredRequested(SipRequest request) {
    	ExtensionHeader hd = (ExtensionHeader)request.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
    	if (hd != null){
    		String hdValue = hd.getValue();
    		if (hdValue!=null && hdValue.contains(ImdnDocument.POSITIVE_DELIVERY)){    		
    			return true;
    		}else{
    			return false;
    		}
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is IMDN notification "displayed" requested
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDisplayedRequested(SipRequest request) {
    	ExtensionHeader hd = (ExtensionHeader)request.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
    	if (hd != null){
    		String hdValue = hd.getValue();
    		if (hdValue!=null && hdValue.contains(ImdnDocument.DISPLAY)){    		
    			return true;
    		}else{
    			return false;
    		}
    	} else {
    		return false;
    	}
    }
    
	/**
	 * Returns the message id header value of a SIP request
	 * 
     * @param request Request
	 * @return String or empty
	 */
	public static String getMessageId(SipRequest request) {
		// Read Message-Id header
		ExtensionHeader messageIdHeader = (ExtensionHeader)request.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID);
		if (messageIdHeader != null) {
			return messageIdHeader.getValue();
		} else {
			return null;
		}
	}
	
	/**
	 * Build a CPIM message in plain text format
	 * 
	 * @param from From
	 * @param to To
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessage(String from, String to, String content, String contentType) {
		String cpim = CpimMessage.HEADER_FROM + ": " + from + CpimMessage.CRLF + 
			CpimMessage.HEADER_TO + ": " + to + CpimMessage.CRLF + 
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CpimMessage.CRLF + 
			CpimMessage.CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + CpimMessage.CRLF + 
			CpimMessage.CRLF + 
			content;	
		   
		return cpim;
	}
	
	/**
	 * Build a CPIM message with IMDN disposition-notification headers
	 * 
	 * @param from From
	 * @param to To
	 * @param messageId Message id value
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithIMDN(String from, String to, String messageId, String content, String contentType) {
		String cpim = CpimMessage.HEADER_FROM + ": " + from + CpimMessage.CRLF + 
			CpimMessage.HEADER_TO + ": " + to + CpimMessage.CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CpimMessage.CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CpimMessage.CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CpimMessage.CRLF + 
			ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + ", " + ImdnDocument.NEGATIVE_DELIVERY + ", " + ImdnDocument.DISPLAY + CpimMessage.CRLF +
			CpimMessage.CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + CpimMessage.CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + content.length() + CpimMessage.CRLF + 
			CpimMessage.CRLF + 
			content;	
		   
		return cpim;
	}
	
	/**
	 * Build a CPIM message with IMDN with content-disposition headers
	 * 
	 * @param from From
	 * @param to To
	 * @param messageId Message id value
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithImdnPlusXml(String from, String to, String messageId, String content, String contentType) {
		String cpim = CpimMessage.HEADER_FROM + ": " + from + CpimMessage.CRLF + 
			CpimMessage.HEADER_TO + ": " + to + CpimMessage.CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CpimMessage.CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CpimMessage.CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CpimMessage.CRLF + 
			CpimMessage.HEADER_CONTENT_DISPOSITION + ": " + ImdnDocument.NOTIFICATION + CpimMessage.CRLF +
			CpimMessage.CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + CpimMessage.CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + content.length() + CpimMessage.CRLF + 
			CpimMessage.CRLF + 
			content;	
		   
		return cpim;
	}
	
}

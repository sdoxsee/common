
/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.extension.html;
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.hl7.handler.ORUR01Handler;
import org.openmrs.hl7.handler.ADTA28Handler;
import org.openmrs.module.RHEASHRadapter.util.RHEA_ORU_R01Handler;
 
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.app.MessageTypeRouter;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.GenericParser;
 
/**
 * Main entry point for processing of HL7 streams into OpenMRS
 * 
 * @version 1.0
 */
public class HL7Receiver {
	
	Log log = LogFactory.getLog(HL7Receiver.class);
	
	private GenericParser parser;
	
	private MessageTypeRouter router;
	
	public HL7Receiver() {
		parser = new GenericParser();
	}
	
	public Message processMessage(String hl7, String enterpriseId) throws HL7Exception {
		
		log.debug("Register handler applications for R01 and A28");
		// TODO draw registered applications from database or configuration file
		router = new MessageTypeRouter();
		router.registerApplication("ORU", "R01", new RHEA_ORU_R01Handler(enterpriseId));
		router.registerApplication("ADT", "A28", new ADTA28Handler());
		
		// TODO: any pre-parsing for HL7 messages would go here
		
		// First, try and parse the message
		Message message;
		try {
			message = parser.parse(hl7);
		}
		catch (EncodingNotSupportedException e) {
			throw new HL7Exception("HL7 encoding not supported", e);
		}
		catch (HL7Exception e) {
			throw new HL7Exception("Error parsing message", e);
		}
		
		// TODO: any post-parsing (pre-routing) processing would go here
		
		// If parsing succeeded, then try to route the message
		Message response;
		try {
			if (!router.canProcess(message))
				throw new HL7Exception("No route for message");
			response = router.processMessage(message);
		}
		catch (ApplicationException e) {
			throw new HL7Exception("Error routing HL7 message", e);
		}
		return response;
	}
}

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
package org.openmrs.module.RHEASHRadapter.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.ParseErrorException;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PersonService;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7Source;

import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.api.EncounterService;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.NM;
import ca.uhn.hl7v2.model.v25.datatype.ST;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.NK1;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.impl.DefaultValidation;

public class GenerateORU_R01 implements Serializable {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private static final long serialVersionUID = 1L;
	private static Integer obxCount;
	
	private ORU_R01 r01 = new ORU_R01();
	
	public ORU_R01 generateORU_R01Message(Patient pat, List<Encounter> encounterList) throws Exception {
		
		MSH msh = r01.getMSH();
		
		// Get current date
		String dateFormat = "yyyyMMddHHmmss";
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		String formattedDate = formatter.format(new Date());
		
			msh.getFieldSeparator().setValue(RHEAHL7Constants.FIELD_SEPARATOR);//
			msh.getEncodingCharacters().setValue(RHEAHL7Constants.ENCODING_CHARACTERS);//
			msh.getVersionID().getInternationalizationCode().getIdentifier().setValue(
			    RHEAHL7Constants.INTERNATIONALIZATION_CODE);//
			msh.getVersionID().getVersionID().setValue(RHEAHL7Constants.VERSION);//
			msh.getDateTimeOfMessage().getTime().setValue(formattedDate);//
			msh.getSendingApplication().getNamespaceID().setValue(RHEAHL7Constants.SENDING_APPLICATION);
			msh.getSendingFacility().getNamespaceID().setValue(RHEAHL7Constants.SENDING_FACILITY);//
			msh.getMessageType().getMessageCode().setValue(RHEAHL7Constants.MESSAGE_TYPE);//
			msh.getMessageType().getTriggerEvent().setValue(RHEAHL7Constants.TRIGGER_EVENT);//
			msh.getMessageType().getMessageStructure().setValue(RHEAHL7Constants.MESSAGE_STRUCTURE);//
			msh.getReceivingApplication().getNamespaceID().setValue(RHEAHL7Constants.RECEIVING_APPLICATION);
			msh.getReceivingFacility().getNamespaceID().setValue(RHEAHL7Constants.RECEIVING_FACILITY);//
			msh.getProcessingID().getProcessingID().setValue(RHEAHL7Constants.PROCESSING_ID);//
			msh.getProcessingID().getProcessingMode().setValue(RHEAHL7Constants.PROCESSING_MODE);//
			msh.getMessageControlID().setValue(UUID.randomUUID().toString());//
			
			msh.getAcceptAcknowledgmentType().setValue(RHEAHL7Constants.ACK_TYPE);
			msh.getApplicationAcknowledgmentType().setValue(RHEAHL7Constants.APPLICATION_ACK_TYPE);
		
		Cohort singlePatientCohort = new Cohort();
		singlePatientCohort.addMember(pat.getId());
		
		Map<Integer, String> patientIdentifierMap = Context.getPatientSetService().getPatientIdentifierStringsByType(
		    singlePatientCohort,
		    Context.getPatientService().getPatientIdentifierTypeByName(RHEAHL7Constants.IDENTIFIER_TYPE));
		
		PID pid = r01.getPATIENT_RESULT().getPATIENT().getPID();
		
			pid.getSetIDPID().setValue(RHEAHL7Constants.IDPID);
			pid.getPatientIdentifierList(0).getIDNumber().setValue(
			    patientIdentifierMap.get(patientIdentifierMap.keySet().iterator().next()));
			pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(RHEAHL7Constants.IDENTIFIER_TYPE_CODE);
			pid.getPatientName(0).getFamilyName().getSurname().setValue(pat.getFamilyName());
			
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
			Date dob = pat.getBirthdate();
			Date dod = pat.getDeathDate();
			String dobStr = "";
			String dodStr = "";
			if (dob != null)
				dobStr = df.format(dob);
			if (dod != null)
				dodStr = df.format(dod);
			
			// Address
			pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(
			    pat.getPersonAddress().getAddress1());
			pid.getPatientAddress(0).getOtherDesignation().setValue(pat.getPersonAddress().getAddress2());
			pid.getPatientAddress(0).getCity().setValue(pat.getPersonAddress().getCityVillage());
			pid.getPatientAddress(0).getStateOrProvince().setValue(pat.getPersonAddress().getStateProvince());
			pid.getPatientAddress(0).getZipOrPostalCode().setValue(pat.getPersonAddress().getPostalCode());
			
			// gender
			pid.getAdministrativeSex().setValue(pat.getGender());
			
			// dob
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);
			
			// Death
			pid.getPatientDeathIndicator().setValue(pat.getDead().toString());
			pid.getPatientDeathDateAndTime().getTime().setValue(dodStr);
			
		
		PV1 pv1 = r01.getPATIENT_RESULT().getPATIENT().getVISIT().getPV1();
	
			pv1.getSetIDPV1().setValue(RHEAHL7Constants.IDPV1);
			pv1.getPatientClass().setValue(RHEAHL7Constants.PATIENT_CLASS);
			pv1.getAssignedPatientLocation().getFacility().getNamespaceID().setValue(
			    encounterList.get(0).getLocation().getDescription());
			if(encounterList.get(0).getProvider().getId() != null)
			pv1.getAttendingDoctor(0).getIDNumber().setValue(encounterList.get(0).getProvider().getId().toString());
			
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue(encounterList.get(0).getProvider().getFamilyName());
			pv1.getAttendingDoctor(0).getGivenName().setValue(encounterList.get(0).getProvider().getGivenName());
			pv1.getVisitNumber().getIDNumber().setValue(encounterList.get(0).getEncounterId().toString());
			pv1.getAdmitDateTime().getTime().setValue(
			    new SimpleDateFormat("yyyyMMddhhmm").format(encounterList.get(0).getDateCreated()));
			
		
		// / NK1
		
		NK1 nk1 = r01.getPATIENT_RESULT().getPATIENT().getNK1();
	
			String ln = "";
			String fn = "";
			
			if(pat.getAttribute(RHEAHL7Constants.ATTR_NEXT_OF_KIN) != null) {
			String nkName = pat.getAttribute(RHEAHL7Constants.ATTR_NEXT_OF_KIN).getValue();
			
			if ((pat != null) && (pat.getAttribute(RHEAHL7Constants.ATTR_TELEPHONE_NUMBER) != null)) {
				String tel = pat.getAttribute(RHEAHL7Constants.ATTR_TELEPHONE_NUMBER).getValue();
				nk1.getPhoneNumber(0).getTelephoneNumber().setValue(tel);
			}
			if ((nkName != null) && !nkName.equals("")) {
				int indexSpace = nkName.indexOf(" ");
				if (!nkName.isEmpty()) {
					if (indexSpace < 0) {
						fn = nkName;
					} else {
						fn = nkName.substring(0, indexSpace - 1);
						ln = nkName.substring(indexSpace + 1);
					}
				}
			}		
			
			nk1.getNKName(0).getFamilyName().getSurname().setValue(ln);
			nk1.getNKName(0).getGivenName().setValue(fn);
			
			nk1.getRelationship().getIdentifier().setValue(RHEAHL7Constants.ATTR_NEXT_OF_KIN);
			}
			
			if (pat != null) {
				PersonAddress pa = pat.getPersonAddress();
				nk1.getAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(pa.getAddress1());
				nk1.getAddress(0).getStreetAddress().getDwellingNumber().setValue(pa.getAddress2());
				nk1.getAddress(0).getCity().setValue(pa.getCityVillage());
				nk1.getAddress(0).getStateOrProvince().setValue(pa.getStateProvince());
				nk1.getAddress(0).getZipOrPostalCode().setValue(pa.getPostalCode());
				nk1.getAddress(0).getCountry().setValue(pa.getCountry());
			}
		
		// populate ORC segments
		
		createORCs(r01,encounterList);
		
		// populate OBR segments
		
		createOBRs(r01,encounterList);
		
		// populate OBX segments
		
		ConceptService cs = Context.getConceptService();
		
		int counter = 0;
		for(Encounter e : encounterList){
			
			Set<Obs> s = new HashSet<Obs>();
			s = e.getAllObs();
		
			 obxCount = 0;
		for (Obs obs : s) {
			 
			// set datatype
			ConceptDatatype datatype = obs.getConcept().getDatatype();
			
			Collection<ConceptMap> conceptMappings = obs.getConcept().getConceptMappings();
			
			boolean hasMapping = false;
			for (ConceptMap conceptMap : conceptMappings) {
				if (conceptMap.getSource().getHl7Code().equals(RHEAHL7Constants.RW_CS)) {
					hasMapping = true;
				}
			}
			
			if (hasMapping) {
				log.info("Obs has a mapping for concept...");
			
					// if numeric value
					if (datatype.equals(cs.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_NUMERIC))) {

						OBX obx = createOBX(r01, obs, counter);
						obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_NM);
						
						NM nm = new NM(r01);
						nm.setValue(obs.getValueNumeric() + "");
						
						Concept concept = obs.getConcept();
						if (concept.isNumeric()) {
							ConceptNumeric conceptNumeric = cs.getConceptNumeric(concept.getId());
							if (conceptNumeric.getUnits() != null && !conceptNumeric.getUnits().equals("")) {
								obx.getUnits().getIdentifier().setValue(conceptNumeric.getUnits());
								obx.getUnits().getNameOfCodingSystem().setValue(RHEAHL7Constants.UNIT_CODING_SYSTEM);
							}
						}
						obx.getObservationValue(0).setData(nm);
						TS ts = new TS(r01);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
						ts.getTime().setValue(sdf.format(obs.getDateCreated()));
						obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
						
					} else if (datatype.equals(cs.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_DATETIME))
					        || datatype.equals(cs.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_DATE))) {

						OBX obx = createOBX(r01, obs, counter);
						
						obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_TS);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						TS ts = new TS(r01);
						ts.getTime().setValue(sdf.format(obs.getValueDatetime()));
						obx.getObservationValue(0).setData(ts);
						
						SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
						ts.getTime().setValue(sdf1.format(obs.getDateCreated()));
						obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
						
					} else if (datatype.equals(cs.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_TEXT))) {

						OBX obx = createOBX(r01, obs, counter);
						
						obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_ST);
						ST st = new ST(r01);
						st.setValue(obs.getValueText());
						obx.getObservationValue(0).setData(st);
						
						TS ts = new TS(r01);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
						ts.getTime().setValue(sdf.format(obs.getDateCreated()));
						obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
						
					} else if (datatype.equals(cs.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_CODED))) {

						OBX obx = createOBX(r01, obs, counter);
						
						obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_CE);
						
						CE ce = new CE(r01);
						Concept concept = obs.getValueCoded();
						
						Collection<ConceptMap> conceptValueMappings = concept.getConceptMappings();
						
						for (ConceptMap conceptMap : conceptValueMappings) {
							if (conceptMap.getSource().getHl7Code().equals(RHEAHL7Constants.RW_CN)) {
								ce.getText().setValue(conceptMap.getSourceCode());
							}
							if (conceptMap.getSource().getHl7Code().equals("RW_AC")) {
								ce.getIdentifier().setValue(conceptMap.getSourceCode());
							}
							if (conceptMap.getSource().getHl7Code().equals("RW_AS")) {
								ce.getNameOfCodingSystem().setValue(conceptMap.getSourceCode());
							}
						}
						
						if (ce.getText().getValue() == null || ce.getText().getValue().equals("")) {
							String nameStr = concept.getName().toString();
							ce.getText().setValue(nameStr);
						}
						
						obx.getObservationValue(0).setData(ce);
						
						TS ts = new TS(r01);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
						ts.getTime().setValue(sdf.format(obs.getDateCreated()));
						obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
					}
					
			}
		}
		
		counter++;
	}
		
		return r01;
	}
	
	public String getMessage(ORU_R01 r01) {
		PipeParser pipeParser = new PipeParser();
		String msg = null;
		try {
			msg = pipeParser.encode(r01);
		}
		catch (HL7Exception e) {
			log.error("Exception parsing constructed message.");
		}
		return msg;
	}	
	
	private static void createORCs(ORU_R01 r01, List<Encounter> encounterList) throws Exception{
		int orderORCCount = 0;
		
		for(Encounter e : encounterList){
		ORC orc = null;

			orc = r01.getPATIENT_RESULT().getORDER_OBSERVATION(orderORCCount).getORC();
			orc.getPlacerOrderNumber().getNamespaceID().setValue(RHEAHL7Constants.PROVIDER_SENDING_APPLICATION);
	        orc.getOrderingProvider(0).getFamilyName().getSurname().setValue(e.getProvider().getFamilyName());

	        orc.getOrderingProvider(0).getGivenName().setValue(e.getProvider().getGivenName());
	        orc.getOrderingProvider(0).getIDNumber().setValue(e.getProvider().getId().toString());
	        
	        orc.getOrderControlCodeReason().getIdentifier().setValue("");
	        
	        orc.getEnteredBy(0).getFamilyName().getSurname().setValue(e.getCreator().getFamilyName());
	        orc.getEnteredBy(0).getGivenName().setValue(e.getCreator().getGivenName());
	        orc.getEnteredBy(0).getIDNumber().setValue(e.getCreator().getId().toString());
	        
	        //Cannot input ordering facility information since OpenMRS trunk does not store these
	        
	        orc.getEnteringOrganization().getText().setValue(RHEAHL7Constants.ENTERING_ORGANIZATION);
	        orc.getOrderingFacilityName(0).getOrganizationName().setValue(RHEAHL7Constants.ORDERING_ORGANIZATION);
	        
	        //How shall I fill in the Order control reason ? 
	        //orc.getOrderControlCodeReason();

	        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
	        String dateStr = "";
	        Date d = new Date();
	        dateStr = df.format(d);
	        
	        orc.getDateTimeOfTransaction().getTime().setValue(dateStr);
		
		orderORCCount++;
		
		}
	}
	
	private static void createOBRs(ORU_R01 r01, List<Encounter> encounterList) throws Exception{
		
		int orderObsCount = 0;
		for(Encounter e : encounterList){
		OBR obr = null;
	
			obr = r01.getPATIENT_RESULT().getORDER_OBSERVATION(orderObsCount).getOBR();
			int reps = r01.getPATIENT_RESULT().getORDER_OBSERVATIONReps();
			
			Date encDt = e.getEncounterDatetime();
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
			SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
			
			String encDateStr = "";
			String encDateOnly = "";
			if (encDt != null) {
				encDateStr = df.format(encDt);
				encDateOnly = dayFormat.format(encDt);
			}
			obr.getObservationDateTime().getTime().setValue(encDateStr);
			obr.getSetIDOBR().setValue(String.valueOf(reps));
			obr.getUniversalServiceIdentifier().getIdentifier().setValue(RHEAHL7Constants.UNIV_SERVICE_ID);
			obr.getUniversalServiceIdentifier().getText().setValue(RHEAHL7Constants.UNIV_SERVICE_NAME);
			obr.getUniversalServiceIdentifier().getNameOfCodingSystem().setValue(RHEAHL7Constants.OBR_CODE_SYSTEM);
			obr.getOrderingProvider(0).getFamilyName().getSurname().setValue(e.getProvider().getFamilyName());
			obr.getOrderingProvider(0).getGivenName().setValue(e.getProvider().getGivenName());
			obr.getOrderingProvider(0).getIDNumber().setValue(Integer.toString(e.getProvider().getId()));
			obr.getResultCopiesTo(0).getFamilyName().getSurname().setValue(e.getProvider().getFamilyName());
			obr.getResultCopiesTo(0).getGivenName().setValue(e.getProvider().getGivenName());
			obr.getResultCopiesTo(0).getIDNumber().setValue(Integer.toString(e.getProvider().getId()));
			obr.getResultStatus().setValue(RHEAHL7Constants.RESULT_STATUS);
			obr.getSpecimenActionCode().setValue(RHEAHL7Constants.SPECIMEN_ACTION_CODE);
			
			// Accession number
			String accessionNumber = String.valueOf(e.getEncounterId()) + "-" + RHEAHL7Constants.UNIV_SERVICE_ID
			        + "-" + encDateOnly;
			obr.getFillerOrderNumber().getEntityIdentifier().setValue(accessionNumber);
			
		orderObsCount++;
		}
	}
	
	private static OBX createOBX(ORU_R01 r01, Obs obs, int counter) throws HL7Exception, DataTypeException {
		// for each obs
		OBX obx = r01.getPATIENT_RESULT().getORDER_OBSERVATION(counter).getOBSERVATION(obxCount).getOBX();
		obx.getSetIDOBX().setValue(obxCount + "");
		
		Collection<ConceptMap> conceptMappings = obs.getConcept().getConceptMappings();
		
		for (ConceptMap conceptMap : conceptMappings) {
			if (conceptMap.getSource().getHl7Code().equals(RHEAHL7Constants.RW_CS)) {
				obx.getObservationIdentifier().getIdentifier().setValue(conceptMap.getSourceCode());
			}
			if (conceptMap.getSource().getHl7Code().equals(RHEAHL7Constants.RW_CN)) {
				obx.getObservationIdentifier().getText().setValue(conceptMap.getSourceCode());
			}
		}
		
		if (obx.getObservationIdentifier().getText().getValue() == null
		        || obx.getObservationIdentifier().getText().getValue().equals("")) {
			obx.getObservationIdentifier().getText().setValue(obs.getConcept().getName().toString());
		}
		obx.getObservationIdentifier().getNameOfCodingSystem().setValue(RHEAHL7Constants.NAME_OF_CODING_SYSTEM);
		
		obxCount++;
			
		return obx;
	}
	
}

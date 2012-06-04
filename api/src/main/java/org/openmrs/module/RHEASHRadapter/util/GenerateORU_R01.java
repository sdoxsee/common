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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.NM;
import ca.uhn.hl7v2.model.v25.datatype.ST;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.NK1;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.parser.PipeParser;

public class GenerateORU_R01 implements Serializable {
	
	private Log log = LogFactory.getLog(this.getClass());
	private static final long serialVersionUID = 1L;
	
	private ConceptService conceptService;

	public GenerateORU_R01() {
		this(Context.getConceptService());
	}
	
	public GenerateORU_R01(ConceptService conceptService) {
		this.conceptService = conceptService;
	}
	
	public ORU_R01 generateORU_R01Message(Patient patient, List<Encounter> encounterList) throws Exception {
		ORU_R01 message = new ORU_R01();
		
		mapToMSH(message.getMSH());

		ORU_R01_PATIENT oru_R01_PATIENT = message.getPATIENT_RESULT().getPATIENT();
		
		mapToPID(oru_R01_PATIENT.getPID(), patient);
		mapToPV1(oru_R01_PATIENT.getVISIT().getPV1(), encounterList);
		mapToNK1(oru_R01_PATIENT.getNK1(), patient);
		// populate ORC segments
		mapToORCs(message, encounterList);
		// populate OBR segments
		mapToOBRs(message, encounterList);
		// populate OBX segments
		mapToOBXs(message, encounterList);
		
		return message;
	}
	
	private void mapToOBXs(ORU_R01 message, List<Encounter> encounterList) throws HL7Exception,
			DataTypeException {
		int orderIndex = 0;
		for(Encounter e : encounterList) {
			int obxIndex = 0;
			for (Obs observation : e.getAllObs()) {
				if (hasMapping(observation)) {
					log.info("Obs has a mapping for concept...");
					
					// for each observation
					OBX obx = message.getPATIENT_RESULT().getORDER_OBSERVATION(orderIndex).getOBSERVATION(obxIndex).getOBX();
					mapToOBX(obx, observation);
					obx.getSetIDOBX().setValue(obxIndex + "");
					
					ConceptDatatype datatype = observation.getConcept().getDatatype();
					// if numeric value
					if (isNumeric(obx, datatype)) {
						mapToNumericObx(obx, observation);
					} else if (isDateOrDatetime(datatype)) {
						mapToDateOrDatetimeObx(obx, observation);
					} else if (isText(datatype)) {
						mapToTextObx(obx, observation);
					} else if (isCoded(datatype)) {
						mapToCodedObx(obx, observation);
					}
					obxIndex++;
				}
			}
			
			orderIndex++;
		}
	}

	private boolean hasMapping(Obs observation) {
		Collection<ConceptMap> conceptMappings = observation.getConcept().getConceptMappings();
		boolean mapping = false;
		for (ConceptMap conceptMap : conceptMappings) {
			if (conceptMap.getSource().getHl7Code().equals(RHEAHL7Constants.RW_CS)) {
				mapping = true;
			}
		}
		return mapping;
	}
	
	private boolean isCoded(ConceptDatatype datatype) {
		return datatype.equals(conceptService.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_CODED));
	}
	private boolean isText(ConceptDatatype datatype) {
		return datatype.equals(conceptService.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_TEXT));
	}
	private boolean isDateOrDatetime(ConceptDatatype datatype) {
		return datatype.equals(conceptService.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_DATETIME))
		        || datatype.equals(conceptService.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_DATE));
	}
	private boolean isNumeric(OBX obx, ConceptDatatype datatype) {
		return datatype.equals(conceptService.getConceptDatatypeByName(RHEAHL7Constants.CONCEPT_DATATYPE_NUMERIC));
	}

	private void mapToCodedObx(OBX obx, Obs observation) throws HL7Exception,
			DataTypeException {
		
		obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_CE);
		
		CE ce = new CE(obx.getMessage());
		Concept concept = observation.getValueCoded();
		
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
		
		TS ts = new TS(obx.getMessage());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		ts.getTime().setValue(sdf.format(observation.getDateCreated()));
		obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
	}

	private void mapToTextObx(OBX obx, Obs observation) throws HL7Exception,
			DataTypeException {
		
		obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_ST);
		ST st = new ST(obx.getMessage());
		st.setValue(observation.getValueText());
		obx.getObservationValue(0).setData(st);
		
		TS ts = new TS(obx.getMessage());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		ts.getTime().setValue(sdf.format(observation.getDateCreated()));
		obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
	}

	private void mapToDateOrDatetimeObx(OBX obx, Obs obs) throws HL7Exception,
			DataTypeException {
		
		obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_TS);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		TS ts = new TS(obx.getMessage());
		ts.getTime().setValue(sdf.format(obs.getValueDatetime()));
		obx.getObservationValue(0).setData(ts);
		
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
		ts.getTime().setValue(sdf1.format(obs.getDateCreated()));
		obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
	}

	private void mapToNumericObx(OBX obx, Obs observation)
			throws HL7Exception, DataTypeException {
		obx.getValueType().setValue(RHEAHL7Constants.VALUE_TYPE_NM);
		
		NM nm = new NM(obx.getMessage());
		nm.setValue(observation.getValueNumeric() + "");
		
		Concept concept = observation.getConcept();
		if (concept.isNumeric()) {
			ConceptNumeric conceptNumeric = this.conceptService.getConceptNumeric(concept.getId());
			if (conceptNumeric.getUnits() != null && !conceptNumeric.getUnits().equals("")) {
				obx.getUnits().getIdentifier().setValue(conceptNumeric.getUnits());
				obx.getUnits().getNameOfCodingSystem().setValue(RHEAHL7Constants.UNIT_CODING_SYSTEM);
			}
		}
		obx.getObservationValue(0).setData(nm);
		TS ts = new TS(obx.getMessage());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		ts.getTime().setValue(sdf.format(observation.getDateCreated()));
		obx.getDateTimeOfTheObservation().getTime().setValue(ts.toString());
	}

	private void mapToNK1(NK1 nk1, Patient patient) throws DataTypeException,
			HL7Exception {
		
			String ln = "";
			String fn = "";
			
			if(patient.getAttribute(RHEAHL7Constants.ATTR_NEXT_OF_KIN) != null) {
			String nkName = patient.getAttribute(RHEAHL7Constants.ATTR_NEXT_OF_KIN).getValue();
			
			if ((patient != null) && (patient.getAttribute(RHEAHL7Constants.ATTR_TELEPHONE_NUMBER) != null)) {
				String tel = patient.getAttribute(RHEAHL7Constants.ATTR_TELEPHONE_NUMBER).getValue();
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
			
			if (patient != null) {
				PersonAddress pa = patient.getPersonAddress();
				nk1.getAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(pa.getAddress1());
				nk1.getAddress(0).getStreetAddress().getDwellingNumber().setValue(pa.getAddress2());
				nk1.getAddress(0).getCity().setValue(pa.getCityVillage());
				nk1.getAddress(0).getStateOrProvince().setValue(pa.getStateProvince());
				nk1.getAddress(0).getZipOrPostalCode().setValue(pa.getPostalCode());
				nk1.getAddress(0).getCountry().setValue(pa.getCountry());
			}
	}

	private void mapToPV1(PV1 pv1, List<Encounter> encounterList)
			throws DataTypeException, HL7Exception {
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
	}

	private void mapToMSH(MSH msh) throws DataTypeException {
		msh.getFieldSeparator().setValue(RHEAHL7Constants.FIELD_SEPARATOR);//
			msh.getEncodingCharacters().setValue(RHEAHL7Constants.ENCODING_CHARACTERS);//
			msh.getVersionID().getInternationalizationCode().getIdentifier().setValue(
			    RHEAHL7Constants.INTERNATIONALIZATION_CODE);//
			msh.getVersionID().getVersionID().setValue(RHEAHL7Constants.VERSION);//
			msh.getDateTimeOfMessage().getTime().setValue(getCurrentDate());//
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
	}

	private void mapToPID(PID pid, Patient patient) throws DataTypeException,
			HL7Exception {
		Cohort singlePatientCohort = new Cohort();
		singlePatientCohort.addMember(patient.getId());
		
		Map<Integer, String> patientIdentifierMap = Context.getPatientSetService().getPatientIdentifierStringsByType(
		    singlePatientCohort,
		    Context.getPatientService().getPatientIdentifierTypeByName(RHEAHL7Constants.IDENTIFIER_TYPE));
		
			pid.getSetIDPID().setValue(RHEAHL7Constants.IDPID);
			pid.getPatientIdentifierList(0).getIDNumber().setValue(
			    patientIdentifierMap.get(patientIdentifierMap.keySet().iterator().next()));
			pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(RHEAHL7Constants.IDENTIFIER_TYPE_CODE);
			pid.getPatientName(0).getFamilyName().getSurname().setValue(patient.getFamilyName());
			
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
			Date dob = patient.getBirthdate();
			Date dod = patient.getDeathDate();
			String dobStr = "";
			String dodStr = "";
			if (dob != null)
				dobStr = df.format(dob);
			if (dod != null)
				dodStr = df.format(dod);
			
			// Address
			pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(
			    patient.getPersonAddress().getAddress1());
			pid.getPatientAddress(0).getOtherDesignation().setValue(patient.getPersonAddress().getAddress2());
			pid.getPatientAddress(0).getCity().setValue(patient.getPersonAddress().getCityVillage());
			pid.getPatientAddress(0).getStateOrProvince().setValue(patient.getPersonAddress().getStateProvince());
			pid.getPatientAddress(0).getZipOrPostalCode().setValue(patient.getPersonAddress().getPostalCode());
			
			// gender
			pid.getAdministrativeSex().setValue(patient.getGender());
			
			// dob
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);
			
			// Death
			pid.getPatientDeathIndicator().setValue(patient.getDead().toString());
			pid.getPatientDeathDateAndTime().getTime().setValue(dodStr);
	}

	private String getCurrentDate() {
		// Get current date
		String dateFormat = "yyyyMMddHHmmss";
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		String formattedDate = formatter.format(new Date());
		return formattedDate;
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
	
	private static void mapToORCs(ORU_R01 r01, List<Encounter> encounterList) throws Exception{
		int orderORCCount = 0;
		for(Encounter encounter : encounterList){
			mapToORC(r01.getPATIENT_RESULT().getORDER_OBSERVATION(orderORCCount).getORC(), encounter);
			orderORCCount++;
		}
	}

	private static void mapToORC(ORC orc, Encounter encounter)
			throws DataTypeException, HL7Exception {
		orc.getPlacerOrderNumber().getNamespaceID().setValue(RHEAHL7Constants.PROVIDER_SENDING_APPLICATION);
		orc.getOrderingProvider(0).getFamilyName().getSurname().setValue(encounter.getProvider().getFamilyName());

		orc.getOrderingProvider(0).getGivenName().setValue(encounter.getProvider().getGivenName());
		orc.getOrderingProvider(0).getIDNumber().setValue(encounter.getProvider().getId().toString());
		
		orc.getOrderControlCodeReason().getIdentifier().setValue("");
		
		orc.getEnteredBy(0).getFamilyName().getSurname().setValue(encounter.getCreator().getFamilyName());
		orc.getEnteredBy(0).getGivenName().setValue(encounter.getCreator().getGivenName());
		orc.getEnteredBy(0).getIDNumber().setValue(encounter.getCreator().getId().toString());
		
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
	}
	
	private static void mapToOBRs(ORU_R01 r01, List<Encounter> encounterList) throws Exception{
		int orderObsCount = 0;
		for(Encounter e : encounterList) {
			OBR obr = r01.getPATIENT_RESULT().getORDER_OBSERVATION(orderObsCount).getOBR();
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
	
	private void mapToOBX(OBX obx, Obs obs) throws HL7Exception, DataTypeException {
		
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
	}
	
}

package org.openmrs.module.hl7output.custommodel.v25.helper;

import org.openmrs.module.hl7output.custommodel.v25.group.ZPT_ZP1_ENCOUNTER;

import ca.uhn.hl7v2.parser.ModelClassFactory;

public class AbstractMessage extends ca.uhn.hl7v2.model.AbstractMessage {

	public AbstractMessage(ModelClassFactory theFactory) {
		super(theFactory);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7593252699087142749L;

	public ZPT_ZP1_ENCOUNTER removeRepetition(String string, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

	public ZPT_ZP1_ENCOUNTER insertRepetition(String string, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

	public void insertRepetition(String string, ZPT_ZP1_ENCOUNTER structure,
			int rep) {
		// TODO Auto-generated method stub
	}
}

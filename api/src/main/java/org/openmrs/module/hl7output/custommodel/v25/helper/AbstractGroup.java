package org.openmrs.module.hl7output.custommodel.v25.helper;

import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class AbstractGroup extends ca.uhn.hl7v2.model.AbstractGroup {

	protected AbstractGroup(Group parent, ModelClassFactory factory) {
		super(parent, factory);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3258722859130870840L;

	public void insertRepetition(String string, Object structure, int rep) {
		// TODO Auto-generated method stub
		
	}

	public Object insertRepetition(String string, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object removeRepetition(String string, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

}

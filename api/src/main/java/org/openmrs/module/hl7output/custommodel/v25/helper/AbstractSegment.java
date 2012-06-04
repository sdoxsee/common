package org.openmrs.module.hl7output.custommodel.v25.helper;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.v25.datatype.ID;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class AbstractSegment extends ca.uhn.hl7v2.model.AbstractSegment {

	public AbstractSegment(Group parent, ModelClassFactory factory) {
		super(parent, factory);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5880952508689798675L;

	protected void add(Class<?> class1, boolean b, int i, int j,
			Object[] objects, String string) throws HL7Exception {
				// TODO Auto-generated method stub
				add(class1, b, i, j, objects);
			}

	public Object insertRepetition(int i, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object removeRepetition(int i, int rep) {
		// TODO Auto-generated method stub
		return null;
	}

}

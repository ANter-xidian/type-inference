/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;

public abstract class VariableBinding extends Binding {
    
	public int modifiers;
	public TypeBinding type;
	public char[] name;
	protected Constant constant;
	public int id; // for flow-analysis (position in flowInfo bit vector)
	public long tagBits;

	public VariableBinding(char[] name, TypeBinding type, int modifiers, Constant constant) {
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
		this.constant = constant;
	}
	
	public Constant constant() {
		return this.constant;
	}
	
	public final boolean isBlankFinal(){
		return (modifiers & ExtraCompilerModifiers.AccBlankFinal) != 0;
	}
	/* Answer true if the receiver is final and cannot be changed
	*/
	
	public final boolean isFinal() {
		return (modifiers & ClassFileConstants.AccFinal) != 0;
	}
	public char[] readableName() {
		return name;
	}
	public void setConstant(Constant constant) {
		this.constant = constant;
	}
	public String toString() {
		String s = (type != null) ? type.debugName() : "UNDEFINED TYPE"; //$NON-NLS-1$
		s += " "; //$NON-NLS-1$
		s += (name != null) ? new String(name) : "UNNAMED FIELD"; //$NON-NLS-1$
		return s;
	}
}

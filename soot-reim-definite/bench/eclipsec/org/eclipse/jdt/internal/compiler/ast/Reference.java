/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public abstract class Reference extends Expression  {
/**
 * BaseLevelReference constructor comment.
 */
public Reference() {
	super();
}
public abstract FlowInfo analyseAssignment(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Assignment assignment, boolean isCompound);

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}
public FieldBinding fieldBinding() {
	//this method should be sent one FIELD-tagged references
	//  (ref.bits & BindingIds.FIELD != 0)()
	return null ;
}
public void fieldStore(CodeStream codeStream, FieldBinding fieldBinding, MethodBinding syntheticWriteAccessor, boolean valueRequired) {
	int pc = codeStream.position;
	if (fieldBinding.isStatic()) {
		if (valueRequired) {
			if ((fieldBinding.type == LongBinding) || (fieldBinding.type == DoubleBinding)) {
				codeStream.dup2();
			} else {
				codeStream.dup();
			}
		}
		if (syntheticWriteAccessor == null) {
			codeStream.putstatic(fieldBinding);
		} else {
			codeStream.invokestatic(syntheticWriteAccessor);
		}
	} else { // Stack:  [owner][new field value]  ---> [new field value][owner][new field value]
		if (valueRequired) {
			if ((fieldBinding.type == LongBinding) || (fieldBinding.type == DoubleBinding)) {
				codeStream.dup2_x1();
			} else {
				codeStream.dup_x1();
			}
		}
		if (syntheticWriteAccessor == null) {
			codeStream.putfield(fieldBinding);
		} else {
			codeStream.invokestatic(syntheticWriteAccessor);
		}
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
public abstract void generateAssignment(BlockScope currentScope, CodeStream codeStream, Assignment assignment, boolean valueRequired);

public abstract void generateCompoundAssignment(BlockScope currentScope, CodeStream codeStream, Expression expression, int operator, int assignmentImplicitConversion, boolean valueRequired);

public abstract void generatePostIncrement(BlockScope currentScope, CodeStream codeStream, CompoundAssignment postIncrement, boolean valueRequired);

public int nullStatus(FlowInfo flowInfo) {

	if (this.constant != null && this.constant != Constant.NotAConstant)
		return FlowInfo.NON_NULL; // constant expression cannot be null
	
	LocalVariableBinding local = localVariableBinding();
	if (local != null) {
		if (flowInfo.isDefinitelyNull(local))
			return FlowInfo.NULL;
		if (flowInfo.isDefinitelyNonNull(local))
			return FlowInfo.NON_NULL;
	}	
	return FlowInfo.UNKNOWN;
}

}

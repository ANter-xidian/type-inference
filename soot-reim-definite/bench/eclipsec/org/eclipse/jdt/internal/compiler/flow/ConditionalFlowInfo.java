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
package org.eclipse.jdt.internal.compiler.flow;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;

/**
 * Record conditional initialization status during definite assignment analysis
 *
 */
public class ConditionalFlowInfo extends FlowInfo {
	
	public FlowInfo initsWhenTrue;
	public FlowInfo initsWhenFalse;
	
	ConditionalFlowInfo(FlowInfo initsWhenTrue, FlowInfo initsWhenFalse){
		
		this.initsWhenTrue = initsWhenTrue;
		this.initsWhenFalse = initsWhenFalse; 
	}
	
	public FlowInfo addInitializationsFrom(FlowInfo otherInits) {
		
		this.initsWhenTrue.addInitializationsFrom(otherInits);
		this.initsWhenFalse.addInitializationsFrom(otherInits);
		return (/*@OwnPar*/ /*@NoRep*/ ConditionalFlowInfo)this;
	}
	
	public FlowInfo addPotentialInitializationsFrom(FlowInfo otherInits) {
		
		this.initsWhenTrue.addPotentialInitializationsFrom(otherInits);
		this.initsWhenFalse.addPotentialInitializationsFrom(otherInits);
		return (/*@OwnPar*/ /*@NoRep*/ ConditionalFlowInfo)this;
	}
	
	public FlowInfo asNegatedCondition() {
		
		FlowInfo extra = initsWhenTrue;
		initsWhenTrue = initsWhenFalse;
		initsWhenFalse = extra;
		return (/*@OwnPar*/ /*@NoRep*/ ConditionalFlowInfo)this;
	}

	public FlowInfo copy() {
		
		return new ConditionalFlowInfo(initsWhenTrue.copy(), initsWhenFalse.copy());
	}
	
	public FlowInfo initsWhenFalse() {
		
		return initsWhenFalse;
	}
	
	public FlowInfo initsWhenTrue() {
		
		return initsWhenTrue;
	}
	
	/**
	 * Check status of definite assignment for a field.
	 */
	public boolean isDefinitelyAssigned(FieldBinding field) {
		
		return initsWhenTrue.isDefinitelyAssigned(field) 
				&& initsWhenFalse.isDefinitelyAssigned(field);
	}
	
	/**
	 * Check status of definite assignment for a local variable.
	 */
	public boolean isDefinitelyAssigned(LocalVariableBinding local) {
		
		return initsWhenTrue.isDefinitelyAssigned(local) 
				&& initsWhenFalse.isDefinitelyAssigned(local);
	}
	
	/**
	 * Check status of definite non-null assignment for a field.
	 */
	public boolean isDefinitelyNonNull(FieldBinding field) {
		
		return initsWhenTrue.isDefinitelyNonNull(field) 
				&& initsWhenFalse.isDefinitelyNonNull(field);
	}

	/**
	 * Check status of definite non-null assignment for a local variable.
	 */
	public boolean isDefinitelyNonNull(LocalVariableBinding local) {
		
		return initsWhenTrue.isDefinitelyNonNull(local) 
				&& initsWhenFalse.isDefinitelyNonNull(local);
	}
	
	/**
	 * Check status of definite null assignment for a field.
	 */
	public boolean isDefinitelyNull(FieldBinding field) {
		
		return initsWhenTrue.isDefinitelyNull(field) 
				&& initsWhenFalse.isDefinitelyNull(field);
	}

	/**
	 * Check status of definite null assignment for a local variable.
	 */
	public boolean isDefinitelyNull(LocalVariableBinding local) {
		
		return initsWhenTrue.isDefinitelyNull(local) 
				&& initsWhenFalse.isDefinitelyNull(local);
	}

	public int reachMode(){
		return unconditionalInits().reachMode();
	}
	
	public boolean isReachable(){
		
		return unconditionalInits().isReachable();	
		//should maybe directly be: false
	}
	
	/**
	 * Check status of potential assignment for a field.
	 */
	public boolean isPotentiallyAssigned(FieldBinding field) {
		
		return initsWhenTrue.isPotentiallyAssigned(field) 
				|| initsWhenFalse.isPotentiallyAssigned(field);
	}
	
	/**
	 * Check status of potential assignment for a local variable.
	 */
	public boolean isPotentiallyAssigned(LocalVariableBinding local) {
		
		return initsWhenTrue.isPotentiallyAssigned(local) 
				|| initsWhenFalse.isPotentiallyAssigned(local);
	}
	
	/**
	 * Record a field got definitely assigned.
	 */
	public void markAsDefinitelyAssigned(FieldBinding field) {
		
		initsWhenTrue.markAsDefinitelyAssigned(field);
		initsWhenFalse.markAsDefinitelyAssigned(field);	
	}
	
	/**
	 * Record a field got definitely assigned.
	 */
	public void markAsDefinitelyAssigned(LocalVariableBinding local) {
		
		initsWhenTrue.markAsDefinitelyAssigned(local);
		initsWhenFalse.markAsDefinitelyAssigned(local);	
	}
	
	/**
	 * Record a field got definitely assigned to non-null value.
	 */
	public void markAsDefinitelyNonNull(FieldBinding field) {
		
		initsWhenTrue.markAsDefinitelyNonNull(field);
		initsWhenFalse.markAsDefinitelyNonNull(field);	
	}
	
	/**
	 * Record a field got definitely assigned to non-null value
	 */
	public void markAsDefinitelyNonNull(LocalVariableBinding local) {
		
		initsWhenTrue.markAsDefinitelyNonNull(local);
		initsWhenFalse.markAsDefinitelyNonNull(local);	
	}

	/**
	 * Record a field got definitely assigned to null.
	 */
	public void markAsDefinitelyNull(FieldBinding field) {
		
		initsWhenTrue.markAsDefinitelyNull(field);
		initsWhenFalse.markAsDefinitelyNull(field);	
	}
	
	/**
	 * Record a field got definitely assigned to null.
	 */
	public void markAsDefinitelyNull(LocalVariableBinding local) {
		
		initsWhenTrue.markAsDefinitelyNull(local);
		initsWhenFalse.markAsDefinitelyNull(local);	
	}

	/**
	 * Clear the initialization info for a field
	 */
	public void markAsDefinitelyNotAssigned(FieldBinding field) {
		
		initsWhenTrue.markAsDefinitelyNotAssigned(field);
		initsWhenFalse.markAsDefinitelyNotAssigned(field);	
	}
	
	/**
	 * Clear the initialization info for a local variable
	 */
	public void markAsDefinitelyNotAssigned(LocalVariableBinding local) {
		
		initsWhenTrue.markAsDefinitelyNotAssigned(local);
		initsWhenFalse.markAsDefinitelyNotAssigned(local);	
	}
	
	public FlowInfo setReachMode(int reachMode) {
		
		initsWhenTrue.setReachMode(reachMode);
		initsWhenFalse.setReachMode(reachMode);
		return (/*@OwnPar*/ /*@NoRep*/ ConditionalFlowInfo)this;
	}
	
	/**
	 * Converts conditional receiver into inconditional one, updated in the following way: <ul>
	 * <li> intersection of definitely assigned variables, 
	 * <li> union of potentially assigned variables.
	 * </ul>
	 */
	public UnconditionalFlowInfo mergedWith(UnconditionalFlowInfo otherInits) {
		
		return unconditionalInits().mergedWith(otherInits);
	}
	
	public String toString() {
		
		return "FlowInfo<true: " + initsWhenTrue.toString() + ", false: " + initsWhenFalse.toString() + ">"; //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
	}
	
	public UnconditionalFlowInfo unconditionalInits() {
		
		return initsWhenTrue.unconditionalInits().copy()
				.mergedWith(initsWhenFalse.unconditionalInits());
	}
}

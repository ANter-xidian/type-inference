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
package org.eclipse.jdt.internal.compiler.flow;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

/**
 * Record initialization status during definite assignment analysis
 *
 * No caching of pre-allocated instances.
 */
public class UnconditionalFlowInfo extends FlowInfo {

	
	public long definiteInits;
	public long potentialInits;
	public long extraDefiniteInits[];
	public long extraPotentialInits[];
	
	public long definiteNulls;
	public long definiteNonNulls;
	public long extraDefiniteNulls[];
	public long extraDefiniteNonNulls[];

	public int reachMode; // by default

	public int maxFieldCount;
	
	// Constants
	public static final int BitCacheSize = 64; // 64 bits in a long.

	UnconditionalFlowInfo() {
		this.reachMode = REACHABLE;
	}

	// unions of both sets of initialization - used for try/finally
	public FlowInfo addInitializationsFrom(FlowInfo inits) {

		if (this == DEAD_END)
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;

		UnconditionalFlowInfo otherInits = inits.unconditionalInits();	
		if (otherInits == DEAD_END)
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
			
		// union of definitely assigned variables, 
		definiteInits |= otherInits.definiteInits;
		// union of potentially set ones
		potentialInits |= otherInits.potentialInits;
	
		// union of definitely null variables, 
		definiteNulls = (definiteNulls | otherInits.definiteNulls) & ~otherInits.definiteNonNulls;
		// union of definitely non null variables,
		definiteNonNulls = (definiteNonNulls | otherInits.definiteNonNulls) & ~otherInits.definiteNulls;
		// fix-up null/non-null infos since cannot overlap: <defN1:0,defNoN1:1>  + <defN2:1,defNoN2:0>  --> <defN:0,defNon:0>

		// treating extra storage
		if (extraDefiniteInits != null) {
			if (otherInits.extraDefiniteInits != null) {
				// both sides have extra storage
				int i = 0, length, otherLength;
				if ((length = extraDefiniteInits.length) < (otherLength = otherInits.extraDefiniteInits.length)) {
					// current storage is shorter -> grow current (could maybe reuse otherInits extra storage?)
					System.arraycopy(extraDefiniteInits, 0, (extraDefiniteInits = new long[otherLength]), 0, length);
					System.arraycopy(extraPotentialInits, 0, (extraPotentialInits = new long[otherLength]), 0, length);
					System.arraycopy(extraDefiniteNulls, 0, (extraDefiniteNulls = new long[otherLength]), 0, length);
					System.arraycopy(extraDefiniteNonNulls, 0, (extraDefiniteNonNulls = new long[otherLength]), 0, length);					
					for (; i < length; i++) {
						extraDefiniteInits[i] |= otherInits.extraDefiniteInits[i];
						extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						extraDefiniteNulls[i] = (extraDefiniteNulls[i] | otherInits.extraDefiniteNulls[i]) & ~otherInits.extraDefiniteNonNulls[i];
						extraDefiniteNonNulls[i] = (extraDefiniteNonNulls[i] | otherInits.extraDefiniteNonNulls[i]) & ~otherInits.extraDefiniteNulls[i];
					}
					for (; i < otherLength; i++) {
						extraPotentialInits[i] = otherInits.extraPotentialInits[i];
					}
				} else {
					// current storage is longer
					for (; i < otherLength; i++) {
						extraDefiniteInits[i] |= otherInits.extraDefiniteInits[i];
						extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						extraDefiniteNulls[i] = (extraDefiniteNulls[i] | otherInits.extraDefiniteNulls[i]) & ~otherInits.extraDefiniteNonNulls[i];
						extraDefiniteNonNulls[i] = (extraDefiniteNonNulls[i] | otherInits.extraDefiniteNonNulls[i]) & ~otherInits.extraDefiniteNulls[i];
					}
					for (; i < length; i++) {
						extraDefiniteInits[i] = 0;
						extraDefiniteNulls[i] = 0;
						extraDefiniteNonNulls[i] = 0;
					}
				}
			} else {
				// no extra storage on otherInits
			}
		} else
			if (otherInits.extraDefiniteInits != null) {
				// no storage here, but other has extra storage.
				int otherLength;
				System.arraycopy(otherInits.extraDefiniteInits, 0, (extraDefiniteInits = new long[otherLength = otherInits.extraDefiniteInits.length]), 0, otherLength);			
				System.arraycopy(otherInits.extraPotentialInits, 0, (extraPotentialInits = new long[otherLength]), 0, otherLength);
				System.arraycopy(otherInits.extraDefiniteNulls, 0, (extraDefiniteNulls = new long[otherLength]), 0, otherLength);			
				System.arraycopy(otherInits.extraDefiniteNonNulls, 0, (extraDefiniteNonNulls = new long[otherLength]), 0, otherLength);			
			}
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}

	// unions of both sets of initialization - used for try/finally
	public FlowInfo addPotentialInitializationsFrom(FlowInfo inits) {
	
		if (this == DEAD_END){
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
		}

		UnconditionalFlowInfo otherInits = inits.unconditionalInits();
		if (otherInits == DEAD_END){
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
		}
		// union of potentially set ones
		this.potentialInits |= otherInits.potentialInits;
		// also merge null check information (affected by potential inits)
		this.definiteNulls &= otherInits.definiteNulls;
		this.definiteNonNulls &= otherInits.definiteNonNulls;
	
		// treating extra storage
		if (this.extraDefiniteInits != null) {
			if (otherInits.extraDefiniteInits != null) {
				// both sides have extra storage
				int i = 0, length, otherLength;
				if ((length = this.extraDefiniteInits.length) < (otherLength = otherInits.extraDefiniteInits.length)) {
					// current storage is shorter -> grow current (could maybe reuse otherInits extra storage?)
					System.arraycopy(this.extraDefiniteInits, 0, (this.extraDefiniteInits = new long[otherLength]), 0, length);
					System.arraycopy(this.extraPotentialInits, 0, (this.extraPotentialInits = new long[otherLength]), 0, length);
					System.arraycopy(this.extraDefiniteNulls, 0, (this.extraDefiniteNulls = new long[otherLength]), 0, length);
					System.arraycopy(this.extraDefiniteNonNulls, 0, (this.extraDefiniteNonNulls = new long[otherLength]), 0, length);
					while (i < length) {
						this.extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						this.extraDefiniteNulls[i] &= otherInits.extraDefiniteNulls[i];
						this.extraDefiniteNonNulls[i] &= otherInits.extraDefiniteNonNulls[i++];
					}
					while (i < otherLength) {
						this.extraPotentialInits[i] = otherInits.extraPotentialInits[i];
						this.extraDefiniteNulls[i] &= otherInits.extraDefiniteNulls[i];
						this.extraDefiniteNonNulls[i] &= otherInits.extraDefiniteNonNulls[i++];
					}
				} else {
					// current storage is longer
					while (i < otherLength) {
						this.extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						this.extraDefiniteNulls[i] &= otherInits.extraDefiniteNulls[i];
						this.extraDefiniteNonNulls[i] &= otherInits.extraDefiniteNonNulls[i++];
					}
				}
			}
		} else
			if (otherInits.extraDefiniteInits != null) {
				// no storage here, but other has extra storage.
				int otherLength;
				this.extraDefiniteInits = new long[otherLength = otherInits.extraDefiniteInits.length];			
				System.arraycopy(otherInits.extraPotentialInits, 0, (this.extraPotentialInits = new long[otherLength]), 0, otherLength);
				this.extraDefiniteNulls = new long[otherLength];			
				this.extraDefiniteNonNulls = new long[otherLength];			
			}
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}

	/**
	 * Answers a copy of the current instance
	 */
	public FlowInfo copy() {
		
		// do not clone the DeadEnd
		if (this == DEAD_END)
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	
		// look for an unused preallocated object
		UnconditionalFlowInfo copy = new UnconditionalFlowInfo();
	
		// copy slots
		copy.definiteInits = this.definiteInits;
		copy.potentialInits = this.potentialInits;
		copy.definiteNulls = this.definiteNulls;
		copy.definiteNonNulls = this.definiteNonNulls;
		copy.reachMode = this.reachMode;
		copy.maxFieldCount = this.maxFieldCount;
		
		if (this.extraDefiniteInits != null) {
			int length;
			System.arraycopy(this.extraDefiniteInits, 0, (copy.extraDefiniteInits = new long[length = extraDefiniteInits.length]), 0, length);
			System.arraycopy(this.extraPotentialInits, 0, (copy.extraPotentialInits = new long[length]), 0, length);
			System.arraycopy(this.extraDefiniteNulls, 0, (copy.extraDefiniteNulls = new long[length]), 0, length);
			System.arraycopy(this.extraDefiniteNonNulls, 0, (copy.extraDefiniteNonNulls = new long[length]), 0, length);
		}
		return copy;
	}
	
	public UnconditionalFlowInfo discardFieldInitializations(){
		
		int limit = this.maxFieldCount;
		
		if (limit < BitCacheSize) {
			long mask = (1L << limit)-1;
			this.definiteInits &= ~mask;
			this.potentialInits &= ~mask;
			this.definiteNulls &= ~mask;
			this.definiteNonNulls &= ~mask;
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
		} 

		this.definiteInits = 0;
		this.potentialInits = 0;
		this.definiteNulls = 0;
		this.definiteNonNulls = 0;
		
		// use extra vector
		if (extraDefiniteInits == null) {
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this; // if vector not yet allocated, then not initialized
		}
		int vectorIndex, length = this.extraDefiniteInits.length;
		if ((vectorIndex = (limit / BitCacheSize) - 1) >= length) {
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this; // not enough room yet
		}
		for (int i = 0; i < vectorIndex; i++) {
			this.extraDefiniteInits[i] = 0L;
			this.extraPotentialInits[i] = 0L;
			this.extraDefiniteNulls[i] = 0L;
			this.extraDefiniteNonNulls[i] = 0L;
		}
		long mask = (1L << (limit % BitCacheSize))-1;
		this.extraDefiniteInits[vectorIndex] &= ~mask;
		this.extraPotentialInits[vectorIndex] &= ~mask;
		this.extraDefiniteNulls[vectorIndex] &= ~mask;
		this.extraDefiniteNonNulls[vectorIndex] &= ~mask;
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}

	public UnconditionalFlowInfo discardNonFieldInitializations(){
		
		int limit = this.maxFieldCount;
		
		if (limit < BitCacheSize) {
			long mask = (1L << limit)-1;
			this.definiteInits &= mask;
			this.potentialInits &= mask;
			this.definiteNulls &= mask;
			this.definiteNonNulls &= mask;
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
		} 
		// use extra vector
		if (extraDefiniteInits == null) {
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this; // if vector not yet allocated, then not initialized
		}
		int vectorIndex, length = this.extraDefiniteInits.length;
		if ((vectorIndex = (limit / BitCacheSize) - 1) >= length) {
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this; // not enough room yet
		}
		long mask = (1L << (limit % BitCacheSize))-1;
		this.extraDefiniteInits[vectorIndex] &= mask;
		this.extraPotentialInits[vectorIndex] &= mask;
		this.extraDefiniteNulls[vectorIndex] &= mask;
		this.extraDefiniteNonNulls[vectorIndex] &= mask;
		for (int i = vectorIndex+1; i < length; i++) {
			this.extraDefiniteInits[i] = 0L;
			this.extraPotentialInits[i] = 0L;
			this.extraDefiniteNulls[i] = 0L;
			this.extraDefiniteNonNulls[i] = 0L;
		}
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}
	
	public UnconditionalFlowInfo discardNullRelatedInitializations(){
		
		this.definiteNulls = 0;
		this.definiteNonNulls = 0;
		
		int length = this.extraDefiniteInits == null ? 0 : this.extraDefiniteInits.length;
		for (int i = 0; i < length; i++) {
			this.extraDefiniteNulls[i] = 0L;
			this.extraDefiniteNonNulls[i] = 0L;
		}
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}

	public FlowInfo initsWhenFalse() {
		
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}
	
	public FlowInfo initsWhenTrue() {
		
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}
	
	/**
	 * Check status of definite assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private boolean isDefinitelyAssigned(int position) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// id is zero-based
		if (position < BitCacheSize) {
			return (definiteInits & (1L << position)) != 0; // use bits
		}
		// use extra vector
		if (extraDefiniteInits == null)
			return false; // if vector not yet allocated, then not initialized
		int vectorIndex;
		if ((vectorIndex = (position / BitCacheSize) - 1) >= extraDefiniteInits.length)
			return false; // if not enough room in vector, then not initialized 
		return ((extraDefiniteInits[vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
	}
	
	/**
	 * Check status of definite non-null assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private boolean isDefinitelyNonNull(int position) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// id is zero-based
		if (position < BitCacheSize) {
			return (definiteNonNulls & (1L << position)) != 0; // use bits
		}
		// use extra vector
		if (extraDefiniteNonNulls == null)
			return false; // if vector not yet allocated, then not initialized
		int vectorIndex;
		if ((vectorIndex = (position / BitCacheSize) - 1) >= extraDefiniteNonNulls.length)
			return false; // if not enough room in vector, then not initialized 
		return ((extraDefiniteNonNulls[vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
	}

	/**
	 * Check status of definite null assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private boolean isDefinitelyNull(int position) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// id is zero-based
		if (position < BitCacheSize) {
			return (definiteNulls & (1L << position)) != 0; // use bits
		}
		// use extra vector
		if (extraDefiniteNulls == null)
			return false; // if vector not yet allocated, then not initialized
		int vectorIndex;
		if ((vectorIndex = (position / BitCacheSize) - 1) >= extraDefiniteNulls.length)
			return false; // if not enough room in vector, then not initialized 
		return ((extraDefiniteNulls[vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
	}

	/**
	 * Check status of definite assignment for a field.
	 */
	final public boolean isDefinitelyAssigned(FieldBinding field) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)  
			return true;
		return isDefinitelyAssigned(field.id); 
	}
	
	/**
	 * Check status of definite assignment for a local.
	 */
	final public boolean isDefinitelyAssigned(LocalVariableBinding local) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)
			return true;

		// final constants are inlined, and thus considered as always initialized
		if (local.constant() != Constant.NotAConstant) {
			return true;
		}
		return isDefinitelyAssigned(local.id + maxFieldCount);
	}
	
	/**
	 * Check status of definite non-null assignment for a field.
	 */
	final public boolean isDefinitelyNonNull(FieldBinding field) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)  
			return false;
		return isDefinitelyNonNull(field.id); 
	}
	
	/**
	 * Check status of definite non-null assignment for a local.
	 */
	final public boolean isDefinitelyNonNull(LocalVariableBinding local) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)
			return false;
		// final constants are inlined, and thus considered as always initialized
		if (local.constant() != Constant.NotAConstant) {
			return true;
		}
		return isDefinitelyNonNull(local.id + maxFieldCount);
	}

	/**
	 * Check status of definite null assignment for a field.
	 */
	final public boolean isDefinitelyNull(FieldBinding field) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)  
			return false;
		return isDefinitelyNull(field.id); 
	}
	
	/**
	 * Check status of definite null assignment for a local.
	 */
	final public boolean isDefinitelyNull(LocalVariableBinding local) {
		
		// Dependant of CodeStream.isDefinitelyAssigned(..)
		// We do not want to complain in unreachable code
		if ((this.reachMode & UNREACHABLE) != 0)
			return false;
		return isDefinitelyNull(local.id + maxFieldCount);
	}

	public boolean isReachable() {
		
		return this.reachMode == REACHABLE;
	}
	
	/**
	 * Check status of potential assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo3:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private boolean isPotentiallyAssigned(int position) {
		
		// id is zero-based
		if (position < BitCacheSize) {
			// use bits
			return (potentialInits & (1L << position)) != 0;
		}
		// use extra vector
		if (extraPotentialInits == null)
			return false; // if vector not yet allocated, then not initialized
		int vectorIndex;
		if ((vectorIndex = (position / BitCacheSize) - 1) >= extraPotentialInits.length)
			return false; // if not enough room in vector, then not initialized 
		return ((extraPotentialInits[vectorIndex]) & (1L << (position % BitCacheSize))) != 0;
	}
	
	/**
	 * Check status of definite assignment for a field.
	 */
	final public boolean isPotentiallyAssigned(FieldBinding field) {
		
		return isPotentiallyAssigned(field.id); 
	}
	
	/**
	 * Check status of potential assignment for a local.
	 */
	final public boolean isPotentiallyAssigned(LocalVariableBinding local) {
		
		// final constants are inlined, and thus considered as always initialized
		if (local.constant() != Constant.NotAConstant) {
			return true;
		}
		return isPotentiallyAssigned(local.id + maxFieldCount);
	}
	
	/**
	 * Record a definite assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private void markAsDefinitelyAssigned(int position) {
		
		if (this != DEAD_END) {
	
			// position is zero-based
			if (position < BitCacheSize) {
				// use bits
				long mask;
				definiteInits |= (mask = 1L << position);
				potentialInits |= mask;
				definiteNulls &= ~mask;
				definiteNonNulls &= ~mask;
			} else {
				// use extra vector
				int vectorIndex = (position / BitCacheSize) - 1;
				if (extraDefiniteInits == null) {
					int length;
					extraDefiniteInits = new long[length = vectorIndex + 1];
					extraPotentialInits = new long[length];
					extraDefiniteNulls = new long[length];
					extraDefiniteNonNulls = new long[length];
				} else {
					int oldLength; // might need to grow the arrays
					if (vectorIndex >= (oldLength = extraDefiniteInits.length)) {
						System.arraycopy(extraDefiniteInits, 0, (extraDefiniteInits = new long[vectorIndex + 1]), 0, oldLength);
						System.arraycopy(extraPotentialInits, 0, (extraPotentialInits = new long[vectorIndex + 1]), 0, oldLength);
						System.arraycopy(extraDefiniteNulls, 0, (extraDefiniteNulls = new long[vectorIndex + 1]), 0, oldLength);
						System.arraycopy(extraDefiniteNonNulls, 0, (extraDefiniteNonNulls = new long[vectorIndex + 1]), 0, oldLength);
					}
				}
				long mask;
				extraDefiniteInits[vectorIndex] |= (mask = 1L << (position % BitCacheSize));
				extraPotentialInits[vectorIndex] |= mask;
				extraDefiniteNulls[vectorIndex] &= ~mask;
				extraDefiniteNonNulls[vectorIndex] &= ~mask;
			}
		}
	}
	
	/**
	 * Record a field got definitely assigned.
	 */
	public void markAsDefinitelyAssigned(FieldBinding field) {
		if (this != DEAD_END)
			markAsDefinitelyAssigned(field.id);
	}
	
	/**
	 * Record a local got definitely assigned.
	 */
	public void markAsDefinitelyAssigned(LocalVariableBinding local) {
		if (this != DEAD_END)
			markAsDefinitelyAssigned(local.id + maxFieldCount);
	}

	/**
	 * Record a definite non-null assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private void markAsDefinitelyNonNull(int position) {
		
		if (this != DEAD_END) {
	
			// position is zero-based
			if (position < BitCacheSize) {
				// use bits
				long mask;
				definiteNonNulls |= (mask = 1L << position);
				definiteNulls &= ~mask;
			} else {
				// use extra vector
				int vectorIndex = (position / BitCacheSize) - 1;
				long mask;
				extraDefiniteNonNulls[vectorIndex] |= (mask = 1L << (position % BitCacheSize));
				extraDefiniteNulls[vectorIndex] &= ~mask;
			}
		}
	}

	/**
	 * Record a field got definitely assigned to non-null value.
	 */
	public void markAsDefinitelyNonNull(FieldBinding field) {
		if (this != DEAD_END)
			markAsDefinitelyNonNull(field.id);
	}
	
	/**
	 * Record a local got definitely assigned to non-null value.
	 */
	public void markAsDefinitelyNonNull(LocalVariableBinding local) {
		if (this != DEAD_END)
			markAsDefinitelyNonNull(local.id + maxFieldCount);
	}

	/**
	 * Record a definite null assignment at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private void markAsDefinitelyNull(int position) {
		
		if (this != DEAD_END) {
	
			// position is zero-based
			if (position < BitCacheSize) {
				// use bits
				long mask;
				definiteNulls |= (mask = 1L << position);
				definiteNonNulls &= ~mask;
			} else {
				// use extra vector
				int vectorIndex = (position / BitCacheSize) - 1;
				long mask;
				extraDefiniteNulls[vectorIndex] |= (mask = 1L << (position % BitCacheSize));
				extraDefiniteNonNulls[vectorIndex] &= ~mask;
			}
		}
	}

	/**
	 * Record a field got definitely assigned to null.
	 */
	public void markAsDefinitelyNull(FieldBinding field) {
		if (this != DEAD_END)
			markAsDefinitelyAssigned(field.id);
	}
	
	/**
	 * Record a local got definitely assigned to null.
	 */
	public void markAsDefinitelyNull(LocalVariableBinding local) {
		if (this != DEAD_END)
			markAsDefinitelyNull(local.id + maxFieldCount);
	}
	
	/**
	 * Clear initialization information at a given position.
	 * It deals with the dual representation of the InitializationInfo2:
	 * bits for the first 64 entries, then an array of booleans.
	 */
	final private void markAsDefinitelyNotAssigned(int position) {
		if (this != DEAD_END) {
	
			// position is zero-based
			if (position < BitCacheSize) {
				// use bits
				long mask;
				definiteInits &= ~(mask = 1L << position);
				potentialInits &= ~mask;
				definiteNulls &= ~mask;
				definiteNonNulls &= ~mask;
			} else {
				// use extra vector
				int vectorIndex = (position / BitCacheSize) - 1;
				if (extraDefiniteInits == null) {
					return; // nothing to do, it was not yet set 
				}
				// might need to grow the arrays
				if (vectorIndex >= extraDefiniteInits.length) {
					return; // nothing to do, it was not yet set 
				}
				long mask;
				extraDefiniteInits[vectorIndex] &= ~(mask = 1L << (position % BitCacheSize));
				extraPotentialInits[vectorIndex] &= ~mask;
				extraDefiniteNulls[vectorIndex] &= ~mask;
				extraDefiniteNonNulls[vectorIndex] &= ~mask;
			}
		}
	}
	
	/**
	 * Clear the initialization info for a field
	 */
	public void markAsDefinitelyNotAssigned(FieldBinding field) {
		
		if (this != DEAD_END)
			markAsDefinitelyNotAssigned(field.id);
	}
	
	/**
	 * Clear the initialization info for a local variable
	 */
	
	public void markAsDefinitelyNotAssigned(LocalVariableBinding local) {
		
		if (this != DEAD_END)
			markAsDefinitelyNotAssigned(local.id + maxFieldCount);
	}
		
	/**
	 * Returns the receiver updated in the following way: <ul>
	 * <li> intersection of definitely assigned variables, 
	 * <li> union of potentially assigned variables.
	 * </ul>
	 */
	public UnconditionalFlowInfo mergedWith(UnconditionalFlowInfo otherInits) {
	
		if (this == DEAD_END) return otherInits;
		if (otherInits == DEAD_END) return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	
		if ((this.reachMode & UNREACHABLE) != (otherInits.reachMode & UNREACHABLE)){
			if ((this.reachMode & UNREACHABLE) != 0){
				return otherInits;
			} 
			return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
		}
		
		// if one branch is not fake reachable, then the merged one is reachable
		this.reachMode &= otherInits.reachMode;
	
		// intersection of definitely assigned variables, 
		this.definiteInits &= otherInits.definiteInits;
		// union of potentially set ones
		this.potentialInits |= otherInits.potentialInits;
		// intersection of definitely null variables, 
		this.definiteNulls &= otherInits.definiteNulls;
		// intersection of definitely non-null variables, 
		this.definiteNonNulls &= otherInits.definiteNonNulls;
	
		// treating extra storage
		if (this.extraDefiniteInits != null) {
			if (otherInits.extraDefiniteInits != null) {
				// both sides have extra storage
				int i = 0, length, otherLength;
				if ((length = this.extraDefiniteInits.length) < (otherLength = otherInits.extraDefiniteInits.length)) {
					// current storage is shorter -> grow current (could maybe reuse otherInits extra storage?)
					System.arraycopy(this.extraDefiniteInits, 0, (this.extraDefiniteInits = new long[otherLength]), 0, length);
					System.arraycopy(this.extraPotentialInits, 0, (this.extraPotentialInits = new long[otherLength]), 0, length);
					System.arraycopy(this.extraDefiniteNulls, 0, (this.extraDefiniteNulls = new long[otherLength]), 0, length);
					System.arraycopy(this.extraDefiniteNonNulls, 0, (this.extraDefiniteNonNulls = new long[otherLength]), 0, length);
					while (i < length) {
						this.extraDefiniteInits[i] &= otherInits.extraDefiniteInits[i];
						this.extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						this.extraDefiniteNulls[i] &= otherInits.extraDefiniteNulls[i];
						this.extraDefiniteNonNulls[i] &= otherInits.extraDefiniteNonNulls[i++];
					}
					while (i < otherLength) {
						this.extraPotentialInits[i] = otherInits.extraPotentialInits[i++];
					}
				} else {
					// current storage is longer
					while (i < otherLength) {
						this.extraDefiniteInits[i] &= otherInits.extraDefiniteInits[i];
						this.extraPotentialInits[i] |= otherInits.extraPotentialInits[i];
						this.extraDefiniteNulls[i] &= otherInits.extraDefiniteNulls[i];
						this.extraDefiniteNonNulls[i] &= otherInits.extraDefiniteNonNulls[i++];
					}
					while (i < length) {
						this.extraDefiniteInits[i] = 0;
						this.extraDefiniteNulls[i] = 0;
						this.extraDefiniteNonNulls[i++] = 0;
					}
				}
			} else {
				// no extra storage on otherInits
				int i = 0, length = this.extraDefiniteInits.length;
				while (i < length) {
					this.extraDefiniteInits[i] = 0;
					this.extraDefiniteNulls[i] = 0;
					this.extraDefiniteNonNulls[i++] = 0;
				}
			}
		} else
			if (otherInits.extraDefiniteInits != null) {
				// no storage here, but other has extra storage.
				int otherLength;
				this.extraDefiniteInits = new long[otherLength = otherInits.extraDefiniteInits.length];
				System.arraycopy(otherInits.extraPotentialInits, 0, (this.extraPotentialInits = new long[otherLength]), 0, otherLength);
				this.extraDefiniteNulls = new long[otherLength];
				this.extraDefiniteNonNulls = new long[otherLength];
			}
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}
	
	/*
	 * Answer the total number of fields in enclosing types of a given type
	 */
	static int numberOfEnclosingFields(ReferenceBinding type){
		
		int count = 0;
		type = type.enclosingType();
		while(type != null) {
			count += type.fieldCount();
			type = type.enclosingType();
		}
		return count;
	}
	
	public int reachMode(){
		return this.reachMode;
	}
	
	public FlowInfo setReachMode(int reachMode) {
		
		if (this == DEAD_END) return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this; // cannot modify DEAD_END
	
		// reset optional inits when becoming unreachable
		if ((this.reachMode & UNREACHABLE) == 0 && (reachMode & UNREACHABLE) != 0) {
			this.potentialInits = 0;
			if (this.extraPotentialInits != null){
				for (int i = 0, length = this.extraPotentialInits.length; i < length; i++){
					this.extraPotentialInits[i] = 0;
				}
			}
		}				
		this.reachMode = reachMode;
	
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}

	public String toString(){
		
		if (this == DEAD_END){
			return "FlowInfo.DEAD_END"; //$NON-NLS-1$
		}
		return "FlowInfo<def: "+ this.definiteInits //$NON-NLS-1$
			+", pot: " + this.potentialInits  //$NON-NLS-1$
			+ ", reachable:" + ((this.reachMode & UNREACHABLE) == 0) //$NON-NLS-1$
			+", defNull: " + this.definiteNulls  //$NON-NLS-1$
			+", defNonNull: " + this.definiteNonNulls  //$NON-NLS-1$
			+">"; //$NON-NLS-1$
	}
	
	public UnconditionalFlowInfo unconditionalInits() {
		
		// also see conditional inits, where it requests them to merge
		return (/*@OwnPar*/ /*@NoRep*/ UnconditionalFlowInfo)this;
	}
}

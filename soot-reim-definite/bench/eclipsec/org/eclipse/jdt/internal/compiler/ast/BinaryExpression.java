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

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class BinaryExpression extends OperatorExpression {

	public Expression left, right;
	public Constant optimizedBooleanConstant;

	public BinaryExpression(Expression left, Expression right, int operator) {

		this.left = left;
		this.right = right;
		this.bits |= operator << OperatorSHIFT; // encode operator
		this.sourceStart = left.sourceStart;
		this.sourceEnd = right.sourceEnd;
	}

	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		return right
			.analyseCode(
				currentScope,
				flowContext,
				left.analyseCode(currentScope, flowContext, flowInfo).unconditionalInits())
			.unconditionalInits();
	}

	public void computeConstant(BlockScope scope, int leftId, int rightId) {

		//compute the constant when valid
		if ((this.left.constant != Constant.NotAConstant)
			&& (this.right.constant != Constant.NotAConstant)) {
			try {
				this.constant =
					Constant.computeConstantOperation(
						this.left.constant,
						leftId,
						(this.bits & OperatorMASK) >> OperatorSHIFT,
						this.right.constant,
						rightId);
			} catch (ArithmeticException e) {
				this.constant = Constant.NotAConstant;
				// 1.2 no longer throws an exception at compile-time
				//scope.problemReporter().compileTimeConstantThrowsArithmeticException(this);
			}
		} else {
			this.constant = Constant.NotAConstant;
			//add some work for the boolean operators & |  
			this.optimizedBooleanConstant(
				leftId,
				(this.bits & OperatorMASK) >> OperatorSHIFT,
				rightId);
		}
	}

	public Constant optimizedBooleanConstant() {

		return this.optimizedBooleanConstant == null ? this.constant : this.optimizedBooleanConstant;
	}

	/**
	 * Code generation for a binary operation
	 */
	public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		int pc = codeStream.position;
		if (constant != Constant.NotAConstant) {
			if (valueRequired)
				codeStream.generateConstant(constant, implicitConversion);
			codeStream.recordPositionsFrom(pc, this.sourceStart);
			return;
		}
		switch ((bits & OperatorMASK) >> OperatorSHIFT) {
			case PLUS :
				switch (bits & ReturnTypeIDMASK) {
					case T_JavaLangString :
						codeStream.generateStringConcatenationAppend(currentScope, left, right);
						if (!valueRequired)
							codeStream.pop();
						break;
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.iadd();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.ladd();
						break;
					case T_double :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.dadd();
						break;
					case T_float :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.fadd();
						break;
				}
				break;
			case MINUS :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.isub();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.lsub();
						break;
					case T_double :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.dsub();
						break;
					case T_float :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.fsub();
						break;
				}
				break;
			case MULTIPLY :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.imul();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.lmul();
						break;
					case T_double :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.dmul();
						break;
					case T_float :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.fmul();
						break;
				}
				break;
			case DIVIDE :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, true);
						right.generateCode(currentScope, codeStream, true);
						codeStream.idiv();
						if (!valueRequired)
							codeStream.pop();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, true);
						right.generateCode(currentScope, codeStream, true);
						codeStream.ldiv();
						if (!valueRequired)
							codeStream.pop2();
						break;
					case T_double :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.ddiv();
						break;
					case T_float :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.fdiv();
						break;
				}
				break;
			case REMAINDER :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, true);
						right.generateCode(currentScope, codeStream, true);
						codeStream.irem();
						if (!valueRequired)
							codeStream.pop();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, true);
						right.generateCode(currentScope, codeStream, true);
						codeStream.lrem();
						if (!valueRequired)
							codeStream.pop2();
						break;
					case T_double :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.drem();
						break;
					case T_float :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.frem();
						break;
				}
				break;
			case AND :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						// 0 & x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_int)
							&& (left.constant.intValue() == 0)) {
							right.generateCode(currentScope, codeStream, false);
							if (valueRequired)
								codeStream.iconst_0();
						} else {
							// x & 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_int)
								&& (right.constant.intValue() == 0)) {
								left.generateCode(currentScope, codeStream, false);
								if (valueRequired)
									codeStream.iconst_0();
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.iand();
							}
						}
						break;
					case T_long :
						// 0 & x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_long)
							&& (left.constant.longValue() == 0L)) {
							right.generateCode(currentScope, codeStream, false);
							if (valueRequired)
								codeStream.lconst_0();
						} else {
							// x & 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_long)
								&& (right.constant.longValue() == 0L)) {
								left.generateCode(currentScope, codeStream, false);
								if (valueRequired)
									codeStream.lconst_0();
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.land();
							}
						}
						break;
					case T_boolean : // logical and
						generateLogicalAnd(currentScope, codeStream, valueRequired);
						break;
				}
				break;
			case OR :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						// 0 | x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_int)
							&& (left.constant.intValue() == 0)) {
							right.generateCode(currentScope, codeStream, valueRequired);
						} else {
							// x | 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_int)
								&& (right.constant.intValue() == 0)) {
								left.generateCode(currentScope, codeStream, valueRequired);
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.ior();
							}
						}
						break;
					case T_long :
						// 0 | x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_long)
							&& (left.constant.longValue() == 0L)) {
							right.generateCode(currentScope, codeStream, valueRequired);
						} else {
							// x | 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_long)
								&& (right.constant.longValue() == 0L)) {
								left.generateCode(currentScope, codeStream, valueRequired);
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.lor();
							}
						}
						break;
					case T_boolean : // logical or
						generateLogicalOr(currentScope, codeStream, valueRequired);
						break;
				}
				break;
			case XOR :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						// 0 ^ x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_int)
							&& (left.constant.intValue() == 0)) {
							right.generateCode(currentScope, codeStream, valueRequired);
						} else {
							// x ^ 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_int)
								&& (right.constant.intValue() == 0)) {
								left.generateCode(currentScope, codeStream, valueRequired);
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.ixor();
							}
						}
						break;
					case T_long :
						// 0 ^ x
						if ((left.constant != Constant.NotAConstant)
							&& (left.constant.typeID() == T_long)
							&& (left.constant.longValue() == 0L)) {
							right.generateCode(currentScope, codeStream, valueRequired);
						} else {
							// x ^ 0
							if ((right.constant != Constant.NotAConstant)
								&& (right.constant.typeID() == T_long)
								&& (right.constant.longValue() == 0L)) {
								left.generateCode(currentScope, codeStream, valueRequired);
							} else {
								left.generateCode(currentScope, codeStream, valueRequired);
								right.generateCode(currentScope, codeStream, valueRequired);
								if (valueRequired)
									codeStream.lxor();
							}
						}
						break;
					case T_boolean :
						generateLogicalXor(currentScope, 	codeStream, valueRequired);
						break;
				}
				break;
			case LEFT_SHIFT :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.ishl();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.lshl();
				}
				break;
			case RIGHT_SHIFT :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.ishr();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.lshr();
				}
				break;
			case UNSIGNED_RIGHT_SHIFT :
				switch (bits & ReturnTypeIDMASK) {
					case T_int :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.iushr();
						break;
					case T_long :
						left.generateCode(currentScope, codeStream, valueRequired);
						right.generateCode(currentScope, codeStream, valueRequired);
						if (valueRequired)
							codeStream.lushr();
				}
				break;
			case GREATER :
				Label falseLabel, endLabel;
				generateOptimizedGreaterThan(
					currentScope,
					codeStream,
					null,
					(falseLabel = new Label(codeStream)),
					valueRequired);
				if (valueRequired) {
					codeStream.iconst_1();
					if ((bits & IsReturnedValue) != 0) {
						codeStream.generateImplicitConversion(this.implicitConversion);
						codeStream.generateReturnBytecode((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this);
						falseLabel.place();
						codeStream.iconst_0();
					} else {
						codeStream.goto_(endLabel = new Label(codeStream));
						codeStream.decrStackSize(1);
						falseLabel.place();
						codeStream.iconst_0();
						endLabel.place();
					}
				}
				break;
			case GREATER_EQUAL :
				generateOptimizedGreaterThanOrEqual(
					currentScope,
					codeStream,
					null,
					(falseLabel = new Label(codeStream)),
					valueRequired);
				if (valueRequired) {
					codeStream.iconst_1();
					if ((bits & IsReturnedValue) != 0) {
						codeStream.generateImplicitConversion(this.implicitConversion);
						codeStream.generateReturnBytecode((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this);
						falseLabel.place();
						codeStream.iconst_0();
					} else {
						codeStream.goto_(endLabel = new Label(codeStream));
						codeStream.decrStackSize(1);
						falseLabel.place();
						codeStream.iconst_0();
						endLabel.place();
					}
				}
				break;
			case LESS :
				generateOptimizedLessThan(
					currentScope,
					codeStream,
					null,
					(falseLabel = new Label(codeStream)),
					valueRequired);
				if (valueRequired) {
					codeStream.iconst_1();
					if ((bits & IsReturnedValue) != 0) {
						codeStream.generateImplicitConversion(this.implicitConversion);
						codeStream.generateReturnBytecode((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this);
						falseLabel.place();
						codeStream.iconst_0();
					} else {
						codeStream.goto_(endLabel = new Label(codeStream));
						codeStream.decrStackSize(1);
						falseLabel.place();
						codeStream.iconst_0();
						endLabel.place();
					}
				}
				break;
			case LESS_EQUAL :
				generateOptimizedLessThanOrEqual(
					currentScope,
					codeStream,
					null,
					(falseLabel = new Label(codeStream)),
					valueRequired);
				if (valueRequired) {
					codeStream.iconst_1();
					if ((bits & IsReturnedValue) != 0) {
						codeStream.generateImplicitConversion(this.implicitConversion);
						codeStream.generateReturnBytecode((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this);
						falseLabel.place();
						codeStream.iconst_0();
					} else {
						codeStream.goto_(endLabel = new Label(codeStream));
						codeStream.decrStackSize(1);
						falseLabel.place();
						codeStream.iconst_0();
						endLabel.place();
					}
				}
		}
		if (valueRequired) {
			codeStream.generateImplicitConversion(implicitConversion);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	/**
	 * Boolean operator code generation
	 *	Optimized operations are: <, <=, >, >=, &, |, ^
	 */
	public void generateOptimizedBoolean(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {

		if ((constant != Constant.NotAConstant) && (constant.typeID() == T_boolean)) {
			super.generateOptimizedBoolean(
				currentScope,
				codeStream,
				trueLabel,
				falseLabel,
				valueRequired);
			return;
		}
		switch ((bits & OperatorMASK) >> OperatorSHIFT) {
			case LESS :
				generateOptimizedLessThan(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case LESS_EQUAL :
				generateOptimizedLessThanOrEqual(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case GREATER :
				generateOptimizedGreaterThan(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case GREATER_EQUAL :
				generateOptimizedGreaterThanOrEqual(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case AND :
				generateOptimizedLogicalAnd(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case OR :
				generateOptimizedLogicalOr(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
			case XOR :
				generateOptimizedLogicalXor(
					currentScope,
					codeStream,
					trueLabel,
					falseLabel,
					valueRequired);
				return;
		}
		super.generateOptimizedBoolean(
			currentScope,
			codeStream,
			trueLabel,
			falseLabel,
			valueRequired);
	}

	/**
	 * Boolean generation for >
	 */
	public void generateOptimizedGreaterThan(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {

		int promotedTypeID = (left.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4;
		// both sides got promoted in the same way
		if (promotedTypeID == T_int) {
			// 0 > x
			if ((left.constant != Constant.NotAConstant) && (left.constant.intValue() == 0)) {
				right.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.iflt(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifge(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
			// x > 0
			if ((right.constant != Constant.NotAConstant) && (right.constant.intValue() == 0)) {
				left.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifgt(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifle(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
		}
		// default comparison
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmpgt(trueLabel);
							break;
						case T_float :
							codeStream.fcmpl();
							codeStream.ifgt(trueLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifgt(trueLabel);
							break;
						case T_double :
							codeStream.dcmpl();
							codeStream.ifgt(trueLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				}
			} else {
				if (trueLabel == null) {
					// implicit falling through the TRUE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmple(falseLabel);
							break;
						case T_float :
							codeStream.fcmpl();
							codeStream.ifle(falseLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifle(falseLabel);
							break;
						case T_double :
							codeStream.dcmpl();
							codeStream.ifle(falseLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
	}

	/**
	 * Boolean generation for >=
	 */
	public void generateOptimizedGreaterThanOrEqual(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {

		int promotedTypeID = (left.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4;
		// both sides got promoted in the same way
		if (promotedTypeID == T_int) {
			// 0 >= x
			if ((left.constant != Constant.NotAConstant) && (left.constant.intValue() == 0)) {
				right.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifle(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifgt(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
			// x >= 0
			if ((right.constant != Constant.NotAConstant) && (right.constant.intValue() == 0)) {
				left.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifge(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.iflt(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
		}
		// default comparison
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmpge(trueLabel);
							break;
						case T_float :
							codeStream.fcmpl();
							codeStream.ifge(trueLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifge(trueLabel);
							break;
						case T_double :
							codeStream.dcmpl();
							codeStream.ifge(trueLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				}
			} else {
				if (trueLabel == null) {
					// implicit falling through the TRUE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmplt(falseLabel);
							break;
						case T_float :
							codeStream.fcmpl();
							codeStream.iflt(falseLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.iflt(falseLabel);
							break;
						case T_double :
							codeStream.dcmpl();
							codeStream.iflt(falseLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
	}

	/**
	 * Boolean generation for <
	 */
	public void generateOptimizedLessThan(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {

		int promotedTypeID = (left.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4;
		// both sides got promoted in the same way
		if (promotedTypeID == T_int) {
			// 0 < x
			if ((left.constant != Constant.NotAConstant) && (left.constant.intValue() == 0)) {
				right.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifgt(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifle(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);
				return;
			}
			// x < 0
			if ((right.constant != Constant.NotAConstant) && (right.constant.intValue() == 0)) {
				left.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.iflt(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifge(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);
				return;
			}
		}
		// default comparison
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmplt(trueLabel);
							break;
						case T_float :
							codeStream.fcmpg();
							codeStream.iflt(trueLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.iflt(trueLabel);
							break;
						case T_double :
							codeStream.dcmpg();
							codeStream.iflt(trueLabel);
					}
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);
					return;
				}
			} else {
				if (trueLabel == null) {
					// implicit falling through the TRUE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmpge(falseLabel);
							break;
						case T_float :
							codeStream.fcmpg();
							codeStream.ifge(falseLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifge(falseLabel);
							break;
						case T_double :
							codeStream.dcmpg();
							codeStream.ifge(falseLabel);
					}
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);
					return;
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
	}
	
	/**
	 * Boolean generation for <=
	 */
	public void generateOptimizedLessThanOrEqual(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {

		int promotedTypeID = (left.implicitConversion & IMPLICIT_CONVERSION_MASK) >> 4;
		// both sides got promoted in the same way
		if (promotedTypeID == T_int) {
			// 0 <= x
			if ((left.constant != Constant.NotAConstant) && (left.constant.intValue() == 0)) {
				right.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifge(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.iflt(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
			// x <= 0
			if ((right.constant != Constant.NotAConstant) && (right.constant.intValue() == 0)) {
				left.generateCode(currentScope, codeStream, valueRequired);
				if (valueRequired) {
					if (falseLabel == null) {
						if (trueLabel != null) {
							// implicitly falling through the FALSE case
							codeStream.ifle(trueLabel);
						}
					} else {
						if (trueLabel == null) {
							// implicitly falling through the TRUE case
							codeStream.ifgt(falseLabel);
						} else {
							// no implicit fall through TRUE/FALSE --> should never occur
						}
					}
				}
				// reposition the endPC
				codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				return;
			}
		}
		// default comparison
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmple(trueLabel);
							break;
						case T_float :
							codeStream.fcmpg();
							codeStream.ifle(trueLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifle(trueLabel);
							break;
						case T_double :
							codeStream.dcmpg();
							codeStream.ifle(trueLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				}
			} else {
				if (trueLabel == null) {
					// implicit falling through the TRUE case
					switch (promotedTypeID) {
						case T_int :
							codeStream.if_icmpgt(falseLabel);
							break;
						case T_float :
							codeStream.fcmpg();
							codeStream.ifgt(falseLabel);
							break;
						case T_long :
							codeStream.lcmp();
							codeStream.ifgt(falseLabel);
							break;
						case T_double :
							codeStream.dcmpg();
							codeStream.ifgt(falseLabel);
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					return;
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
	}
	
	/**
	 * Boolean generation for &
	 */
	public void generateLogicalAnd(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> & x
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, valueRequired);
				} else {
					// <something equivalent to false> & x
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_0();
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				}
				return;
			} 
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x & <something equivalent to true>
					left.generateCode(currentScope, codeStream, valueRequired);
					right.generateCode(currentScope, codeStream, false);
				} else {
					// x & <something equivalent to false>
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_0();
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.iand();
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}
	
	/**
	 * Boolean generation for |
	 */
	public void generateLogicalOr(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> | x
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_1();
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				} else {
					// <something equivalent to false> | x
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, valueRequired);
				}
				return;
			}
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x | <something equivalent to true>
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_1();
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				} else {
					// x | <something equivalent to false>
					left.generateCode(currentScope, codeStream, valueRequired);
					right.generateCode(currentScope, codeStream, false);
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.ior();
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}
	
	/**
	 * Boolean generation for ^
	 */
	public void generateLogicalXor(BlockScope currentScope,	CodeStream codeStream, boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> ^ x
					left.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_1();
					}
					right.generateCode(currentScope, codeStream, valueRequired);
					if (valueRequired) {
						codeStream.ixor(); // negate
						codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					}
				} else {
					// <something equivalent to false> ^ x
					left.generateCode(currentScope, codeStream, false);
					right.generateCode(currentScope, codeStream, valueRequired);
				}
				return;
			}
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x ^ <something equivalent to true>
					left.generateCode(currentScope, codeStream, valueRequired);
					right.generateCode(currentScope, codeStream, false);
					if (valueRequired) {
						codeStream.iconst_1();
						codeStream.ixor(); // negate
						codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
					}
				} else {
					// x ^ <something equivalent to false>
					left.generateCode(currentScope, codeStream, valueRequired);
					right.generateCode(currentScope, codeStream, false);
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.ixor();
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}	
	
	/**
	 * Boolean generation for &
	 */
	public void generateOptimizedLogicalAnd(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> & x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
				} else {
					// <something equivalent to false> & x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					if (valueRequired) {
						if (falseLabel != null) {
							// implicit falling through the TRUE case
							codeStream.goto_(falseLabel);
						}
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				}
				return;
			}
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x & <something equivalent to true>
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
				} else {
					// x & <something equivalent to false>
					Label internalTrueLabel = new Label(codeStream);
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						internalTrueLabel,
						falseLabel,
						false);
					internalTrueLabel.place();
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					if (valueRequired) {
						if (falseLabel != null) {
							// implicit falling through the TRUE case
							codeStream.goto_(falseLabel);
						}
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.iand();
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					codeStream.ifne(trueLabel);
				}
			} else {
				// implicit falling through the TRUE case
				if (trueLabel == null) {
					codeStream.ifeq(falseLabel);
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}
	
	/**
	 * Boolean generation for |
	 */
	public void generateOptimizedLogicalOr(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> | x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					Label internalFalseLabel = new Label(codeStream);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						internalFalseLabel,
						false);
					internalFalseLabel.place();
					if (valueRequired) {
						if (trueLabel != null) {
							codeStream.goto_(trueLabel);
						}
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				} else {
					// <something equivalent to false> | x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
				}
				return;
			}
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x | <something equivalent to true>
					Label internalFalseLabel = new Label(codeStream);
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						internalFalseLabel,
						false);
					internalFalseLabel.place();
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					if (valueRequired) {
						if (trueLabel != null) {
							codeStream.goto_(trueLabel);
						}
					}
					// reposition the endPC
					codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
				} else {
					// x | <something equivalent to false>
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.ior();
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					codeStream.ifne(trueLabel);
				}
			} else {
				// implicit falling through the TRUE case
				if (trueLabel == null) {
					codeStream.ifeq(falseLabel);
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}
	
	/**
	 * Boolean generation for ^
	 */
	public void generateOptimizedLogicalXor(
		BlockScope currentScope,
		CodeStream codeStream,
		Label trueLabel,
		Label falseLabel,
		boolean valueRequired) {
			
		Constant condConst;
		if ((left.implicitConversion & COMPILE_TYPE_MASK) == T_boolean) {
			if ((condConst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// <something equivalent to true> ^ x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						falseLabel, // negating
						trueLabel,
						valueRequired);
				} else {
					// <something equivalent to false> ^ x
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
				}
				return;
			}
			if ((condConst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
				if (condConst.booleanValue() == true) {
					// x ^ <something equivalent to true>
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						falseLabel, // negating
						trueLabel,
						valueRequired);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
				} else {
					// x ^ <something equivalent to false>
					left.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						valueRequired);
					right.generateOptimizedBoolean(
						currentScope,
						codeStream,
						trueLabel,
						falseLabel,
						false);
				}
				return;
			}
		}
		// default case
		left.generateCode(currentScope, codeStream, valueRequired);
		right.generateCode(currentScope, codeStream, valueRequired);
		if (valueRequired) {
			codeStream.ixor();
			if (falseLabel == null) {
				if (trueLabel != null) {
					// implicit falling through the FALSE case
					codeStream.ifne(trueLabel);
				}
			} else {
				// implicit falling through the TRUE case
				if (trueLabel == null) {
					codeStream.ifeq(falseLabel);
				} else {
					// no implicit fall through TRUE/FALSE --> should never occur
				}
			}
		}
		// reposition the endPC
		codeStream.updateLastRecordedEndPC(currentScope, codeStream.position);					
	}
	
	public void generateOptimizedStringConcatenation(
		BlockScope blockScope,
		CodeStream codeStream,
		int typeID) {
			
		/* In the case trying to make a string concatenation, there is no need to create a new
		 * string buffer, thus use a lower-level API for code generation involving only the
		 * appending of arguments to the existing StringBuffer
		 */

		if ((((bits & OperatorMASK) >> OperatorSHIFT) == PLUS)
			&& ((bits & ReturnTypeIDMASK) == T_JavaLangString)) {
			if (constant != Constant.NotAConstant) {
				codeStream.generateConstant(constant, implicitConversion);
				codeStream.invokeStringConcatenationAppendForType(implicitConversion & COMPILE_TYPE_MASK);
			} else {
				int pc = codeStream.position;
				left.generateOptimizedStringConcatenation(
					blockScope,
					codeStream,
					left.implicitConversion & COMPILE_TYPE_MASK);
				codeStream.recordPositionsFrom(pc, left.sourceStart);
				pc = codeStream.position;
				right.generateOptimizedStringConcatenation(
					blockScope,
					codeStream,
					right.implicitConversion & COMPILE_TYPE_MASK);
				codeStream.recordPositionsFrom(pc, right.sourceStart);
			}
		} else {
			super.generateOptimizedStringConcatenation(blockScope, codeStream, typeID);
		}
	}
	
	public void generateOptimizedStringConcatenationCreation(
		BlockScope blockScope,
		CodeStream codeStream,
		int typeID) {
			
		/* In the case trying to make a string concatenation, there is no need to create a new
		 * string buffer, thus use a lower-level API for code generation involving only the 
		 * appending of arguments to the existing StringBuffer
		 */

		if ((((bits & OperatorMASK) >> OperatorSHIFT) == PLUS)
			&& ((bits & ReturnTypeIDMASK) == T_JavaLangString)) {
			if (constant != Constant.NotAConstant) {
				codeStream.newStringContatenation(); // new: java.lang.StringBuffer
				codeStream.dup();
				codeStream.ldc(constant.stringValue());
				codeStream.invokeStringConcatenationStringConstructor();
				// invokespecial: java.lang.StringBuffer.<init>(Ljava.lang.String;)V
			} else {
				int pc = codeStream.position;
				left.generateOptimizedStringConcatenationCreation(
					blockScope,
					codeStream,
					left.implicitConversion & COMPILE_TYPE_MASK);
				codeStream.recordPositionsFrom(pc, left.sourceStart);
				pc = codeStream.position;
				right.generateOptimizedStringConcatenation(
					blockScope,
					codeStream,
					right.implicitConversion & COMPILE_TYPE_MASK);
				codeStream.recordPositionsFrom(pc, right.sourceStart);
			}
		} else {
			super.generateOptimizedStringConcatenationCreation(blockScope, codeStream, typeID);
		}
	}
	
	public boolean isCompactableOperation() {
		
		return true;
	}
	
	public void optimizedBooleanConstant(int leftId, int operator, int rightId) {

		switch (operator) {
			case AND :
				if ((leftId != T_boolean) || (rightId != T_boolean))
					return;
			case AND_AND :
				Constant cst;
				if ((cst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
					if (cst.booleanValue() == false) { // left is equivalent to false
						optimizedBooleanConstant = cst; // constant(false)
						return;
					} else { //left is equivalent to true
						if ((cst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
							optimizedBooleanConstant = cst;
							// the conditional result is equivalent to the right conditional value
						}
						return;
					}
				}
				if ((cst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
					if (cst.booleanValue() == false) { // right is equivalent to false
						optimizedBooleanConstant = cst; // constant(false)
					}
				}
				return;
			case OR :
				if ((leftId != T_boolean) || (rightId != T_boolean))
					return;
			case OR_OR :
				if ((cst = left.optimizedBooleanConstant()) != Constant.NotAConstant) {
					if (cst.booleanValue() == true) { // left is equivalent to true
						optimizedBooleanConstant = cst; // constant(true)
						return;
					} else { //left is equivalent to false
						if ((cst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
							optimizedBooleanConstant = cst;
						}
						return;
					}
				}
				if ((cst = right.optimizedBooleanConstant()) != Constant.NotAConstant) {
					if (cst.booleanValue() == true) { // right is equivalent to true
						optimizedBooleanConstant = cst; // constant(true)
					}
				}
		}
	}

	public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {

		left.printExpression(indent, output).append(' ').append(operatorToString()).append(' ');
		return right.printExpression(0, output);
	}
		
	public TypeBinding resolveType(BlockScope scope) {

		boolean leftIsCast, rightIsCast;
		if ((leftIsCast = left instanceof CastExpression) == true) left.bits |= DisableUnnecessaryCastCheck; // will check later on
		TypeBinding leftType = left.resolveType(scope);

		if ((rightIsCast = right instanceof CastExpression) == true) right.bits |= DisableUnnecessaryCastCheck; // will check later on
		TypeBinding rightType = right.resolveType(scope);

		// use the id of the type to navigate into the table
		if (leftType == null || rightType == null) {
			constant = Constant.NotAConstant;
			return null;
		}

		int leftTypeID = leftType.id;
		int rightTypeID = rightType.id;

		// autoboxing support
		boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		if (use15specifics) {
			if (!leftType.isBaseType() && rightTypeID != T_JavaLangString && rightTypeID != T_null) {
				leftTypeID = scope.environment().computeBoxingType(leftType).id;
			}
			if (!rightType.isBaseType() && leftTypeID != T_JavaLangString && leftTypeID != T_null) {
				rightTypeID = scope.environment().computeBoxingType(rightType).id;
			}
		}
		if (leftTypeID > 15
			|| rightTypeID > 15) { // must convert String + Object || Object + String
			if (leftTypeID == T_JavaLangString) {
				rightTypeID = T_JavaLangObject;
			} else if (rightTypeID == T_JavaLangString) {
				leftTypeID = T_JavaLangObject;
			} else {
				constant = Constant.NotAConstant;
				scope.problemReporter().invalidOperator((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this, leftType, rightType);
				return null;
			}
		}
		if (((bits & OperatorMASK) >> OperatorSHIFT) == PLUS) {
			if (leftTypeID == T_JavaLangString) {
				this.left.computeConversion(scope, leftType, leftType);
				if (rightType.isArrayType() && ((ArrayBinding) rightType).elementsType() == CharBinding) {
					scope.problemReporter().signalNoImplicitStringConversionForCharArrayExpression(right);
				}
			}
			if (rightTypeID == T_JavaLangString) {
				this.right.computeConversion(scope, rightType, rightType);
				if (leftType.isArrayType() && ((ArrayBinding) leftType).elementsType() == CharBinding) {
					scope.problemReporter().signalNoImplicitStringConversionForCharArrayExpression(left);
				}
			}
		}

		// the code is an int
		// (cast)  left   Op (cast)  right --> result
		//  0000   0000       0000   0000      0000
		//  <<16   <<12       <<8    <<4       <<0

		// Don't test for result = 0. If it is zero, some more work is done.
		// On the one hand when it is not zero (correct code) we avoid doing the test	
		int operator = (bits & OperatorMASK) >> OperatorSHIFT;
		int operatorSignature = OperatorSignatures[operator][(leftTypeID << 4) + rightTypeID];

		left.computeConversion(	scope, 	TypeBinding.wellKnownType(scope, (operatorSignature >>> 16) & 0x0000F), leftType);
		right.computeConversion(scope, TypeBinding.wellKnownType(scope, (operatorSignature >>> 8) & 0x0000F), rightType);
		bits |= operatorSignature & 0xF;
		switch (operatorSignature & 0xF) { // record the current ReturnTypeID
			// only switch on possible result type.....
			case T_boolean :
				this.resolvedType = BooleanBinding;
				break;
			case T_byte :
				this.resolvedType = ByteBinding;
				break;
			case T_char :
				this.resolvedType = CharBinding;
				break;
			case T_double :
				this.resolvedType = DoubleBinding;
				break;
			case T_float :
				this.resolvedType = FloatBinding;
				break;
			case T_int :
				this.resolvedType = IntBinding;
				break;
			case T_long :
				this.resolvedType = LongBinding;
				break;
			case T_JavaLangString :
				this.resolvedType = scope.getJavaLangString();
				break;
			default : //error........
				constant = Constant.NotAConstant;
				scope.problemReporter().invalidOperator((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this, leftType, rightType);
				return null;
		}

		// check need for operand cast
		if (leftIsCast || rightIsCast) {
			CastExpression.checkNeedForArgumentCasts(scope, operator, operatorSignature, left, leftTypeID, leftIsCast, right, rightTypeID, rightIsCast);
		}
		// compute the constant when valid
		computeConstant(scope, leftTypeID, rightTypeID);
		return this.resolvedType;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		
		if (visitor.visit((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this, scope)) {
			left.traverse(visitor, scope);
			right.traverse(visitor, scope);
		}
		visitor.endVisit((/*@OwnPar*/ /*@NoRep*/ BinaryExpression)this, scope);
	}
}

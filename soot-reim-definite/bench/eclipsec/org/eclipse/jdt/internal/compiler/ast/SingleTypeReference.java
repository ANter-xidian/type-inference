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
package org.eclipse.jdt.internal.compiler.ast;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

public class SingleTypeReference extends TypeReference {

	public char[] token;

	public SingleTypeReference(char[] source, long pos) {

			token = source;
			sourceStart = (int) (pos>>>32)  ;
			sourceEnd = (int) (pos & 0x00000000FFFFFFFFL) ;
		
	}

	public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		
		return new ArrayTypeReference(token, dim,(((long)sourceStart)<<32)+sourceEnd);
	}

	protected TypeBinding getTypeBinding(Scope scope) {
		if (this.resolvedType != null)
			return this.resolvedType;

		this.resolvedType = scope.getType(token);

		if (scope.kind == Scope.CLASS_SCOPE && this.resolvedType.isValidBinding())
			if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, (/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, null))
				return null;
		return this.resolvedType;
	}

	public char [][] getTypeName() {
		return new char[][] { token };
	}

	public StringBuffer printExpression(int indent, StringBuffer output){
		
		return output.append(token);
	}

	public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {

		TypeBinding memberType = scope.getMemberType(token, enclosingType);
		if (!memberType.isValidBinding()) {
			this.resolvedType = memberType;
			scope.problemReporter().invalidEnclosingType((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, memberType, enclosingType);
			return null;
		}
		if (isTypeUseDeprecated(memberType, scope))
			scope.problemReporter().deprecatedType(memberType, (/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this);
		memberType = scope.environment().convertToRawType(memberType);
		if (memberType.isRawType() 
				&& (this.bits & IgnoreRawTypeCheck) == 0 
				&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore){
			scope.problemReporter().rawTypeReference((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, memberType);
		}
		return this.resolvedType = memberType;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, scope);
		visitor.endVisit((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, scope);
		visitor.endVisit((/*@OwnPar*/ /*@NoRep*/ SingleTypeReference)this, scope);
	}
}

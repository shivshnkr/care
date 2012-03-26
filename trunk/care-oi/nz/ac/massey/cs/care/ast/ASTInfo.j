;; Produced by JasminVisitor (BCEL)
;; http://bcel.sourceforge.net/
;; Fri Feb 17 23:23:57 NZDT 2012

.source ASTInfo.java
.class public nz/ac/massey/cs/care/ast/ASTInfo
.super java/lang/Object

.field protected startPosition I
.field protected length I

.method public <init>()V
.limit stack 1
.limit locals 1
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1

Label0:
.line 9
	aload_0
	invokespecial java/lang/Object/<init>()V
Label1:
.line 10
	return

.end method

.method public <init>(II)V
.limit stack 2
.limit locals 3
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1
.var 1 is startPosition2 I from Label0 to Label1
.var 2 is length2 I from Label0 to Label1

Label0:
.line 12
	aload_0
	invokespecial java/lang/Object/<init>()V
.line 13
	aload_0
	iload_1
	putfield nz.ac.massey.cs.care.ast.ASTInfo.startPosition I
.line 14
	aload_0
	iload_2
	putfield nz.ac.massey.cs.care.ast.ASTInfo.length I
Label1:
.line 15
	return

.end method

.method public getStartPosition()I
.limit stack 1
.limit locals 1
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1

Label0:
.line 18
	aload_0
	getfield nz.ac.massey.cs.care.ast.ASTInfo.startPosition I
Label1:
	ireturn

.end method

.method public setStartPosition(I)V
.limit stack 2
.limit locals 2
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1
.var 1 is startPosition I from Label0 to Label1

Label0:
.line 22
	aload_0
	iload_1
	putfield nz.ac.massey.cs.care.ast.ASTInfo.startPosition I
Label1:
.line 23
	return

.end method

.method public getLength()I
.limit stack 1
.limit locals 1
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1

Label0:
.line 26
	aload_0
	getfield nz.ac.massey.cs.care.ast.ASTInfo.length I
Label1:
	ireturn

.end method

.method public setLength(I)V
.limit stack 2
.limit locals 2
.var 0 is this Lnz/ac/massey/cs/care/ast/ASTInfo; from Label0 to Label1
.var 1 is length I from Label0 to Label1

Label0:
.line 30
	aload_0
	iload_1
	putfield nz.ac.massey.cs.care.ast.ASTInfo.length I
Label1:
.line 31
	return

.end method

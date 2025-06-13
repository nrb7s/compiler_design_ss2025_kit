.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $48, %esp
L0:
	movl $2147483646, %ebx
	movl $2, %ecx
	movl $3, -8(%rbp)
	movl %ecx, -4(%rbp)
	movl -4(%rbp), %esi
	movl -8(%rbp), %edi
	imull %edi, %esi
	movl %esi, -4(%rbp)
	movl %ecx, -4(%rbp)
	movl -4(%rbp), %esi
	movl -8(%rbp), %edi
	imull %edi, %esi
	movl %esi, -4(%rbp)
	movl $3, -8(%rbp)
	movl -4(%rbp),%esi
	movl %esi, -12(%rbp)
	movl -12(%rbp), %esi
	movl -8(%rbp), %edi
	imull %edi, %esi
	movl %esi, -12(%rbp)
	movl -4(%rbp),%esi
	movl %esi, -12(%rbp)
	movl -12(%rbp), %esi
	movl -8(%rbp), %edi
	imull %edi, %esi
	movl %esi, -12(%rbp)
	movl $11, -16(%rbp)
	movl -12(%rbp),%esi
	movl %esi, -20(%rbp)
	movl -20(%rbp), %esi
	movl -16(%rbp), %edi
	imull %edi, %esi
	movl %esi, -20(%rbp)
	movl -12(%rbp),%esi
	movl %esi, -20(%rbp)
	movl -20(%rbp), %esi
	movl -16(%rbp), %edi
	imull %edi, %esi
	movl %esi, -20(%rbp)
	movl $31, -24(%rbp)
	movl -20(%rbp),%esi
	movl %esi, -28(%rbp)
	movl -28(%rbp), %esi
	movl -24(%rbp), %edi
	imull %edi, %esi
	movl %esi, -28(%rbp)
	movl -20(%rbp),%esi
	movl %esi, -28(%rbp)
	movl -28(%rbp), %esi
	movl -24(%rbp), %edi
	imull %edi, %esi
	movl %esi, -28(%rbp)
	movl $151, -32(%rbp)
	movl -28(%rbp),%esi
	movl %esi, -36(%rbp)
	movl -36(%rbp), %esi
	movl -32(%rbp), %edi
	imull %edi, %esi
	movl %esi, -36(%rbp)
	movl -28(%rbp),%esi
	movl %esi, -36(%rbp)
	movl -36(%rbp), %esi
	movl -32(%rbp), %edi
	imull %edi, %esi
	movl %esi, -36(%rbp)
	movl $331, -40(%rbp)
	movl -36(%rbp),%esi
	movl %esi, -44(%rbp)
	movl -44(%rbp), %esi
	movl -40(%rbp), %edi
	imull %edi, %esi
	movl %esi, -44(%rbp)
	movl -36(%rbp),%esi
	movl %esi, -44(%rbp)
	movl -44(%rbp), %esi
	movl -40(%rbp), %edi
	imull %edi, %esi
	movl %esi, -44(%rbp)
	movl -44(%rbp), %esi
	movl %ebx, %eax
	cdq
	idivl %esi
	movl %eax, -48(%rbp)
	movl -48(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


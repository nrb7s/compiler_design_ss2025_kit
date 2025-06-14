.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $40, %rsp
	jmp L0
L0:
	movl $1, %ebx
	movl $2, %ecx
	movl $3, -4(%rbp)
	movl %ecx, -8(%rbp)
	movl -8(%rbp), %esi
	movl -4(%rbp), %edi
	imull %edi, %esi
	movl %esi, -8(%rbp)
	movl %ecx, -8(%rbp)
	movl -8(%rbp), %esi
	movl -4(%rbp), %edi
	imull %edi, %esi
	movl %esi, -8(%rbp)
	movl %ebx, -12(%rbp)
	movl -8(%rbp), %esi
	addl %esi, -12(%rbp)
	movl %ebx, -12(%rbp)
	movl -8(%rbp), %esi
	addl %esi, -12(%rbp)
	movl $10, -16(%rbp)
	movl -16(%rbp), %esi
	cmpl %esi, -12(%rbp)
	setl %al
	movzbl %al, %esi
	movl %esi, -20(%rbp)
	cmpl $0, -20(%rbp)
	jne L2
	jmp L3
	movl $42, -28(%rbp)
	movl $24, -32(%rbp)
	jmp L2
L2:
	movl $42, -28(%rbp)
	jmp L4
L3:
	movl $24, -32(%rbp)
	jmp L4
L4:
	movl -36(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L1:
	movq %rbp, %rsp
	pop %rbp
	ret


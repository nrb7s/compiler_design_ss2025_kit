.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $44, %esp
	jmp L0
L0:
	movl $10, %ebx
	movl $0, -20(%rbp)
	movl $7, -4(%rbp)
	movl $1, -36(%rbp)
	movl $0, -20(%rbp)
	jmp L2
L2:
	movl -8(%rbp), %esi
	movl -8(%rbp), %eax
	cdq
	idivl %esi
	movl %eax, -16(%rbp)
	movl -8(%rbp), %esi
	movl -8(%rbp), %eax
	cdq
	idivl %esi
	movl %eax, -16(%rbp)
	movl -20(%rbp), %esi
	cmpl %esi, -16(%rbp)
	movzbl %al, %esi
	movl %esi, -24(%rbp)
	cmpl $0, -24(%rbp)
	jne L3
	jmp L4
L3:
	movl -32(%rbp),%esi
	movl %esi, -40(%rbp)
	movl -36(%rbp), %esi
	subl %esi, -40(%rbp)
	jmp L2
L4:
	movl -20(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret
	mov %rbp, %rsp
	pop %rbp
	ret


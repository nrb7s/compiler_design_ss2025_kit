.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $40, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl $10, %ecx
	movl $0, %ebx
	movl $0, %ebx
	movl $7, -16(%rbp)
	movl $1, -28(%rbp)
	movl $0, %ebx
	movl -12(%rbp), %esi
	movl %esi, -4(%rbp)
	jmp L2
L2:
	movl %ecx, %eax
	cdq
	idivl %ecx
	movl %eax, -40(%rbp)
	cmpl %ebx, -40(%rbp)
	setg %al
	movzbl %al, %esi
	movl %esi, -24(%rbp)
	cmpl $0, -24(%rbp)
	jne L3
	jmp L4
L3:
	movl -4(%rbp),%esi
	movl %esi, -32(%rbp)
	movl -28(%rbp), %esi
	subl %esi, -32(%rbp)
	jmp L2
L4:
	movl %ebx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L1:
	movq %rbp, %rsp
	pop %rbp
	ret


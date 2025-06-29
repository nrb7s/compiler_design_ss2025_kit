.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $28, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl $0, %ebx
	movl $0, %ebx
	movl $1, %ecx
	movl $1, %ecx
	movl -16(%rbp), %esi
	movl %esi, -12(%rbp)
	cmpl $0, %ecx
	jne L2
	jmp L3
L2:
	movl %ebx, -28(%rbp)
	movl %ebx, -28(%rbp)
	movl -8(%rbp), %esi
	movl %esi, -12(%rbp)
	jmp L3
L3:
	movl %ebx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L1:
	movq %rbp, %rsp
	pop %rbp
	ret


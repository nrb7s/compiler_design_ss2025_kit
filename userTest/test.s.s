.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $48, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl $781248, %ecx
	movl $0, %ebx
	movl $0, %ebx
	movl $0, %ebx
	movl $0, %ebx
	movl $32, -8(%rbp)
	movl $1, -44(%rbp)
	movl $1, -44(%rbp)
	movl $1, -44(%rbp)
	movl -20(%rbp), %esi
	movl %esi, -32(%rbp)
	jmp L2
L2:
	cmpl -8(%rbp), %ebx
	setl %al
	movzbl %al, %esi
	movl %esi, -16(%rbp)
	cmpl $0, -16(%rbp)
	jne L3
	jmp L4
L3:
	movl -32(%rbp), %esi
	movl %esi, -24(%rbp)
	movl -44(%rbp), %esi
	andl %esi, -24(%rbp)
	movl -32(%rbp), %esi
	movl %esi, -28(%rbp)
	movl -24(%rbp), %esi
	addl %esi, -28(%rbp)
	movl -32(%rbp), %esi
	movl %esi, -4(%rbp)
	movl -44(%rbp), %ecx
	andl $0x1F, %ecx
	sarl %cl, -4(%rbp)
	movl -32(%rbp), %esi
	movl %esi, -12(%rbp)
	movl -44(%rbp), %esi
	addl %esi, -12(%rbp)
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


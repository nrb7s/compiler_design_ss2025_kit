.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $16, %esp
L0:
	movl $3, -12(%rbp)
	movl $3, -12(%rbp)
	movl $0, -4(%rbp)
	movl -4(%rbp),%esi
	movl %esi, -8(%rbp)
	movl -12(%rbp), %esi
	subl %esi, -8(%rbp)
	movl -4(%rbp),%esi
	movl %esi, -8(%rbp)
	movl -12(%rbp), %esi
	subl %esi, -8(%rbp)
	movl $3, -12(%rbp)
	movl $3, -12(%rbp)
	movl -8(%rbp),%esi
	movl %esi, -16(%rbp)
	movl -12(%rbp), %esi
	addl %esi, -16(%rbp)
	movl -16(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


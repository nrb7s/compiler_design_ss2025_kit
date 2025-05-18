.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $20, %esp
	movl $64, %eax
	movl $2, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl $5, %edx
	movl $3, -4(%rbp)
	movl %edx, -8(%rbp)
	movl -4(%rbp), %esi
	addl %esi, -8(%rbp)
	movl $4, -12(%rbp)
	movl -8(%rbp),%esi
	movl %esi, -16(%rbp)
	movl -16(%rbp), %esi
	movl -12(%rbp), %edi
	imull %edi, %esi
	movl %esi, -16(%rbp)
	movl -16(%rbp),%esi
	movl %esi, -20(%rbp)
	subl %ecx, -20(%rbp)
	movl -20(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


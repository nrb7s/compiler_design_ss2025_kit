.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $20, %esp
	movl $132515, %ebx
	movl $12, %ecx
	movl %ebx, %eax
	cdq
	idivl %ecx
	movl %eax, -4(%rbp)
	movl %ebx, %eax
	cdq
	idivl %ecx
	movl %edx, -8(%rbp)
	movl -4(%rbp),%esi
	movl %esi, -12(%rbp)
	movl -12(%rbp), %esi
	imull %ecx, %esi
	movl %esi, -12(%rbp)
	movl -12(%rbp),%esi
	movl %esi, -16(%rbp)
	movl -8(%rbp), %esi
	addl %esi, -16(%rbp)
	movl -16(%rbp),%esi
	movl %esi, -20(%rbp)
	subl %ebx, -20(%rbp)
	movl -20(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


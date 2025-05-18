.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $8, %esp
	movl $6, %eax
	movl $2, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl $3, %edx
	movl %ebx, %esi
	addl %edx, %esi
	movl $4, %edi
	movl %esi, -4(%rbp)
	movl -4(%rbp), %eax
	imull %edi, %eax
	movl %eax, -4(%rbp)
	movl -4(%rbp), %eax
	movl %eax, -8(%rbp)
	subl %ecx, -8(%rbp)
	movl -8(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


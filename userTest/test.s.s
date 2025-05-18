.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $12, %esp
	movl $1, %eax
	movl %eax, %ebx
	addl %eax, %ebx
	movl %ebx, %ecx
	addl %eax, %ecx
	movl %ecx, %edx
	addl %eax, %edx
	movl %edx, %esi
	addl %eax, %esi
	movl %esi, %edi
	addl %eax, %edi
	movl %edi, -4(%rbp)
	addl %eax, -4(%rbp)
	movl -4(%rbp), %eax
	movl %eax, -8(%rbp)
	addl %eax, -8(%rbp)
	movl -8(%rbp), %eax
	movl %eax, -12(%rbp)
	addl %eax, -12(%rbp)
	movl -12(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	pushl %ebp
	movl %esp, %ebp
	subl $8, %esp
	movl $6, %eax
	movl $3, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl $2, %edx
	movl %edx, %esi
	addl %ebx, %esi
	movl $4, %edi
	movl %esi, -4(%ebp)
	imull %edi, -4(%ebp)
	movl -4(%ebp), -8(%ebp)
	subl %ecx, -8(%ebp)
	movl -8(%ebp), %eax
	movl %ebp, %esp
	popl %ebp
	ret


.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	movl $752423891, %eax
	movl $2934786, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %edx, %edx
	movl %ecx, %esi
	imull %ebx, %esi
	movl %esi, %edi
	addl %edx, %edi
	movl %edi, %eax
	ret

.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	movl $5, %eax
	movl $5, %eax
	movl $5, %eax
	movl %eax, %ebx
	subl %eax, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl $5, %eax
	movl $5, %eax
	movl $5, %eax
	movl %eax, %ebx
	subl %eax, %ebx
	movl %eax, %eax
	cdq
	idivl %ebx
	movl %eax, %ecx
	movl %ecx, %eax
	ret

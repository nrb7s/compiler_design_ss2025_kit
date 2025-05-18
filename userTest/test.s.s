.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	movl $1, %eax
	movl %eax, %eax
	ret


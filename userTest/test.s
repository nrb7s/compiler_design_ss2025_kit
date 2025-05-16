.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	movq $42, %rax
	movq %rax, %rax
	ret

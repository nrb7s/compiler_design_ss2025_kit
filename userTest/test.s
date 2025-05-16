	.text
	.globl main
main:
	movq $42, %rax
	movq %rax, %eax
	ret
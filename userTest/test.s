	.text
	.globl main
main:
	pushp %ebp
	movp %esp, %ebp
	movq $42, %rax
	movq %rax, %eax
	popp %ebp
	ret

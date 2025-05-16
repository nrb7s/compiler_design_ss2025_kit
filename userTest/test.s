	.text
	.globl main
main:
	pushl %ebp
	movl %esp, %ebp
	movq $2, %rax
	movq $5, %rbx
	movq %rax, %rcx
	addq %rbx, %rcx
	movq %rcx, %eax
	popl %ebp
	ret

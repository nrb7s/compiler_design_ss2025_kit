	.text
	.globl main
main:
	pushl %ebp
	movl %esp, %ebp
	movl $10, %eax
	movl $32, %ebx
	movl %eax, %ecx
	addl %ebx, %ecx
	movl %ecx, %eax
	popl %ebp
	ret

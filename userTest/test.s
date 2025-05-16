	.text
	.globl main
main:
	pushl %rbp
	movl %rsp, %rbp
	movl $1, %r8d
	movl $3, %r9d
	movl %r8d, %r10d
	addl %r9d, %r10d
	movl $4, %r11d
	movl %r10d, %r12d
	addl %r11d, %r12d
	movl %r12d, %eax
	popl %rbp
	ret

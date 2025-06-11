.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $4, %esp
	movl $10, %ebx
	movl $32, %ecx
	movl %ebx, -4(%rbp)
	addl %ecx, -4(%rbp)
	movl -4(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


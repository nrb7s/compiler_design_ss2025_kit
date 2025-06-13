.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $12, %esp
L0:
	movl $10, %ebx
	mov %rbp, %rsp
	pop %rbp
	ret


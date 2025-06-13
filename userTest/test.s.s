.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	mov %rbp, %rsp
	pop %rbp
	ret


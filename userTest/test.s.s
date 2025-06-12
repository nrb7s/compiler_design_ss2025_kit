.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $4, %esp
	jmp L0
L0:
	mov %rbp, %rsp
	pop %rbp
	ret


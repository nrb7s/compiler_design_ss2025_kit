.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	jmp L0
L0:
	movl $1, %ebx
	movl $5, %ecx
	mov %rbp, %rsp
	pop %rbp
	ret


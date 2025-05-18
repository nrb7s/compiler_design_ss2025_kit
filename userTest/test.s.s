.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	movl $0, %ebx
	movl %ebx, %eax
	mov %rbp, %rsp
	pop %rbp
	ret


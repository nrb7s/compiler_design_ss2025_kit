.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $8, %esp
	jmp L0
L0:
	movl $1, -4(%rbp)
	movl $5, %ecx
	jmp L2
L2:
	movl $1, -4(%rbp)
	cmpl $0, -4(%rbp)
	jne L3
	jmp L4
L3:
L4:
	movl %ecx, %eax
	mov %rbp, %rsp
	pop %rbp
	ret
	mov %rbp, %rsp
	pop %rbp
	ret


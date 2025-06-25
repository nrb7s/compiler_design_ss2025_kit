.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $12, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl $42, %ecx
	movl %ecx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L1:
	movq %rbp, %rsp
	pop %rbp
	ret


.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	movl $3, %eax
	movl $0, %ebx
	movl $2, %ecx
	movl %ebx, %edx
	subl %ecx, %edx
	movl %eax, %eax
	cdq
	movl %edx, %ecx
	idivl %ecx
	movl %edx, %esi
	movl %esi, %eax
	mov %rbp, %rsp
	pop %rbp
	ret


.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $4, %esp
	movl $3, %eax
	movl $0, %ebx
	movl $2, %ecx
	movl %ebx, %edx
	subl %ecx, %edx
	movl %edx, %esi
	movl %eax, %eax
	cdq
	idivl %esi
	movl %edx, -4(%rbp)
	movl -4(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


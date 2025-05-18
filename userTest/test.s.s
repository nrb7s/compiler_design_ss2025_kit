.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $4, %esp
	movl $0, %eax
	movl $6, %ebx
	movl %eax, %ecx
	subl %ebx, %ecx
	movl $3, %edx
	movl %ecx, %eax
	cdq
	movl %edx, %esi
	idivl %esi
	movl %eax, -4(%rbp)
	movl -4(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret


.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $32, %esp
	jmp L0
L0:
	movl $0, %ebx
	movl $100, %ecx
	movl $1, -4(%rbp)
	jmp L2
L2:
	cmpl %ecx, -8(%rbp)
	movzbl %al, %esi
	movl %esi, -12(%rbp)
	cmpl $0, -12(%rbp)
	jne L3
	jmp L4
L3:
	movl -20(%rbp),%esi
	movl %esi, -24(%rbp)
	movl -4(%rbp), %esi
	addl %esi, -24(%rbp)
	jmp L4
L4:
	movl -32(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret
	mov %rbp, %rsp
	pop %rbp
	ret


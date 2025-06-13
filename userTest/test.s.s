.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subl $28, %esp
	jmp L0
L0:
	movl $0, %ebx
	movl $0, %ebx
	movl $1, %ecx
	movl $1, %ecx
	jmp L2
L2:
	cmpl %ecx, -4(%rbp)
	movzbl %al, %esi
	movl %esi, -8(%rbp)
	cmpl $0, -8(%rbp)
	jne L3
	jmp L4
L3:
	movl -16(%rbp),%esi
	movl %esi, -20(%rbp)
	addl %ecx, -20(%rbp)
	jmp L2
L4:
	movl -28(%rbp), %eax
	mov %rbp, %rsp
	pop %rbp
	ret
	mov %rbp, %rsp
	pop %rbp
	ret


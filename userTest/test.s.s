.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $60, %rsp
	jmp L0
L0:
	movl $-5588020, %ebx
	movl $32, %ecx
	movl $0, -4(%rbp)
	movl %ebx, -8(%rbp)
	movl -4(%rbp), %ecx
	andl $0x1F, %ecx
	shll %cl, -8(%rbp)
	movl $32, %ecx
	movl $0, -4(%rbp)
	movl %ebx, -12(%rbp)
	movl -4(%rbp), %ecx
	andl $0x1F, %ecx
	sarl %cl, -12(%rbp)
	cmpl -8(%rbp), %ebx
	sete %al
	movzbl %al, %esi
	movl %esi, -16(%rbp)
	cmpl $0, -16(%rbp)
	jne L5
	jmp L6
	movl $0, -4(%rbp)
	movl $50, -24(%rbp)
	movl $1, -28(%rbp)
	movl $0, -4(%rbp)
	jmp L5
L5:
	movl -36(%rbp), %esi
	cmpl %esi, -32(%rbp)
	sete %al
	movzbl %al, %esi
	movl %esi, -40(%rbp)
	jmp L7
L6:
	movl $0, -4(%rbp)
	jmp L7
L7:
	cmpl $0, -44(%rbp)
	jne L2
	jmp L4
L2:
	movl -24(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L4:
	movl -4(%rbp),%esi
	movl %esi, -56(%rbp)
	movl -28(%rbp), %esi
	subl %esi, -56(%rbp)
	movl -56(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L1:
	movq %rbp, %rsp
	pop %rbp
	ret


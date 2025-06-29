.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $88, %rsp
	jmp L0
L0:
	movl $3, %ebx
	movl $2, %ecx
	movl $1, -72(%rbp)
	movl $1, -72(%rbp)
	movl $0, -76(%rbp)
	movl $0, -76(%rbp)
	movl $0, -76(%rbp)
	movl $1, -72(%rbp)
	movl $0, -76(%rbp)
	movl -76(%rbp), %esi
	movl %esi, -84(%rbp)
	movl $1, -72(%rbp)
	movl -72(%rbp), %esi
	movl %esi, -84(%rbp)
	movl -76(%rbp), %esi
	movl %esi, -88(%rbp)
	movl $0, -76(%rbp)
	movl -76(%rbp), %esi
	movl %esi, -88(%rbp)
	movl -84(%rbp), %esi
	movl %esi, -32(%rbp)
	cmpl $0, -32(%rbp)
	jne L4
	jmp L5
L4:
	movl -88(%rbp), %esi
	movl %esi, -24(%rbp)
	movl -24(%rbp), %esi
	movl %esi, -36(%rbp)
	jmp L6
L5:
	movl $0, -76(%rbp)
	movl -76(%rbp), %esi
	movl %esi, -36(%rbp)
	jmp L6
L6:
	cmpl $0, -36(%rbp)
	jne L2
	jmp L3
L2:
	movl -72(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L3:
	movl -84(%rbp), %esi
	movl %esi, -60(%rbp)
	cmpl $0, -60(%rbp)
	sete %al
	movzbl %al, %esi
	movl %esi, -48(%rbp)
	cmpl $0, -48(%rbp)
	jne L9
	jmp L10
L1:
L9:
	movl $1, -72(%rbp)
	movl -72(%rbp), %esi
	movl %esi, -20(%rbp)
	jmp L11
L10:
	movl -88(%rbp), %esi
	movl %esi, -56(%rbp)
	movl -56(%rbp), %esi
	movl %esi, -20(%rbp)
	jmp L11
L11:
	cmpl $0, -20(%rbp)
	jne L7
	jmp L8
L7:
	movl %ecx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L8:
	movl %ebx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
	movq %rbp, %rsp
	pop %rbp
	ret


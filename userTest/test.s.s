.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $76, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl %ebx, -72(%rbp)
	movl $1, %ecx
	movl $1, %ecx
	movl %ecx, -72(%rbp)
	movl $0, %ebx
	movl %ebx, -76(%rbp)
	movl $0, %ebx
	movl $0, %ebx
	movl %ebx, -76(%rbp)
	movl -72(%rbp), %esi
	movl %esi, -64(%rbp)
	cmpl $0, -64(%rbp)
	jne L4
	jmp L5
	movl $0, %ebx
	movl $1, %ecx
	movl $1, %ecx
	movl $2, -32(%rbp)
	movl $3, -36(%rbp)
	jmp L4
L4:
	movl -76(%rbp), %esi
	movl %esi, -16(%rbp)
	movl -16(%rbp), %ebx
	jmp L6
L5:
	movl $0, %ebx
	jmp L6
L6:
	cmpl $0, %ebx
	jne L2
	jmp L3
L2:
	movl %ecx, %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L3:
	movl -72(%rbp), %esi
	movl %esi, -12(%rbp)
	cmpl $0, -12(%rbp)
	sete %al
	movzbl %al, %esi
	movl %esi, -20(%rbp)
	cmpl $0, -20(%rbp)
	jne L9
	jmp L10
L1:
L9:
	movl $1, %ecx
	movl %ecx, -56(%rbp)
	jmp L11
L10:
	movl -76(%rbp), %esi
	movl %esi, -48(%rbp)
	movl -48(%rbp), %esi
	movl %esi, -56(%rbp)
	jmp L11
L11:
	cmpl $0, -56(%rbp)
	jne L7
	jmp L8
L7:
	movl -32(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L8:
	movl -36(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
	movq %rbp, %rsp
	pop %rbp
	ret


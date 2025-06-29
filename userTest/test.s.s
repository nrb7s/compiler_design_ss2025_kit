.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	push %rbp
	mov %rsp, %rbp
	subq $88, %rsp
	jmp L0
L0:
	movl $0, %ebx
	movl %ebx, -84(%rbp)
	movl $1, -48(%rbp)
	movl $1, -48(%rbp)
	movl -48(%rbp), %esi
	movl %esi, -84(%rbp)
	movl $0, %ebx
	movl %ebx, -88(%rbp)
	movl $0, %ebx
	movl $0, %ebx
	movl %ebx, -88(%rbp)
	movl -84(%rbp), %esi
	movl %esi, -8(%rbp)
	cmpl $0, -8(%rbp)
	jne L4
	jmp L5
	movl $0, %ebx
	movl $1, -48(%rbp)
	movl $1, -48(%rbp)
	movl $2, -52(%rbp)
	movl $3, -56(%rbp)
	jmp L4
L4:
	movl -88(%rbp), %esi
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
	movl -48(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L3:
	movl -84(%rbp), %esi
	movl %esi, -60(%rbp)
	cmpl $0, -60(%rbp)
	sete %al
	movzbl %al, %esi
	movl %esi, -72(%rbp)
	cmpl $0, -72(%rbp)
	jne L9
	jmp L10
L1:
L9:
	movl $1, -48(%rbp)
	movl -48(%rbp), %esi
	movl %esi, -20(%rbp)
	jmp L11
L10:
	movl -88(%rbp), %esi
	movl %esi, -64(%rbp)
	movl -64(%rbp), %esi
	movl %esi, -20(%rbp)
	jmp L11
L11:
	cmpl $0, -20(%rbp)
	jne L7
	jmp L8
L7:
	movl -52(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
L8:
	movl -56(%rbp), %eax
	movq %rbp, %rsp
	pop %rbp
	ret
	movq %rbp, %rsp
	pop %rbp
	ret


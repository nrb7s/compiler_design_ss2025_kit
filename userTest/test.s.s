.section .note.GNU-stack,"",@progbits
	.text
	.globl main
main:
	movq $5, %rax
	movq $5, %rax
	movq $5, %rax
	movq %rax, %rbx
	subq %rax, %rbx
	movq %rax, %rax
	cqto
	idivq %rbx
	movq %rax, %rcx
	movq $5, %rax
	movq $5, %rax
	movq $5, %rax
	movq %rax, %rbx
	subq %rax, %rbx
	movq %rax, %rax
	cqto
	idivq %rbx
	movq %rax, %rcx
	movq %rcx, %rax
	ret

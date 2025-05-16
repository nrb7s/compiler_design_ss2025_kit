section .text
global _start

_start:
	push ebp
	mov ebp, esp
	mov eax, 2
	mov ebx, 5
	mov ecx, eax
	add ecx, ebx
	mov eax, ecx
	pop ebp
	ret

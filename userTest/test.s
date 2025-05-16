section .text
global main

main:
	push ebp
	mov ebp, esp
	mov eax, 2
	mov ebx, 1
	mov ecx, eax
	add ecx, ebx
	mov eax, ecx
	pop ebp
	ret

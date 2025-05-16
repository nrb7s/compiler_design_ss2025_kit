section .text
global main

main:
	mov eax, 5
	mov ebx, 2
	mov ecx, eax
	add ecx, ebx
	mov eax, ecx
	ret
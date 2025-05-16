section .text
global _start

_start:
	mov eax, 2
	mov ebx, 5
	mov ecx, eax
	add ecx, ebx
	mov eax, ecx
	mov ebx, eax
	mov eax, 1
	int 0x80

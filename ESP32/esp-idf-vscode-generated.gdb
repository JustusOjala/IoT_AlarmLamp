target remote :3333
symbol-file /build/lamp.elf
mon reset halt
flushregs
thb app_main
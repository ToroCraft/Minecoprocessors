![Redstone Processor Block](http://i.imgur.com/Vp1e18J.png)

# Minecoprocessors

Increase your redstone possibilities and learn assembly programming at the same time with Minecoprocessors! The Minecoprocessors mod adds a redstone processor block that can be programed similar to a real microprocessor.  The redstone processor block is styled to look like and operate like the vanilla redstone blocks.

## Recipe

![Redstone Processor Recipe](http://i.imgur.com/KUacMFg.png)

## Getting Started

To get started you will need to craft a [Book and Quill](http://minecraft.gamepedia.com/Book_and_Quill).  
The Book and Quill can then be used to write a program in assembly which can then be loaded into a redstone processor.  

Using a Book and Quill write the following program.

```Assembly
mov ports, 0010b
start:
cmp pb, 1
jnz start
mov pf, 1
mov c, 40
loop:
dec c
jnz loop
mov pf, 0
jmp start
```

This program acts as a __pulse extender__ when loaded into a redstone processor. Once the processor detects the back port is powered, the processor will power the front port for a period of time relative to the number put into the `c` register on line 6.  The front port will be powered for just over 80 redstone ticks since there are two commands per iteration and a couple setup commands.  After that period of time as elapsed, the processor will stop powering the front port and wait for the next redstone signal to the back port.

![Redstone Program in Book and Quill](http://i.imgur.com/p616ssf.png)

Next you will need to craft a redstone processor using 
one [redstone block](http://minecraft.gamepedia.com/Block_of_Redstone), 
four [redstone comparators](http://minecraft.gamepedia.com/Redstone_Comparator) 
and four [redstone torches](http://minecraft.gamepedia.com/Redstone_Torch).  
When placing the redstone processor, the front port will be placed facing away from the player, similar to redstone repeaters and comparators.  
Right click the processor to open up the processor’s GUI.  
From there you can current status of the process along with an inventory slot to place the book and quill containing your program.

You might want to read through the [Book and Quill](http://minecraft.gamepedia.com/Book_and_Quill) to see what might Minecraft offers.
The books can be named in an [anvil](http://minecraft.gamepedia.com/Anvil) and signed to make them read only.
Once a book and quill is signed, you can also copy them which could be a useful feature.
However, you will most likely want to write your programs using a program outside of Minecraft as book and quills do allow you to move the cursor.
Also, if you are having trouble copy and pasting a program into a book and quill, make sure it is small enough to fit on one page.
If the program doesn't fit on one page, nothing will happen when you try to paste it into the book and quill.

![Redstone Processor GUI](http://i.imgur.com/kBOYQS4.png)

The redstone processor will start executing your program immediately after placing the book and quill with your program into the GUI.  It can now be used in your circuits just like any other redstone block.

### code explanation

Explain the purpose of the program


The first line uses the `mov` instruction to move the value `0010b` into the `ports` register.
This line will switch all of the ports into output mode except for the back port which will be
set to an input.

```Assembly
mov ports, 0010b
```

Refer to the [I/O POrts](#io-ports) for more information about the ports setup.
The value `0010b` is the number 2 expressed in binary as denoted by the`b` at the end.
Binary is used here to make it easier to see which bit are on or off, however writing this line as
`mov ports, 2` would yield the same effect.

```Assembly
start:
```
The next line adds a label.  Labels are not actual instructions but instead markers providing an
easy way to jump to a line.  Labels can be any word containing only letters, numbers and underscores.
The colon (`:`) at the end of the line denotes the line as a label.  

```Assembly
cmp pb, 1
```
The next line uses the `cmp` instruction to check if the back port (`pb`) is on (`1`).
It is important to note that the `pb` and all other port registers are one byte registers,
but only the least significant bit is used to determine the redstone power for that port.  That means
`0101b`, `0001b`, `1`, `15` will all power the ports since all of those numbers have the same
value in the least significant bit.  Also, the only way to read read stone values from a port register 
is to set the port as an input port as we did in the first line of the program.

```Assembly
jnz start
```
The `jnz` instruction will jump to the specified label if the last command did not result in a zero. 
For this program we are checking if back ports is one or not.  If the back port is on, the `cmp` instruction
would have yielded a zero and this jump would not take place, otherwise the program would jump back the the `start`
label and start run the previous commands again.  These last three lines form a loop so that the program will only progress
further once the back port (`pb`) of the processor is powered.

There are currently two other useful jump commands: `jmp` (always jump) and `jz` (only jump if the previous instruction resulted in a zero).

```Assembly
mov pf, 1
```
Once the back port detects a redstone power signal the loop will be broken and following line will be executed. 
This line moves `1` into the front port register (`pf`) which will power the front port.

```Assembly
mov c, 40
```
Then we setup for a delay by moving `40` in the `c` register.  
The `c` register is commonly used to hold a count although any register could be used for this purpose.
The delay of this circuit can easily be adjust by changing this number.

```Assembly
loop:
```
Then we define a new label called `loop`.

```Assembly
dec c
```
Next we decrement the value in the `c` register using the `dec` instruction.
The `dec` instruction is a shorthand version of `sub c, 1` which will subtract 1 from the value in the `c` register.

```Assembly
jnz loop
```
The `jnz` instruction is again used to form a loop with the previous two lines.  
The loop will continue until the the c register is zero, causing the `jnz` instruction to move to the next line instead of jumping to the `loop` label.

```Assembly
mov pf, 0
```
After the delay is finished, `0` is moved into the `pf` register causing the processor to stop powering the front port.

```Assembly
jmp start
```
Finally, the `jmp` command is used to do a non-conditional jump to the `start` label to repeat the entire process.

## Registers

The redstone processor has four one byte general purpose [registers](https://en.wikipedia.org/wiki/Processor_register): 

* `a` General Purpose Register
* `b` General Purpose Register
* `c` General Purpose Register
* `d` General Purpose Register

along with four port registers:

* `pf` Front Port 
* `pb` Back Port
* `pl` Left Port
* `pr` Right Port

that can be used to read or write to the four ports of the processor.
Only the least significant bit is used to set the power signal of the port.

### I/O Ports

There is one additional register (ports) that is used to set mode of the ports. There are three modes that the ports can be set to, input, output or reset.

#### Port Register Bit Mapping

high nibble | low nibble
----------- | -----------
pr’ pl’ pb’ pf’ | pr pl pb pf

Put a zero value in corresponding low nibble bit to set a port as an output, or set it one to use it as an input port.  Set the corresponding bits in both the high and low nibble to use the port as a reset port.

## Flags

* `Z` Zero Flag, set when the previous instruction yields a zero value
* `C` Carry Flag, set when the precious instruction yields a value too large for the register
* `F` Fault Flag, set when processor has a fault condition (e.g. divide by zero)
* `S` Sleep Flag, set when the processor is in sleep mode

## Supported Number Formats

* `-1` Decimal in (two's complement)[https://en.wikipedia.org/wiki/Two%27s_complement]
* `0xff` (Hexadecimal)[https://en.wikipedia.org/wiki/Hexadecimal]
* `0o377` (Octal)[https://en.wikipedia.org/wiki/Octal]
* `11111111b` (Binary)[https://en.wikipedia.org/wiki/Binary_number]

## Supported commands

* `MOV a, b` Move
* `ADD a, b` Add
* `SUB a, b` Subtract
* `AND a, b` Bitwise AND
* `OR a, b` Bitwise OR
* `XOR a, b` Bitwise XOR
* `NOT a` Bitwise NOT
* `MUL a` Multiply (with the a register)
* `DIV a` Divide (with the a register)
* `JMP label` Jump
* `JZ label` Jump if Zero 
* `JNZ label` Jump if not Zero
* `CALL label` Call Subroutine
* `RET` Return from a Subroutine
* `CMP a, b` Compare (zero if same)
* `SHL a` Shift Left
* `SHR a` Shift Right
* `PUSH a` Push to Stack
* `POP a` Pop from Stack
* `NOP` No Operation
* `INC a` Increment by 1
* `DEC a` Decrement by 1
* `WFE` Wait for Event (experimental)

## Sources to Learn Assembly
* Simple 8-bit Assembler Simulator [](https://schweigi.github.io/assembler-simulator/instruction-set.html)

* Assembly Programming Tutorial [](https://www.tutorialspoint.com/assembly_programming/)


## Larger Programs

The book and quill can only have 14 lines per page with a limited space for each line.
It can hold up to 50 pages, however, allow larger programs to be written when split up onto different pages.
While all pages are merged together when a book and quill is loaded into a redstone processor, it is probably
a better idea to treat each page separately and reference as subroutines using them using the `call` and `ret` instructions.

## Programs for Common Circuits



### notes ....

Any number up to `0xff` (all bits on in (Hexadecimal)[https://en.wikipedia.org/wiki/Hexadecimal]) .  


Since decimal numbers are represented in (two's complement)[https://en.wikipedia.org/wiki/Two%27s_complement] format
the largest number possible 






## Development Environment Setup
Download the desired version of Forge MDK from https://files.minecraftforge.net/ and unzip the MDK into a new directory. After the MDK is unzipped, clone this repository into the `src` directory as `main`. Then you will need to either copy or link the `build.gradle` from the repository to the root of the MDK, replacing the original one. 

### Setup Example
Replace `<MC_VERSION>` with the Minecraft version of the MDK (for example `~/mdk_1.11.2`) and `<MDK_FILE>` with the file name of the MDK you downloaded (for example `forge-1.10.2-13.20.0.2228-mdk.zip`)

```
mkdir ~/mdk_<MC_VERSION>
cd ~/mdk_<MC_VERSION>
cp <MDK_FILE> .
unzip <MDK_FILE>
rm -rf src/main
git clone git@github.com:ToroCraft/Minecoprocessors.git src/main
mv build.gradle build.default.gradle
ln -s src/main/build.gradle build.gradle
./gradlew setupDecompWorkspace
./gradlew eclipse
```


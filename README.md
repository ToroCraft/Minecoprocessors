![Redstone Processor Block](http://i.imgur.com/Vp1e18J.png)

# Minecoprocessors

Increase your redstone possibilities and learn assembly programming at the same time with Minecoprocessors! The Minecoprocessors mod adds a redstone processor block that can be programed similar to a real microprocessor.  The redstone processor block is styled to look like and operate like the vanilla redstone blocks.

## Recipe

![Restone Processor Recipe](http://i.imgur.com/KUacMFg.png)

## Getting Started

To get started you will need to craft a [Book and Quill](http://minecraft.gamepedia.com/Book_and_Quill).  
The Book and Quill can then be used to write a program in assembly which can then be loaded into a redstone processor.  

Using a Book and Quill write  the following program.

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

This program will wait for a redstone signal to the back port of the processor. Once it detects the back port is powered, the processor powers the front port for ten iterations.  This will keep the front port powered for just over 80 redstone ticks since there are two commands per iteration and a couple setup commands.  After that period of time as elapsed, the processor will stop powering the front port jump to the beginning of the program with it will again wait for a redstone signal.

![Restone Program in Book and Quill](http://i.imgur.com/p616ssf.png)

Next you will need to craft a redstone processor using one redstone block, four redstone comparators and four redstone torches.  When placing the redstone processor, the front port will be placed facing away from the player, similar to redstone repeaters and comparators.  Right click the processor to open up the processor’s GUI.  From there you can current status of the process along with an inventory slot to place the book and quill containing your program.

![Restone Processor GUI](http://i.imgur.com/kBOYQS4.png)

The redstone processor will start executing your program immediately after placing your program into the GUI.  It can now be used in your circuits just like any other redstone block.

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
`mov ports, 2` would yeild the same effect.

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
The `jnz` instruction will jump to the specificed label if the last command did not result in a zero. 
For this program we are checking if back ports is one or not.  If the back port is on, the `cmp` instruction
would have yielded a zero and this jump would not take place, otherwise the program would jump back the the `start`
label and start run the previous commands again.  These last three lines form a loop so that the program will only progress
further once the back port (`pb`) of the processor is powered.

There are currenlty two other useful jump commands: `jmp` (always jump) and `jz` (only jump if the previous instruction resulted in a zero).

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
The `dec` instruction is a shorthand verion of `sub c, 1` which will subtract 1 from the value in the `c` register.

```Assembly
jnz loop
```
The `jnz` instruction is again used to form a loop with the previous two lines. 




### notes ....

Any number up to `0xff` (all bits on in (Hexidecimal)[https://en.wikipedia.org/wiki/Hexadecimal]) .  


Since decimal numbers are repersented in (two's complement)[https://en.wikipedia.org/wiki/Two%27s_complement] format
the largest number possible 




## Registers

The redstone processor has four one byte general purpose [registers](https://en.wikipedia.org/wiki/Processor_register): 

* `a` General Puporse Register
* `b` General Puporse Register
* `c` General Puporse Register
* `d` General Puporse Register

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

* `Z`
* `C`
* `F`
* `S`

## Supported Number Formats

* `-1` Decial in (two's complement)[https://en.wikipedia.org/wiki/Two%27s_complement]
* `0xff` (Hexidecimal)[https://en.wikipedia.org/wiki/Hexadecimal]
* `0o377` (Octal)[https://en.wikipedia.org/wiki/Octal]
* `11111111b` (Binray)[https://en.wikipedia.org/wiki/Binary_number]

## Supported commands

* `MOV` 
* `ADD` 
* `SUB` 
* `AND` 
* `OR` 
* `XOR` 
* `NOT` 
* `MUL` 
* `DIV` 
* `JMP` 
* `JZ` 
* `JNZ` 
* `CMP` 
* `SHL` 
* `SHR` 
* `PUSH` 
* `POP` 
* `RET` 
* `CALL` 
* `NOP` 
* `INC` 
* `DEC`
* `WFE`

## Sources to Learn Assembly
* Simple 8-bit Assembler Simulator [](https://schweigi.github.io/assembler-simulator/instruction-set.html)

* Assembly Programming Tutorial [](https://www.tutorialspoint.com/assembly_programming/)


## Larger Programs

## Programs for Common Circuits






## Development Environment Setup
Download the desired version of Forge MDK from https://files.minecraftforge.net/ and unzip the MDK into a new directory. After the MDK is unzipped, clone this repo into the `src` directory as `main`. Then you will need to either copy or link the `build.gradle` from the repository to the root of the MDK, replacing the original one. 

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


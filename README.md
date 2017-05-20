
# Minecoprocessors

![Redstone Processor Block](http://i.imgur.com/Vp1e18J.png)

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

### I/O Ports

There is one additional register (ports) that is used to set mode of the ports. There are three modes that the ports can be set to, input, output or reset.

#### Port Register Bit Mapping

high nibble | low nibble
----------- | -----------
pr’ pl’ pb’ pf’ | pr pl pb pf

Put a zero value in corresponding low nibble bit to set a port as an output, or set it one to use it as an input port.  Set the corresponding bits in both the high and low nibble to use the port as a reset port.

Supported commands

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
* `LOOP` 
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

## Common circuits






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


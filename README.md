![Redstone Processor Block](http://i.imgur.com/Vp1e18J.png)

# Minecoprocessors

Increase your redstone possibilities and learn assembly programming at the same time with the Minecoprocessors Minecraft Mod! The Minecoprocessors Mod adds a redstone processor block that can be programed similar to a real microprocessor.  The redstone processor block is styled to look and operate like the other redstone blocks in the game.

## Recipe

![Redstone Processor Recipe](http://i.imgur.com/KUacMFg.png)


### [Getting Started](https://github.com/ToroCraft/Minecoprocessors/wiki/Getting-Started)

### [Processor Details](https://github.com/ToroCraft/Minecoprocessors/wiki/Processor-Details)

### [Programs for Common Circuits](https://github.com/ToroCraft/Minecoprocessors/wiki/Programs-for-Common-Circuits)

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


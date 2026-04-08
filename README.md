# 🎯 Artillery Mod (1936-1937)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-Forge-orange.svg)](https://files.minecraftforge.net/)

A comprehensive Minecraft mod that brings the iconic anti-tank guns of 1936-1937 to Minecraft with historical accuracy. Experience the military technology of the pre-WW2 era through detailed mechanics and realistic gameplay features.

## 📋 Table of Contents
- [Features](#-features)
- [Artillery Pieces](#-artillery-pieces)
- [Ammunition Types](#-ammunition-types)
- [Gameplay Mechanics](#-gameplay-mechanics)
- [Installation](#-installation)
- [Usage](#-usage)
- [Historical Accuracy](#-historical-accuracy)
- [Screenshots](#-screenshots)
- [License](#-license)

## ✨ Features

### 🎨 Detailed 3D Models
- High-quality artillery models in OBJ format
- Historically accurate textures and colors
- Realistic barrel movement and recoil animations

### 🔧 Advanced Mechanics
- **Realistic Loading System:** Loading mechanics based on historical crew drill times
  - Pak 36: ~4.6 seconds (92 ticks)
  - M1937: ~3.6 seconds (72 ticks)
- **Cooldown Period:** Post-fire case extraction and barrel cooling
- **Recoil Simulation:** Realistic recoil distances for each gun
- **Barrel Elevation:** Precise aiming with historical angle limits
  - Pak 36: -25° (up) / +5° (down)
  - M1937: -20° (up) / +5° (down)

### 🐴 Horse-Towing System
- WW2-era horse-drawn artillery mechanics
- Hitch any Minecraft horse to your artillery piece
- Guns automatically position behind the horse
- Realistic towing position and synchronized movement
- Easy hitch/unhitch with keybind (default: H)

### ⚔️ Durability System
- Each gun has its own health pool
  - Pak 36: 30 HP
  - M1937: 35 HP
- Guns break down when damaged and can be recovered
- Safe pickup feature (with bare hand, no damage)

## 🔫 Artillery Pieces

### 3.7 cm Pak 36
**🇩🇪 German Anti-Tank Gun (1936)**

![Pak 36 Placeholder](docs/images/pak36_preview.png)

#### Historical Information:
- **Full Name:** 3.7 cm Panzerabwehrkanone 36
- **Calibre:** 37 mm
- **Weight:** ~432 kg (in action)
- **Rate of Fire:** 15-20 rounds/min (practical)
- **Year:** 1936

#### In-Game Features:
- Lighter construction → More compact → Faster maneuver
- Loading time: 92 ticks (~4.6 seconds)
- Cooldown time: 20 ticks (1 second)
- Recoil: 0.35 blocks
- Compatible ammo: PzGr., PzGr. 40, Stielgranate 41

---

### 45 mm M1937 (53-K)
**🇷🇺 Soviet Anti-Tank Gun (1937)**

![M1937 Placeholder](docs/images/m1937_preview.png)

#### Historical Information:
- **Full Name:** 45-мм противотанковая пушка образца 1937 года (53-К)
- **Calibre:** 45 mm
- **Weight:** ~560 kg (in action)
- **Rate of Fire:** 15-20 rounds/min (well-trained crew)
- **Year:** 1937

#### In-Game Features:
- Larger calibre → More powerful damage
- Loading time: 72 ticks (~3.6 seconds)
- Cooldown time: 16 ticks (0.8 seconds)
- Recoil: 0.45 blocks
- Compatible ammo: BR-240, BR-240P

## 🎯 Ammunition Types

### 37 mm (Pak 36) Ammunition

#### PzGr. (Panzergranate)
- **Type:** Armor-Piercing (AP)
- **Historical Muzzle Velocity:** ~745 m/s
- **Feature:** Standard AP round with blunt cap
- **In-Game:** Balanced damage and velocity

#### PzGr. 40
- **Type:** APCR (Sub-Calibre Tungsten Core)
- **Historical Muzzle Velocity:** ~1020 m/s
- **Feature:** High-velocity tungsten-core round
- **In-Game:** Higher velocity and penetration, slightly lower damage

#### Stielgranate 41
- **Type:** HEAT (Over-Muzzle Shaped Charge)
- **Historical Muzzle Velocity:** ~110 m/s
- **Feature:** Very slow, very high penetration, rocket-assisted
- **In-Game:** Area blast damage, flat trajectory, heavy armor penetration

### 45 mm (M1937) Ammunition

#### BR-240 (53-BR-240)
- **Type:** Armor-Piercing (AP)
- **Historical Muzzle Velocity:** ~760 m/s
- **Feature:** Standard full-calibre AP round
- **In-Game:** Solid performance and damage

#### BR-240P
- **Type:** APCR (Sub-Calibre)
- **Historical Muzzle Velocity:** ~1070 m/s
- **Feature:** Tungsten-carbide core, high velocity
- **In-Game:** Superior penetration, slightly lower damage

## 🎮 Gameplay Mechanics

### Placing Artillery
1. Select the artillery item in your inventory
2. Right-click on a block
3. The gun will be placed automatically

### Loading and Firing

#### Loading Ammunition:
- **Shift + Right-Click** (with ammo in hand): Load one round
- You'll hear a sound when loading completes
- Incompatible ammunition will show a warning

#### Firing:
- **Right-Click** (empty hand): Fire the loaded gun
- Aim by adjusting your look direction
- Barrel elevation adjusts automatically to your view angle

#### Other Interactions:
- **Shift + Right-Click** (empty hand): Display status (HP, breech state)
- **Left-Click** (empty hand): Safely pick up gun into inventory
- **Left-Click** (weapon/tool): Deal damage to the gun

### Horse Hitching System

1. **Horse Selection:** Any Minecraft horse (horse, donkey, mule, etc.)
2. **Hitching:**
   - Mount the horse
   - Approach the artillery piece
   - Press **H key** (default keybind)
   - Gun automatically attaches behind the horse
3. **Usage:**
   - Ride the horse normally
   - Gun follows automatically
   - Synchronized direction and position
4. **Unhitching:** Press **H key** again

## 🔨 Installation

### Requirements
- Minecraft (Java Edition)
- Minecraft Forge

### Steps

1. **Install Forge:**
   - Visit [Forge](https://files.minecraftforge.net/) website
   - Download Forge for your Minecraft version
   - Run the Forge installer

2. **Install the Mod:**
   - Download Artillery Mod `.jar` file
   - Copy to `.minecraft/mods/` folder
   - Select Forge profile in Minecraft Launcher
   - Launch the game

3. **Verification:**
   - Enter Creative mode in-game
   - Open "Artillery (1936-1937)" tab
   - You should see the guns and ammunition

## 📖 Usage

### Getting Started Guide

1. **Obtain Your First Gun:**
   - Creative: Get from "Artillery (1936-1937)" tab
   - Survival: Use crafting recipes

2. **Place the Gun:**
   - Select flat terrain
   - Right-click to place

3. **Prepare Ammunition:**
   - Select correct calibre rounds
   - Pak 36 → 37mm ammunition
   - M1937 → 45mm ammunition

4. **Fire:**
   - Shift + Right-click to load
   - Wait (loading time)
   - Aim at target
   - Right-click to fire

### Advanced Tips

- **Optimize Elevation:** Look up for distant targets
- **Calculate Cooldown:** Learn timings for rapid fire
- **Mobile Tactics:** Use horse-towing for quick repositioning
- **Ammo Selection:** Try different ammunition types for different targets

## 📚 Historical Accuracy

This mod aims to bring the military technology of 1936-1937 to Minecraft as realistically as possible:

### Accuracy Principles:

1. **Correct Nomenclature:** All guns and ammunition use historical names
2. **Real Specifications:** Values like weight, calibre, and rate of fire are based on actual data
3. **Period Authenticity:** Only weapons used in 1936-1937 are included
4. **Mechanical Realism:**
   - Loading times based on real crew drill times
   - Recoil distances proportional to real physics principles
   - Ammunition compatibility reflects historical accuracy
5. **Pre-WW2 Logistics:** Horse-towing system represents pre-motorized transport era

### Sources and References:

The following resources were used during mod development:
- Official military technical manuals and documentation
- Historical photographs and blueprints
- Museum archives and technical specification books
- Military historian research papers

## 📸 Screenshots

> **Note:** Screenshots to be added

### In-Game View
![Gameplay Screenshot 1](docs/images/screenshot1.png)
*Pak 36 anti-tank gun in combat position*

![Gameplay Screenshot 2](docs/images/screenshot2.png)
*M1937 (53-K) firing with recoil*

### Horse-Towing Mechanics
![Horse Towing 1](docs/images/horse_towing1.png)
*Horse-drawn artillery in motion*

![Horse Towing 2](docs/images/horse_towing2.png)
*Transitioning to combat position*

### Ammunition Varieties
![Ammo Types](docs/images/ammo_types.png)
*All ammunition types with tooltips*

## 🛠️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/artillery-mod.git
cd artillery-mod

# Setup Forge workspace
./gradlew setupDecompWorkspace

# IDE for workspace
# For IntelliJ IDEA:
./gradlew idea
# For Eclipse:
./gradlew eclipse

# Build the mod
./gradlew build

# The compiled .jar will be in build/libs/
```

### Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/NewFeature`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

### Contribution Guidelines:

- Maintain historical accuracy
- Follow code style guide
- Add documentation for new features
- Test and ensure bug-free

## 📝 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

### GPL-3.0 Summary:

✅ **Permissions:**
- Commercial use
- Modification
- Distribution
- Patent use
- Private use

❌ **Conditions:**
- Disclose source
- Same license
- State changes

⚠️ **Limitations:**
- Liability
- Warranty

For details, see [LICENSE](LICENSE) file.

## 🤝 Acknowledgments

- Minecraft and Forge community
- Historical research sources
- All testers and contributors

## 📞 Contact and Support

- **GitHub Issues:** For bug reports and feature requests
- **Discussions:** For general questions and discussions

---

<div align="center">

**A historically accurate mod bringing 1936-1937 military technology to Minecraft.**

⭐ Don't forget to star the project if you like it!

</div>

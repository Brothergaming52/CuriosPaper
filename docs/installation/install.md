# Installation

## Download

Download the latest `CuriosPaper.jar` from:

- [GitHub Releases](https://github.com/Brothergaming52/CuriosPaper/releases)
- [JitPack](https://jitpack.io/#Brothergaming52/CuriosPaper) (for API dependency)

## Install

1. **Stop your server** (recommended for first install)
2. Place `CuriosPaper.jar` into your server's `plugins/` directory
3. Start the server

```
plugins/
└── CuriosPaper.jar
```

## Generated Files

On first start, CuriosPaper creates the following file structure:

```
plugins/CuriosPaper/
├── config.yml          # Main configuration
├── items/              # Custom item definitions (YAML per item)
├── playerdata/         # Per-player accessory storage
└── resources/          # Generated resource pack files
```

## Updating

To update CuriosPaper:

1. Stop the server
2. Replace the old `CuriosPaper.jar` with the new version
3. Start the server

!!! warning "Backup First"
    Always back up your `plugins/CuriosPaper/` folder before updating, especially the `playerdata/` directory.

## Building from Source

If you want to build from source:

```bash
git clone https://github.com/Brothergaming52/CuriosPaper.git
cd CuriosPaper
mvn clean package
```

The compiled JAR will be in the `target/` directory.

### Maven Dependency

To use CuriosPaper as a dependency in your own plugin:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>1.2.0</version>
    <scope>provided</scope>
</dependency>
```

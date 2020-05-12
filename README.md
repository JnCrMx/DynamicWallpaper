# DynamicWallpaper

*DynamicWallpaper* is a small Java program for playing a video 
as a wallpaper and settings up dynamic colors (e.g. based on 
your open processes or windows).

## Installation

First of all make sure that your platform is supported (or just don't care and go experiment a bit :wink:).

### Supported platforms

|          OS |            32-bit |              64-bit |
|-------------|-------------------|---------------------|
|Windows 10   |:question:not tried|:heavy_check_mark:yes|
|Linux (KDE)  |:question:not tried|  :warning:not really|
|Linux (Unity)|:question:not tried|                :x:no|

*Note: It's not very likely that it will work on 32-bit systems if it doesn't work on 64-bit systems.*

For more information about problems on Linux see {{DON'T FORGET TO PUT A LINK HERE!!!!!}}.

### Build from source

To build the application from source make sure you have a JDK (at least 8) installed an properly set up.
It's all just about *Gradle* working well, so don't worry.

Now, download the source from GitHub using ``git clone``
(alternatively you could also download and extract a zipped version of the repository):
```bash
git clone https://github.com/JnCrMx/DynamicWallpaper
```

Then build the application using *Gradle*:

*Windows (in CMD)*
```bash
gradlew.bat build
```

*Unix (in bash)*
```bash
chmod +x gradlew
./gradlew build
```

### Launching the application

There are different ways to launch the application *Gralde* built in the previous step.

#### Use a *shadowJar*

You can find the *shadowJar* in ``build/libs/DynamicWallpaper-<version>-all.jar``.
It already contains all required libraries.
Simply copy it to any location and double-click it to launch.

#### Use a distribution

Besides the *shadowJar* there are also distribution archives.
Those also contain all libraries, but must be extracted before using the application.

You can find them in ``build/distributions/DynamicWallpaper-1.0.zip`` or ``build/distributions/DynamicWallpaper-1.0.tar``.
Just extract them to any directory.

After extracting them just open the ``bin/``-directory and then
execute ``DynamicWallpaper.bat`` (for Windows) or ``DynamicWallpaper`` (for Unix).

#### Use *Gradle*

You can also ask *Gradle* to run the application:

*Windows (in CMD)*
```bash
gradlew.bat run
```

*Unix (in bash)*
```bash
chmod +x gradlew
./gradlew run
```

If you use this way, you don't need to rebuild the program if the source changes.
Building the program is included in this command.

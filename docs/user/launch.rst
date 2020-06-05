.. _Gradle: https://gradle.org/
.. _shadowJar: https://imperceptiblethoughts.com/shadow/

Launching the application
=========================

There are different ways to launch the application *Gradle* has built in the previous step.

Use a `shadowJar`_
------------------

You can find the `shadowJar`_ in ``build/libs/DynamicWallpaper-<version>-all.jar``.
It already contains all required libraries.
Simply copy it to any location you like and double-click it to launch.

Use a distribution
------------------

Besides the *shadowJar* there are also distribution archives.
Those also contain all libraries, but must be extracted before using the application.

You can find them in ``build/distributions/DynamicWallpaper-1.0.zip`` or ``build/distributions/DynamicWallpaper-1.0.tar``.
Just extract them to any directory you want.

After extracting them, just open the ``bin/``-directory and then
execute ``DynamicWallpaper.bat`` (for Windows) or ``DynamicWallpaper`` (for Unix).

Use `Gradle`_
-------------

You can also ask `Gradle`_ to run the application:

*Windows (in CMD)*

.. code-block:: batch

    gradlew.bat run

*Unix (in bash)*

.. code-block:: bash

    chmod +x gradlew
    ./gradlew run

If you use this way, you don't need to rebuild the program if the source changes.
Building the program is included in this command.

- For information about controlling and configuring your wallpaper see :ref:`User Interface`

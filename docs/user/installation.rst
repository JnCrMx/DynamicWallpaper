.. _JDK: https://adoptopenjdk.net/?variant=openjdk8
.. _Gradle: https://gradle.org/

Installation
============

Building from source
--------------------

To build the application from source make sure you have a `JDK`_ (at least 8) installed and properly set up.
It's all just about `Gradle`_ working well, so don't worry.

Now, download the source from `GitHub <https://github.com/JnCrMx/DynamicWallpaper>`_ using ``git clone``
(alternatively you could also download and extract a zipped version of the repository):

.. code-block:: bash

    git clone https://github.com/JnCrMx/DynamicWallpaper

Then build the application using `Gradle`_ (you don't need to have it installed for this):

*Windows (in CMD)*

.. code-block:: batch

    gradlew.bat build

*Unix (in bash)*

.. code-block:: bash

    chmod +x gradlew
    ./gradlew build

For next steps see :ref:`Launching the application`.

.. _FFmpeg: https://ffmpeg.org/
.. _JavaCV: https://github.com/bytedeco/javacv

Playing a video
===============

Playing a video in our frame is basically done in three steps:

.. image:: video-render-steps.svg

Decoding video frames
---------------------

To decode the video frames we use `FFmpeg`_ with bindings provided by `JavaCV`_.

Uploading frames to an OpenGL texture
-------------------------------------

Rendering a plane with OpenGL
-----------------------------

.. _FFmpeg: https://ffmpeg.org/
.. _JavaCV: https://github.com/bytedeco/javacv

.. |FFmpegFrameGrabber| replace:: ``FFmpegFrameGrabber``
.. _FFmpegFrameGrabber: http://bytedeco.org/javacv/apidocs/org/bytedeco/javacv/FFmpegFrameGrabber.html

.. |party| unicode:: U+1F973

Playing a video
===============

Playing a video in our frame is basically done in three steps:

.. image:: video-render-steps.svg

Decoding video frames
---------------------

To decode the video frames we use `FFmpeg`_ with bindings provided by `JavaCV`_.
So, we can basically make a class called |FFmpegFrameGrabber|_ do all the work for us. |party|

First, we need initialize a ``FFmpegFrameGrabber`` and then start it:

.. code-block:: java

    File file = ...;
    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
    grabber.start();

Here we want to read and decode a file.

If we want to read from a stream we need to do some adjustments to prevent it from reading
the stream to its end when calling ``start``:

.. code-block:: java

    InputStream in = ...;
    grabber = new FFmpegFrameGrabber(in, 0);
    grabber.setFormat("mp4");
    grabber.start(false);

We first need to pass this ``0`` as the second argument to the constructor
to tell the FrameGrabber not to read the stream in chunks.
This makes it impossible to seek the stream backwards, but we are fine with that,
because we don't want to do that anyway.

Then we need to tell the FrameGrabber which format the stream is gonna be in.
In our case that is ``mp4``. I'm not sure which formats are possible,
just read the documentation or try it out.

Finally we need to pass ``false`` to the ``start`` method's ``findStreamInfo`` argument.
This tells the FrameGrabber not to read the stream info.
This heavily reduces startup time, because for MP4 this information is located at the end
of the file or stream. So, passing ``true`` (or nothing) would require the stream
to be fully read before we can start decoding.

.. warning::
    Using all these options might decrease stability of the FrameGrabber,
    but we don't really care about that.
    Losing a few frames should not be a problem after all.

Actually grabbing the frame is the same no matter if we read from a stream or a file:

.. code-block:: java

    Frame frame = grabber.grabImage();
    ByteBuffer buf = (ByteBuffer) frame.image[0];

We can simply grab an image frame using ``grabImage`` and then retrieve the raw image
from the ``image`` array in the ``Frame``.

Luckily we don't need to de- or encode the image,
because OpenGL can handle the raw image as it is decoded by the FrameGrabber.

Uploading frames to an OpenGL texture
-------------------------------------

Uploading the image to and OpenGL texture is even easier:

.. code-block:: java

    glTexImage2D(GL_TEXTURE_2D,         // target to upload the image to
                 0,                     // level of detail
                 GL_RGB8,               // internal format
                 frame.imageWidth,      // frame width
                 frame.imageHeight,     // frame height
                 0,                     // border width
                 GL_BGR,                // texel format
                 GL_UNSIGNED_BYTE,      // texel type
                 buf);                  // image

Most of those argument are either not important or should be easy to understand
with basic knowledge of OpenGL. Just look at the
`documentation <https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glTexImage2D.xhtml>`_
or read/watch some OpenGL tutorials.

.. caution::
    We pass ``GL_BGR`` and not ``GL_RGB`` as texel format.
    That is also the format the FrameGrabber decodes the video to.
    If you don't pay attention to this, your colors might get messed up.

Rendering a plane with OpenGL
-----------------------------

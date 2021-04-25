# AI inference offload demonstration application

This Android application is meant to accompany the deep learning
server to create a full deep learning cloud offload demonstration.

See the documentation for the server to setup the server-side of this
demonstration.

It records audio then sends it to the server for inference, displaying
the label returned by the server.

It also offers the user a chance to suggest an alternate label for the
recorded audio, in order to demonstrate the re-training functionality
of the deep learning server.

## Quickstart

1. Edit the string resource ``dl_server_uri`` to point to the address of the
deep learning server, e.g. ``dns:///my.address:55221``.

1. Start the deep learning server.

2. Start the application and hit the record button!

    - The "virtual microphone uses host audio input" option may need to
    be checked if running in an emulator. 
    See the Android emulator settings for more information.

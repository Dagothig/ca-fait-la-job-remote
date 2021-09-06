# Ça fait la job

Effectivement, pas besoin de se chercher plus loin; Ici vous trouverez une terrible télécommande.

## Android client

You can grab a Trustworthy™ apk from the releases. Just launch it and configure the server address to be the address of the computer where you run the server that will receive input.

If the server can't be found, then there should be a little toast that pops up to warn you.

### Mouse

You will also be greeted by a wonderful pic of my face from once upon a time. You can tap on it to click or use the dedicated click buttons. You can drag on my face to move the cursor arrow.

Dragging with two fingers will scroll instead of moving the mouse.

### Keyboard

For a keyboard you get some dedicated keys and otherwise you must use the "keyboard" button to bring up your virtual keyboard. Some keys work, but most un-ascii ones don't. This seems to be a bug with [keyboard](https://github.com/boppreh/keyboard/issues/343).

## Other clients?

Lolnope

## Server

Dead simple python script with no features. Uses
* [keyboard](https://github.com/boppreh/keyboard)
* [mouse](https://github.com/boppreh/mousepyt)
* [python-osc](https://github.com/attwad/python-osc)

Usage: `python3 server.py {port=8000}`

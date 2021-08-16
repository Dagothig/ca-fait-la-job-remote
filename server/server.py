import sys

if len(sys.argv) < 1:
    print("Usage: python3 server.py")
    exit()

PORT = 8000

import socket
def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # doesn't even have to be reachable
        s.connect(('10.255.255.255', 1))
        IP = s.getsockname()[0]
    except Exception:
        IP = '127.0.0.1'
    finally:
        s.close()
    return IP

from screeninfo import get_monitors
# TODO refresh monitors
monitors = get_monitors()

import keyboard
import mouse
from pythonosc.dispatcher import Dispatcher
from pythonosc.osc_server import BlockingOSCUDPServer

def handle_keyboard_event(address, c, v):
    print(address, c, v)
    if v == 1:
        keyboard.press(c)
    elif v == 0:
        keyboard.release(c)

def handle_mouse_event(address, v, x, y):
    print(v, x, y)
    x = min(max(x, 0), 1)
    y = min(max(y, 0), 1)

    left = v & 1
    left_pressed = mouse.is_pressed('left')
    if left and not left_pressed:
        mouse.press('left')
    if not left and left_pressed:
        mouse.release('left')

    right = (v >> 1) & 1
    right_pressed = mouse.is_pressed('right')
    if right and not right_pressed:
        mouse.press('right')
    if not right and right_pressed:
        mouse.release('right')

    m_idx = min(int(x * len(monitors)), len(monitors) - 1)
    x = int(monitors[m_idx].width * x * len(monitors))
    y = int(monitors[m_idx].height * y)

    mouse.move(x, y, True)


dispatcher = Dispatcher()
dispatcher.map("/keyboard", handle_keyboard_event)
dispatcher.map("/mouse", handle_mouse_event)

with BlockingOSCUDPServer(("", PORT), dispatcher) as server:
    print("Server started", get_ip() + ":" + str(PORT))
    server.serve_forever()
print("AAA")
sys.stdout.flush()

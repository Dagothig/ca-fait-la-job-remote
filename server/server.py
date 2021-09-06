import sys

def help():
    print("Usage: python3 server.py {port?}")
    exit()

def version():
    print("ca-fait-la-job-remote/server 1.1")
    exit()

if len(sys.argv) < 1:
    help()
for arg in sys.argv:
    if arg == "--help" or arg == "-h":
        help()
    elif arg == "--version" or arg == "-v":
        version()

PORT = 8000 if len(sys.argv) < 2 else int(sys.argv[1])

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

import keyboard
import mouse

from pythonosc.dispatcher import Dispatcher
from pythonosc.osc_server import BlockingOSCUDPServer

def handle_keyboard_event(address, c, v):
    if v & 1:
        keyboard.press(c)
    if v & 2:
        keyboard.release(c)
    if v & 4:
        for car in c:
            if car == " ":
                car = "space"
            if (car.isalnum() and car.islower()) or car == "space":
                keyboard.press_and_release(car)
            else:
                keyboard.write(c, exact=True)

left_pressed = False
right_pressed = False
def handle_mouse_event(address, v, x, y):
    global left_pressed, right_pressed
    left = v & 1
    if left and not left_pressed:
        mouse.press('left')
        left_pressed = True
    if not left and left_pressed:
        mouse.release('left')
        left_pressed = False

    right = v & 2
    if right and not right_pressed:
        mouse.press('right')
        right_pressed = True
    if not right and right_pressed:
        mouse.release('right')
        right_pressed = False

    wheel = v & 4
    if wheel:
        mouse.wheel(y)
    else:
        mouse.move(x, y, False)

dispatcher = Dispatcher()
dispatcher.map("/keyboard", handle_keyboard_event)
dispatcher.map("/mouse", handle_mouse_event)

with BlockingOSCUDPServer(("", PORT), dispatcher) as server:
    print("Server started with address", get_ip() + ":" + str(PORT))
    sys.stdout.flush()
    server.serve_forever()

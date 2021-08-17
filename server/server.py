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
            if car.isalnum() or car == "space":
                keyboard.press_and_release(car)
            else:
                keyboard.write(c, exact=True)

def handle_mouse_event(address, v, x, y):
    left = v & 1
    left_pressed = mouse.is_pressed('left')
    if left and not left_pressed:
        mouse.press('left')
    if not left and left_pressed:
        mouse.release('left')

    right = v & 2
    right_pressed = mouse.is_pressed('right')
    if right and not right_pressed:
        mouse.press('right')
    if not right and right_pressed:
        mouse.release('right')

    mouse.move(x, y, False)


dispatcher = Dispatcher()
dispatcher.map("/keyboard", handle_keyboard_event)
dispatcher.map("/mouse", handle_mouse_event)

with BlockingOSCUDPServer(("", PORT), dispatcher) as server:
    print("Server started with address", get_ip() + ":" + str(PORT))
    server.serve_forever()
print("AAA")
sys.stdout.flush()

import sys

for arg in sys.argv:
    if arg == "--help" or arg == "-h":
        print("""Usage: sudo python3 http-server.py {html-port=80} {socket-port=8765}""")
        exit()

HTTP_PORT = 80 if len(sys.argv) < 2 else int(sys.argv[1])
SOCKET_PORT = 8765 if len(sys.argv) < 3 else int(sys.argv[2])

import asyncio
import websockets
import pynput
import keyboard
import http.server
import socketserver
import os

mouse = pynput.mouse.Controller()
buttonByKey = {
    "left": pynput.mouse.Button.left,
    "right": pynput.mouse.Button.right
}

async def handle_socket(websocket):
    async for msg in websocket:
        parts = msg.split("/")
        controller = parts[0]
        key = parts[1]
        action = parts[2]
        is_down = action == "down"
        is_up = action == "up"
        if controller == "cursor":
            x = float(parts[3]) if len(parts) >= 4 else 0
            y = float(parts[4]) if len(parts) >= 5 else 0
            if key == "left" or key == "right":
                key = buttonByKey[key]
                if is_down:
                    mouse.press(key)
                elif is_up:
                    mouse.release(key)
                elif action == "click":
                    mouse.click(key)
            if x or y:
                if key == "wheel":
                    mouse.scroll(x, y)
                else:
                    mouse.move(x, y)
        elif controller == "keyboard":
            if key == "input":
                for car in action:
                    if car == " ":
                        car = "space"
                    if (car.isalnum() and car.islower()) or car == "space":
                        keyboard.press_and_release(car)
                    else:
                        keyboard.write(car, exact=True)
            else:
                if is_down:
                    keyboard.press(key)
                elif is_up:
                    keyboard.release(key)
async def main():
    async with websockets.serve(handle_socket, "", SOCKET_PORT):
        print("Socket listening on port", SOCKET_PORT)
        os.chdir("../client-http")
        with socketserver.TCPServer(("", HTTP_PORT), http.server.SimpleHTTPRequestHandler) as httpd:
            print("Server http at port", HTTP_PORT)
            httpd.serve_forever()

asyncio.run(main())
import asyncio
import json
import socket
import random
from urllib.parse import urlparse, parse_qs
import websockets

PORT = 8080
rooms = {}


def get_lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except Exception:
        return "127.0.0.1"
    finally:
        s.close()


LAN_IP = get_lan_ip()


async def handler(websocket):
    request = getattr(websocket, "request", None)
    path = request.path if request else "/"
    parsed = urlparse(path)
    params = parse_qs(parsed.query)
    role = params.get("role", [None])[0]
    room_param = params.get("room", [None])[0]

    print(f"[WS] connect role={role} room={room_param or '-'}")

    if role == "host":
        room_id = str(random.randint(1000, 9999))
        while room_id in rooms:
            room_id = str(random.randint(1000, 9999))

        rooms[room_id] = {"host": websocket, "controller": None, "id": room_id}
        qr_content = f"ws://{LAN_IP}:{PORT}?role=controller&room={room_id}"

        await websocket.send(json.dumps({
            "type": "room",
            "roomId": room_id,
            "lanIp": LAN_IP,
            "qrContent": qr_content,
        }))
        print(f"[ROOM {room_id}] created -> QR: {qr_content}")

        try:
            async for message in websocket:
                room = rooms.get(room_id)
                if room and room["controller"]:
                    try:
                        await room["controller"].send(message)
                    except Exception as e:
                        print(f"[ROOM {room_id}] forward->controller fail: {e}")
        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            room = rooms.pop(room_id, None)
            if room and room["controller"]:
                try:
                    await room["controller"].close()
                except Exception:
                    pass
            print(f"[ROOM {room_id}] host closed")

    elif role == "controller" and room_param:
        room = rooms.get(room_param)
        if not room:
            await websocket.send(json.dumps({"type": "error", "message": "Room not found"}))
            await websocket.close()
            return

        room["controller"] = websocket
        print(f"[ROOM {room_param}] controller joined")

        if room["host"]:
            try:
                await room["host"].send(json.dumps({"type": "controller-joined"}))
            except Exception:
                pass

        try:
            async for message in websocket:
                if room["host"]:
                    try:
                        await room["host"].send(message)
                    except Exception as e:
                        print(f"[ROOM {room_param}] forward->host fail: {e}")
        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            room["controller"] = None
            if room["host"]:
                try:
                    await room["host"].send(json.dumps({"type": "controller-left"}))
                except Exception:
                    pass
            print(f"[ROOM {room_param}] controller closed")

    else:
        await websocket.send(json.dumps({"type": "error", "message": "Invalid role"}))
        await websocket.close()


async def main():
    print("\n✨ Endless Drive — Signaling Server (Python)")
    print("─────────────────────────────────────")
    print(f"   Local:   ws://localhost:{PORT}?role=host")
    print(f"   LAN IP:  {LAN_IP}")
    print(f"   Port:    {PORT}")
    print("─────────────────────────────────────")
    print("   Waiting for game + controller...\n")

    async with websockets.serve(handler, "0.0.0.0", PORT):
        await asyncio.Future()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[server] bye 👋")

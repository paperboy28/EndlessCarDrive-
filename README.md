# 🏎️ Endless Drive - Web Game & Android Controller

A proof-of-concept prototype for a 3D endless driving game built with **Three.js** and a native **Android Controller App** built with **Kotlin**. 

The game runs in the browser, and your phone acts as a physical controller. They connect instantly over a local WebSocket connection on the same Wi-Fi network. No internet, no cloud servers, just pure local LAN multiplayer!

## ✨ Features
- **Claymorphism Aesthetic:** Soft pastel colors, toy-like 3D environment inspired by Bruno Simon.
- **Endless Driving:** Procedural track generation, obstacles, nitro boosts, and near-miss bonuses.
- **Native Android App:** Built with Kotlin, featuring large tactile clay buttons and haptic feedback (vibration).
- **Zero Latency Setup:** Connects directly via a local Python WebSocket server.
- **QR Code Pairing:** Scan the QR code on the laptop screen to instantly connect the controller.

## 🛠️ Tech Stack
- **Game (Web):** HTML5, CSS3, Vanilla JS, Three.js (r160), PeerJS (Optional fallback)
- **Server (Signaling):** Python 3, `websockets`
- **Controller (Android):** Kotlin, Android Studio, OkHttp WebSockets, ZXing (QR Scanner)

---

## 🚀 How to Play (Setup Guide)

### 1. Start the Local Server (Laptop)
The laptop acts as the host and needs to run a lightweight Python server for the phone and browser to communicate.
```bash
# Navigate to the project folder
cd endless-signaling

# Activate virtual environment (first time only)
python3 -m venv venv
source venv/bin/activate
pip install websockets

# Run the server
python3 server.py

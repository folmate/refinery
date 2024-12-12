@echo off

docker exec -d -u abc ^
	-e DISPLAY=:0 ^
	-e WAYLAND_DISPLAY=wayland-0 ^
	-e XDG_RUNTIME_DIR=/mnt/wslg/runtime-dir/ ^
	-e PULSE_SERVER=unix:/mnt/wslg/PulseServer ^
	refinery-dev firefox
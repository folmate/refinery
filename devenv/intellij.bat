@echo off

docker exec -d -u abc ^
	-e DISPLAY=:0 ^
	-e WAYLAND_DISPLAY=wayland-0 ^
	-e XDG_RUNTIME_DIR=/mnt/wslg/runtime-dir/ ^
	-e PULSE_SERVER=unix:/mnt/wslg/PulseServer ^
	-e REFINERY_LISTEN_HOST=0.0.0.0 ^
	refinery-dev /opt/idea-IC-241.17890.1/bin/idea.sh
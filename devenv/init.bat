@echo off

echo Stopping container
docker stop refinery-dev

echo Removing container
docker remove refinery-dev

echo Run container
docker run -it -u root -d ^
	-v \\wsl$\Ubuntu\tmp\.X11-unix:/tmp/.X11-unix ^
	-v \\wsl$\Ubuntu\mnt\wslg:/mnt/wslg ^
	-v %~dp0\..:/config/refinery ^
	-p 4050:3389 ^
	-p 1312:1312 ^
	--name=refinery-dev refinery-dev
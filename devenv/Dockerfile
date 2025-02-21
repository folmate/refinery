FROM lscr.io/linuxserver/rdesktop:ubuntu-xfce

ENV PUID=1000 
ENV PGID=1000
ENV TZ=Etc/UTC 

EXPOSE 3389
EXPOSE 1312

RUN apt update
RUN apt install git -y
RUN apt install wget -y
RUN apt install openjdk-21-jdk -y

RUN wget https://download-cdn.jetbrains.com/idea/ideaIC-2024.1.3.tar.gz
RUN tar -xzf ideaIC-*.tar.gz -C /opt
RUN rm ideaIC-*.tar.gz

RUN echo '[Desktop Entry]\n\
Version=13.0\n\
Type=Application\n\
Terminal=false\n\
Exec=/opt/idea-IC-241.17890.1/bin/idea.sh\n\
Name=IntelliJ\n\
Categories=Development\n\
Icon=/opt/idea-IC-*/bin/idea.png' > /usr/share/applications/intellij.desktop

RUN apt install webext-ublock-origin-firefox -y




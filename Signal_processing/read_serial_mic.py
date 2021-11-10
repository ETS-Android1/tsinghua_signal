import serial
import numpy as np
import socket
import threading
import queue
import time
import sys
import matplotlib.pyplot as plt
import numpy as np

plt.rcParams["font.family"] = "Linux Libertine"

SAMPLE_WIDTH=2
CHANNEL_NUM=1
BUFFER_SIZE=5000
HOST='127.0.0.1'
PORT=1234

BUFFER_CNT=10

def sendHeader(client, time):
    client.send(bytearray([time&255, (time>>8)&255, (time>>16)&255, (time>>24)&255]))

def sendData(client, data):
    client.send(bytearray(data))

class TCPThread(threading.Thread):
    def __init__(self, client):
        threading.Thread.__init__(self,None, 'TCPThread', 1)
        self.client=client
        self.queue=queue.Queue(BUFFER_CNT)
    def push(self,data):
        self.queue.put(data)
    def run(self):
        self.client.send(bytearray([0]))
        while(True):
            data=self.queue.get()
            if data==[]:
                break
            t=(int(time.time()*1000)&0xffffffff)
            sendHeader(self.client, t)
            sendData(self.client, data)
        self.client.close()

def openTCP():
    client=socket.socket()
    client.connect((HOST, PORT))
    thread=TCPThread(client)
    thread.start()
    return thread

def sendTCP(thread, data):
    thread.push(data)

def open():
    ser = serial.Serial(port='COM8')
    return ser


def run():
    # thread=openTCP()
    time.sleep(0.05)
    ser=open()
    try:
        while(True):
            data=ser.read(SAMPLE_WIDTH*CHANNEL_NUM*BUFFER_SIZE)
            # sendTCP(thread, data)
            # print(len(data))
            # print(type(data))
            # data = np.fromstring(data, dtype=np.int16)
            # plt.plot(data)
            # plt.pause(1)
    except KeyboardInterrupt:
        # sendTCP(thread, [])
        # thread.join()
        sys.exit()

run()

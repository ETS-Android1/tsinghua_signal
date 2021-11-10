import datetime
import os
from threading import Thread, Event
from time import sleep

import pyaudio
import serial
import numpy as np
import tkinter as tk
import matplotlib.pyplot as plt
from matplotlib import animation, lines
import itertools


from config import Config
from Audio2Feature import Audio2Feature

class Tracker:

    def __init__(self):
        # for microphones
        self.ad_rdy_ev = Event()
        self.n_channels = 2  # CHANGE HERE
        self.sound_sources = 1  # CHANGE HERE
        # for sound sources
        self.a2d = []
        self.colors = ['blue', 'green', 'red', 'orange', 'purple', 'cyan']
        self.plot_x_range = 100
        self.total_channels = 2
        self.channel_index = [2, 4, 5]
        self.n_objects = 6
        self.cur_serial_data = ''
        self.imu_data = []
        self.logger = []
        self.plot_x_range = 200
        self.ani_list = []


    def animate_ppg(self):
        fig, ax = plt.subplots(self.sound_sources, 1)
        line = []

        for i in range(self.sound_sources):
            line.append(lines.Line2D([], [], color=self.colors[i], label=f'Speaker {i + 1}'))

        if (self.sound_sources == 1):
            ax.add_line(line[0])
            ax.set_xlim(0, self.plot_x_range)
            ax.set_ylim(200, 1000)
            ax.grid()
            ax.set_title('distance difference')
        else:
            for i in range(self.sound_sources):
                ax[i].add_line(line[i])
                ax[i].set_xlim(0, self.plot_x_range)
                ax[i].set_ylim(200, 1000)
                ax[i].grid()
                ax[i].set_title('distance difference {}'.format(i + 1))

        def update(i):
            if len(self.imu_data) > 15:
                data_list = np.array(self.imu_data[max(-len(self.imu_data)+15, -self.plot_x_range):])
                imu_list = data_list[:,-1]
                ydata = []
                for item in imu_list:
                    # print(item)
                    temp = item.split(',')
                    # print(temp)
                    ydata.append(int(temp[-2]))
                xdata = np.arange(self.plot_x_range - len(ydata), self.plot_x_range)
                line[0].set_xdata(xdata)
                line[0].set_ydata(ydata)
            return itertools.chain(line)

        ani = animation.FuncAnimation(fig, update, frames=1, interval=100, blit=True)
        self.ani_list.append(ani)
        plt.legend()
        fig.tight_layout()
        fig.show()
        return ani

def mainloop(tracker):
    root = tk.Tk()
    root.title("Config...")
    tk.Button(root, text='Start', command=lambda: add_sound_source(tracker)).grid(row=3,column=0,sticky=tk.W, pady=10)
    tk.Button(root, text='Log', command=lambda: my_logger(tracker)).grid(row=3, column=1, sticky=tk.W, pady=10)

    root.mainloop()

def my_logger(tracker):
    config = Config()
    Thread(target=log_timer, args=(tracker, config), daemon=True).start()

def log_timer(tracker, config):
    tracker.logger.append([tracker.imu_data[-1][0], tracker.imu_data[-1][1]])
    print('successfully log')

def add_sound_source(tracker):
    """
    which : 'left' or 'right', which earbud to calibrate.
    """
    config = Config()
    for i in range(tracker.n_channels):
        a2d_item = Audio2Feature(config, name=str(i))
        tracker.a2d.append(a2d_item)
    print('start recording')

def process_data(tracker, config):
    while True:
        if len(tracker.a2d) == tracker.n_channels * tracker.sound_sources and tracker.cur_serial_data != '':
            tracker.ad_rdy_ev.wait()
            for i, a2d_item in enumerate(tracker.a2d):
                counter, timestamp = a2d_item.get_timestamp()
                tracker.imu_data.append([counter, timestamp, tracker.cur_serial_data])
            tracker.ad_rdy_ev.clear()
        else:
            sleep(0.1)

# pushing the audio data to all a2d.
def stream_callback(tracker):
    def callback(in_data, frame_count, time_info, status):

        data = np.frombuffer(in_data, dtype=np.int16)
        # if (len(tracker.a2d) == tracker.n_channels * tracker.sound_sources):
        for i in range(int(len(tracker.a2d) / tracker.n_channels)):
            for j in range(tracker.n_channels):
                a2d_item = tracker.a2d[i * tracker.n_channels + j]
                a2d_item.push(data[tracker.channel_index[j]::tracker.total_channels])
        # else:
        #     print("In stream_callback, size ERROR.")

        tracker.ad_rdy_ev.set()
        return (None, pyaudio.paContinue)

    return callback

def recv_proc_earbud_stereo(config, tracker):
    p = pyaudio.PyAudio()

    # callback for receiving data
    stream = p.open(format=p.get_format_from_width(config.sample_width),
                    channels=config.total_channels,
                    rate=config.frame_rate,
                    input=True, input_device_index=config.input_device_id[0],
                    frames_per_buffer=config.chunk_size,
                    stream_callback=stream_callback(tracker))

    # threads for processing data
    Thread(target=process_data, args=(tracker, config), daemon=True).start()

    return [stream], p

def read_serial(tracker, config):
    BaudRate = 19200
    ser = serial.Serial(port='COM4', baudrate=BaudRate)
    cnt = 0
    while (True):
        data = ser.readline()
        data = data.strip().decode(encoding='unicode_escape')
        # print(type(data), data)
        if cnt < 15:
            print(data)
            if cnt > 1:
                timestamp_item = datetime.datetime.now().timestamp() * 1000
                tracker.imu_data.append([0, timestamp_item, data])
            cnt += 1
        else:
            tracker.ad_rdy_ev.wait()
            tracker.cur_serial_data = data
            tracker.ad_rdy_ev.clear()

def save(folder, tracker, config=None):
    if not os.path.exists(folder):
        os.mkdir(folder)
    if len(os.listdir(folder)) > 0:
        raise ValueError('Folder not empty!')
    for i, a2d_item in enumerate(tracker.a2d):
        a2d_item.save(os.path.join(folder, str(i)))
    config.save(os.path.join(folder, 'config.json'))
    print(f'All saved in {folder}')

if __name__ == '__main__':
    config = Config()
    tracker = Tracker()

    stream_list, p = recv_proc_earbud_stereo(config, tracker)

    Thread(target=read_serial, args=(tracker, config), daemon=True).start()
    ani = tracker.animate_ppg()

    mainloop(tracker)

    for stream_item in stream_list:
        stream_item.stop_stream()
        stream_item.close()

    p.terminate()

    # saving data
    RECORD = True
    person_num = 4
    now = datetime.datetime.now()
    now_str = now.strftime('%Y-%m-%d-%H-%M-%S')

    if RECORD:
        print("Saving Data")
        path = os.getcwd() + '/data/'
        if not os.path.exists(path):
            os.mkdir(path)
        folder = path + str(person_num) + '/'
        if not os.path.exists(folder):
            os.mkdir(folder)
        folder = folder + now_str
        save(folder, tracker, config)

        np.savetxt(folder + '/imu_data.txt', tracker.imu_data, fmt='%s', encoding='utf-8')
        np.savetxt(folder + '/split_point.txt', tracker.logger, fmt='%s')


import datetime
from threading import Thread, Event
import os
import serial
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import animation, lines
import itertools
from time import sleep
import tkinter as tk


ACCData = [0.0] * 8
GYROData = [0.0] * 8
AngleData = [0.0] * 8


MOTION_TYPE = {0:'turn_left',1:'turn_right', 2:'up', 3:'down', 4:'tile_left', 5:'tilt_right',
               6:'left_up', 7:'right_up', 8:'left_down', 9:'right_down',
               10:'circle_clockwise', 11:'circle_counterclockwise', 12:'eight_left', 13:'eight_right',
               14:'none'}


class SerialReader:
    def __init__(self):
        self.a = [0.0] * 3
        self.w = [0.0] * 3
        self.Angle = [0.0] * 3
        self.FrameState = 0  # 通过0x后面的值判断属于哪一种情况
        self.ByteNum = 0  # 读取到这一段的第几位
        self.RawData = [0.0] * 18

    def DueData(self, inputdata, recorder):
        for i in range(len(inputdata)):
            if self.FrameState == 0:
                if inputdata[i] == 0x55:
                    self.FrameState = 1
                    self.ByteNum = 1
            else:
                if inputdata[i] == 0x0A:
                    self.FrameState = 0
                    self.ByteNum = 0
                    continue

                if self.ByteNum >= 2 and self.ByteNum < 20:
                    self.RawData[self.ByteNum-2] = inputdata[i]

                if self.ByteNum == 20:
                    self.a = self.get_acc(self.RawData[0:6])
                    self.w = self.get_gyro(self.RawData[6:12])
                    self.Angle = self.get_angle(self.RawData[12:18])
                    d = self.a + self.w + self.Angle
                    if len(recorder.a_list[0]) < recorder.plot_len:
                        for j in range(3):
                            recorder.a_list[j].append(round(self.a[j],3))
                            recorder.w_list[j].append(round(self.w[j],3))
                            recorder.Angle_list[j].append(round(self.Angle[j],3))
                    else:
                        for j in range(3):
                            recorder.a_list[j][0:recorder.plot_len-1] = recorder.a_list[j][-recorder.plot_len+1:]
                            recorder.a_list[j][-1] = round(self.a[j],3)
                            recorder.w_list[j][0:recorder.plot_len - 1] = recorder.w_list[j][-recorder.plot_len + 1:]
                            recorder.w_list[j][-1] = round(self.w[j], 3)
                            recorder.Angle_list[j][0:recorder.plot_len - 1] = recorder.Angle_list[j][-recorder.plot_len + 1:]
                            recorder.Angle_list[j][-1] = round(self.Angle[j], 3)
                    if recorder.is_record:
                        for j in range(3):
                            recorder.accel_list[j].append(round(self.a[j],3))
                            recorder.angular_speed_list[j].append(round(self.w[j],3))
                        recorder.counter += 1
                    recorder.ad_rdy_ev.set()
                    # print(a_list[0])
                    # print("a(g):%10.3f %10.3f %10.3f w(deg/s):%10.3f %10.3f %10.3f Angle(deg):%10.3f %10.3f %10.3f" % d)

                self.ByteNum += 1

    def get_acc(self, datahex):
        axl = datahex[0]
        axh = datahex[1]
        ayl = datahex[2]
        ayh = datahex[3]
        azl = datahex[4]
        azh = datahex[5]

        k_acc = 16.0

        acc_x = (axh << 8 | axl) / 32768.0 * k_acc
        acc_y = (ayh << 8 | ayl) / 32768.0 * k_acc
        acc_z = (azh << 8 | azl) / 32768.0 * k_acc
        if acc_x >= k_acc:
            acc_x -= 2 * k_acc
        if acc_y >= k_acc:
            acc_y -= 2 * k_acc
        if acc_z >= k_acc:
            acc_z -= 2 * k_acc

        return acc_x, acc_y, acc_z

    def get_gyro(self, datahex):
        wxl = datahex[0]
        wxh = datahex[1]
        wyl = datahex[2]
        wyh = datahex[3]
        wzl = datahex[4]
        wzh = datahex[5]
        k_gyro = 2000.0

        gyro_x = (wxh << 8 | wxl) / 32768.0 * k_gyro
        gyro_y = (wyh << 8 | wyl) / 32768.0 * k_gyro
        gyro_z = (wzh << 8 | wzl) / 32768.0 * k_gyro
        if gyro_x >= k_gyro:
            gyro_x -= 2 * k_gyro
        if gyro_y >= k_gyro:
            gyro_y -= 2 * k_gyro
        if gyro_z >= k_gyro:
            gyro_z -= 2 * k_gyro
        return gyro_x, gyro_y, gyro_z

    def get_angle(self, datahex):
        rxl = datahex[0]
        rxh = datahex[1]
        ryl = datahex[2]
        ryh = datahex[3]
        rzl = datahex[4]
        rzh = datahex[5]
        k_angle = 180.0

        angle_x = (rxh << 8 | rxl) / 32768.0 * k_angle
        angle_y = (ryh << 8 | ryl) / 32768.0 * k_angle
        angle_z = (rzh << 8 | rzl) / 32768.0 * k_angle
        if angle_x >= k_angle:
            angle_x -= 2 * k_angle
        if angle_y >= k_angle:
            angle_y -= 2 * k_angle
        if angle_z >= k_angle:
            angle_z -= 2 * k_angle

        return angle_x, angle_y, angle_z


def read_serial(recorder, serialReader):
    # use raw_input function for python 2.x or input function for python3.x
    # port = input(
    #     'please input port No. such as com7:');  # Python2软件版本用    port = raw_input('please input port No. such as com7:');*****************************************************************************************************
    # port = input('please input port No. such as com7:'));
    # baud = int(input('please input baudrate(115200 for JY61 or 9600 for JY901):'))
    # ser = serial.Serial(port, baud, timeout=0.5)
    ser = serial.Serial('com9', 230400, timeout=0.5)
    print(ser.is_open)
    while (1):
        datahex = ser.read(42)
        # print(datahex.hex())
        serialReader.DueData(datahex,recorder)
        sleep(0.01)


class Recorder:
    def __init__(self):
        self.ad_rdy_ev = Event()
        self.accel_list = [[],[],[]]
        self.angular_speed_list = [[],[],[]]
        self.motion_type = 0
        self.user_idx = 0
        self.is_record = False
        self.counter = 0
        self.timestamp = []
        self.a_list = [[], [], []]
        self.w_list = [[], [], []]
        self.Angle_list = [[], [], []]
        self.data_dic = {'acceleration': self.a_list, 'angular_speed': self.w_list, 'angle': self.Angle_list}
        self.range_dic = {'acceleration': 1.5, 'angular_speed': 200, 'angle': 180}
        self.plot_len = 100
        self.ani_list = []

        self.motion_type_pred = 14
        self.cnt_dic = {}
        for i in range(15):
            self.cnt_dic[i] = 0
        self.cur_type = 14

        self.predict_start = False
        self.timer_total = 0
        self.predict_reset = False
        self.headphone_state = 'Put on'

    def save_data(self):
        folder = './data'
        if not os.path.exists(folder):
            os.mkdir(folder)
        folder = os.path.join(folder, str(self.user_idx))
        if not os.path.exists(folder):
            os.mkdir(folder)
        folder = os.path.join(folder, str(self.motion_type))
        if not os.path.exists(folder):
            os.mkdir(folder)
        now = datetime.datetime.now()
        now_str = now.strftime('%Y-%m-%d-%H-%M-%S')
        folder = os.path.join(folder, now_str)
        if not os.path.exists(folder):
            os.mkdir(folder)
        np.savetxt(os.path.join(folder, 'accel.txt'), self.accel_list)
        np.savetxt(os.path.join(folder, 'angular_speed.txt'), self.angular_speed_list)
        np.savetxt(os.path.join(folder, 'timestamp.txt'), self.timestamp,fmt='%d')
        print('Save data successfully')

    def update_plot(self, line, data_list):
        def update(i):
            for k in range(3):
                ydata = data_list[k][max(-len(data_list[k]), -1*self.plot_len):]
                xdata = np.arange(self.plot_len - len(ydata), self.plot_len)
                line[k].set_xdata(xdata)
                line[k].set_ydata(ydata)
            return itertools.chain(line)

        return update

    def animate_acc(self, data_type):
        fig, ax = plt.subplots(1, 1)
        line = []

        label_list = ['x', 'y', 'z']
        color_list = ['blue', 'green', 'red']
        for i in range(3):
            line.append(lines.Line2D([], [], label=label_list[i], color=color_list[i]))
            ax.add_line(line[i])
        ax.set_xlim(0, self.plot_len)
        max_value = self.range_dic[data_type]
        ax.set_ylim(-max_value, max_value)
        ax.grid()
        ax.set_title(data_type)
        fig.legend()
        ani = animation.FuncAnimation(fig, self.update_plot(line, self.data_dic[data_type]), frames=1, interval=100, blit=True)
        self.ani_list.append(ani)
        fig.tight_layout()
        fig.show()
        return ani



def start_record(recorder, entry1, entry2):
    if not recorder.is_record:
        print('Start recording')
        recorder.motion_type = int(entry1.get())
        recorder.is_record = True
        user_idx = int(entry2.get())
        recorder.user_idx = user_idx

def stop_record(recorder, is_save):
    if recorder.is_record:
        print('Stop recording')
        recorder.ad_rdy_ev.wait()
        recorder.is_record = False
        if is_save:
            recorder.save_data()
        recorder.accel_list = [[],[],[]]
        recorder.angular_speed_list = [[],[],[]]
        recorder.counter = 0
        recorder.timestamp = []
        recorder.ad_rdy_ev.clear()

def add_timestamp(recorder):
    if recorder.is_record:
        recorder.ad_rdy_ev.wait()
        recorder.timestamp.append(recorder.counter)
        recorder.ad_rdy_ev.clear()
    print('Add timestamp', len(recorder.timestamp))

def delete_timestamp(recorder):
    if len(recorder.timestamp) > 0:
        recorder.timestamp.pop()
    print('Delete timestamp', len(recorder.timestamp))

def mainloop(recorder):
    root = tk.Tk()
    tk.Label(root, text="Motion Type Index: ").grid(row=0)
    entry1 = tk.Entry(root)
    entry1.insert(10, "0")
    entry1.grid(row=0, column=1)
    tk.Label(root, text="User Index: ").grid(row=1)
    entry2 = tk.Entry(root)
    entry2.insert(10, "0")
    entry2.grid(row=1, column=1)
    tk.Button(root, text='Start Record', command=lambda: start_record(recorder, entry1, entry2)).grid(row=2, column=0, sticky=tk.W, pady=10)
    tk.Button(root, text='Stop Record', command=lambda: stop_record(recorder, False)).grid(row=2, column=1, sticky=tk.W, pady=10)
    tk.Button(root, text='Stop Record and Save', command=lambda: stop_record(recorder, True)).grid(row=3, column=1, sticky=tk.W, pady=10)
    tk.Button(root, text='Add Timestamp', command=lambda: add_timestamp(recorder)).grid(row=4, column=0, sticky=tk.W, pady=10)
    tk.Button(root, text='Delete Timestamp', command=lambda: delete_timestamp(recorder)).grid(row=4, column=1, sticky=tk.W, pady=10)

    root.mainloop()

if __name__ == '__main__':
    recorder = Recorder()
    serialReader = SerialReader()
    t1 = Thread(target=read_serial, args=(recorder,serialReader), daemon=True)
    t1.start()
    ani = recorder.animate_acc('acceleration')
    ani2 = recorder.animate_acc('angular_speed')
    # ani3 = animate_acc('angle')
    mainloop(recorder)



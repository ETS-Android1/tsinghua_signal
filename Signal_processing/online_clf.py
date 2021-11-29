import os
import pickle
import time
from time import sleep

import serial
from threading import Thread
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import animation

from readSerialAdapter import SerialReader, Recorder, MOTION_TYPE
from motion_svm import pad_data, MAX_LEN


def read_serial(recorder, serialReader):
    ser = serial.Serial('com9', 230400, timeout=0.5)
    print(ser.is_open)
    while (1):
        datahex = ser.read(42)
        # print(datahex.hex())
        serialReader.DueData(datahex,recorder)
        sleep(0.01)


def predict_motion_type(recorder, clf_model, scaler):
    win_len = 5
    start_th = 15
    stop_th = 5
    is_start = False
    start_cnt = 1

    num_cnt = 0
    is_first_motion = True
    cur_type = -2
    while(1):
        recorder.ad_rdy_ev.wait()
        if recorder.predict_reset:
            is_start = False
            start_cnt = 1
            num_cnt = 0
            is_first_motion = True
            cur_type = -2
            recorder.predict_reset = False
        if len(recorder.w_list[-1]) > start_th:
            if not is_start:
                is_peak = False
                for j in range(3):
                    if abs(recorder.w_list[j][-1] - recorder.w_list[j][-1-win_len]) > start_th:
                        is_peak = True
                        break
                if is_peak:
                    is_start = True
                    # start_cnt += 1
            else:
                if start_cnt > 5:
                    is_static = True
                    for j in range(3):
                        if np.mean(abs(np.array(recorder.w_list[j][-1-win_len:-1]))) > stop_th:
                            is_static = False
                            break
                    feature = []
                    if is_static:
                        for j in range(3):
                            feature.extend(pad_data(recorder.a_list[j][-start_cnt-win_len:-1]))
                        for j in range(3):
                            feature.extend(pad_data(recorder.w_list[j][-start_cnt - win_len:-1]))
                        feature = np.array(feature).reshape((1,-1))
                        feature = scaler.transform(feature)
                        m_type = clf_model.predict(feature)[0]
                        recorder.motion_type_pred = m_type
                        print('m_type: ', recorder.motion_type_pred)

                        if m_type >= 10:
                            if m_type == cur_type:
                                num_cnt += 1
                            else:
                                cur_type = m_type
                                num_cnt = 1
                            recorder.cur_type = cur_type
                            is_first_motion = True
                        else:
                            if is_first_motion:
                                if cur_type != m_type:
                                    cur_type = m_type
                                    num_cnt = 0
                            else:
                                if cur_type == m_type + (-1)**(m_type%2):
                                    num_cnt += 1
                                    recorder.cur_type = cur_type
                                else:
                                    print('predict wrong')
                            is_first_motion = not is_first_motion
                        print('cur_type: ', recorder.cur_type)
                        if num_cnt == 5:
                            recorder.cnt_dic[cur_type] += 1
                            num_cnt = 0

                        is_start = False
                        start_cnt = 0
                start_cnt += 1
        recorder.predict_start = is_start
        recorder.ad_rdy_ev.clear()


def detect_state(recorder):
    state = 0 # 0:'STATIC', 1: 'LAY_DOWN'
    cnt_lay_down = 0
    cnt_put_on = 0
    start_time = time.time()
    delta_total_time = 0

    while(1):
        if state == 1:
            cnt_lay_down = 0
            if recorder.a_list[0][-1] >= 0.4:
                cnt_put_on += 1
                if cnt_put_on >= 30:
                    state = 0
                    print('Headphone is put on')
                    recorder.headphone_state = 'Put on'
        else:
            if len(recorder.a_list[0]) > 1 and recorder.a_list[0][-1] < 0.3:
                cnt_lay_down += 1
                if cnt_lay_down >= 30:
                    state = 1
                    print('Headphone is put down')
                    recorder.headphone_state = 'Put down'
                    recorder.predict_reset = False
                    recorder.motion_type_pred = 14
                    recorder.cur_type = 14
            else:
                cnt_lay_down = 0
                if recorder.predict_start:
                    if delta_total_time > 10:
                        recorder.predict_reset = True
                    delta_total_time = 0
                else:
                    end_time = time.time()
                    if end_time - start_time >= 1:
                        delta_total_time += 1
                        start_time = time.time()
                        if delta_total_time >= 15:
                            recorder.timer_total += 1
                            recorder.motion_type_pred = 14
                            recorder.cur_type = 14
        sleep(0.01)


def animate_motion_type(recorder):
    fig, ax = plt.subplots(1, 1)
    ax.set_xticks([])
    ax.set_yticks([])
    ax.set(xlim=(0, 8), ylim=(0, 8))
    ax.axis('off')

    str_cnt = ''
    for m_idx in [0,1,2,3,4,5,10,11,12,13]:
        str_cnt += MOTION_TYPE[m_idx] + ': ' + str(recorder.cnt_dic[m_idx]) +' group\n'
    t1 = ax.text(0.0, 0.5, 'Motion Type: ', fontsize=15)
    t2 = ax.text(0.0, 1, 'Current Type: ', fontsize=15)
    t3 = ax.text(0.0, 1.5, 'Headphone State: ', fontsize=15)
    t4 = ax.text(0.0, 2, 'Time in the Same Posture: ', fontsize=15)
    t5 = ax.text(0.0, 2.5, str_cnt, fontsize=15)

    def update(i):
        motion_type_name = MOTION_TYPE[recorder.motion_type_pred]
        cur_type_name = MOTION_TYPE[recorder.cur_type]
        t1.set_text('Motion Type: ' + motion_type_name)
        t2.set_text('Current Type: ' + cur_type_name + ' ' + str(recorder.cnt_dic[recorder.cur_type]))
        t3.set_text('Headphone State: ' + recorder.headphone_state)
        t4.set_text('Time in the Same Posture: ' + str(recorder.timer_total) + ' seconds')
        str_cnt = ''
        for m_idx in [0, 1, 2, 3, 4, 5, 10, 11, 12, 13]:
            str_cnt += MOTION_TYPE[m_idx] + ': ' + str(recorder.cnt_dic[m_idx]) + ' group\n'
        t5.set_text(str_cnt)
        return [t1, t2, t3, t4, t5]

    ani = animation.FuncAnimation(fig, update, frames=1, interval=100, blit=True)
    fig.show()
    # fig.tight_layout()
    return ani

if __name__ == '__main__':
    serialReader = SerialReader()
    recorder = Recorder()
    scale = pickle.load(open('./model/2021-11-28-23-40-42/scale.pkl', 'rb'))
    svm_model = pickle.load(open('./model/2021-11-28-23-40-42/svm_model.pkl', 'rb'))

    t1 = Thread(target=read_serial, args=(recorder, serialReader), daemon=True)
    t1.start()

    t2 = Thread(target=predict_motion_type, args=(recorder, svm_model, scale), daemon=True)
    t2.start()

    t3 = Thread(target=detect_state, args=(recorder,), daemon=True)
    t3.start()

    ani = recorder.animate_acc('acceleration')
    ani2 = recorder.animate_acc('angular_speed')
    ani3 = animate_motion_type(recorder)
    plt.show()

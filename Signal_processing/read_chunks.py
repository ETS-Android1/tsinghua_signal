import numpy as np
import os
import matplotlib.pyplot as plt
import wave

folder = os.getcwd() + '/data/0/2021-11-09-21-40-57/1/'
is_convert = True
is_print = not is_convert

if is_convert:
    raw_data = np.load(folder+'chunks.npy',allow_pickle=True)

    data_len, chunk_size = raw_data.shape
    frame_rate = 48000

    wave_data = raw_data.flatten().astype(np.short)
    file = folder+'chunks.wav'
    f = wave.open(file, "wb")
    f.setnchannels(1)
    f.setsampwidth(2)
    f.setframerate(frame_rate)
    f.writeframes(wave_data.tostring())
    f.close()

if is_print:
    raw_data = np.loadtxt(folder+'distances.txt')
    raw_dis = raw_data[:,2]
    plt.plot(raw_dis)
    plt.show()


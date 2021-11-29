import numpy as np
import matplotlib.pyplot as plt
import os

if __name__ == '__main__':
    folder = './data/3/12/2021-11-27-22-51-28'
    a_list = np.loadtxt(os.path.join(folder, 'accel.txt'), dtype=float)
    w_list = np.loadtxt(os.path.join(folder, 'angular_speed.txt'), dtype=float)
    timestamp_list = np.loadtxt(os.path.join(folder, 'timestamp.txt'), dtype=int)

    start_time = timestamp_list[0:len(timestamp_list):2]
    end_time = timestamp_list[1:len(timestamp_list):2]
    a_data = [[],[],[]]
    w_data = [[],[],[]]
    for i in range(len(start_time)):
        for j in range(3):
            a_data[j].append(a_list[j][start_time[i]:end_time[i]])
            w_data[j].append(w_list[j][start_time[i]:end_time[i]])

    # for j in range(3):
    #     plt.plot(w_list[j])
    # plt.show()

    label_list = ['x','y','z']
    # for i in range(len(a_data[0])):
    #     for j in range(3):
    #         plt.plot(a_data[j][i], label=label_list[j])
    #     plt.legend()
    #     plt.show()

    for i in range(len(w_data[0])):
        for j in range(3):
            plt.plot(w_data[j][i], label=label_list[j])
        plt.legend()
        plt.show()

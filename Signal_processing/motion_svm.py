import datetime
import os
import numpy as np
import random

from matplotlib.patches import Rectangle
from scipy import interpolate
from sklearn.metrics import f1_score, accuracy_score, confusion_matrix
from sklearn import svm
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
import pickle

MAX_LEN = 100

def pad_data(raw_data, is_interp=True):
    if is_interp:
        if len(raw_data) >= MAX_LEN:
            tmp = raw_data[:MAX_LEN]
        else:
            y0 = raw_data
            x0 = np.linspace(0, len(y0) - 1, len(y0))
            spl0 = interpolate.splrep(x0, y0)
            x1 = np.linspace(0, len(y0) - 1, MAX_LEN)
            tmp = interpolate.splev(x1, spl0)
    else:
        tmp = [0] * MAX_LEN
        tmp[:min(len(raw_data), MAX_LEN)] = raw_data[:min(len(raw_data), MAX_LEN)]
    return tmp

def plot_data_with_label(x, label, title='', text=''):
    # print('%s Fs = %d, x.shape = %s, x.dtype = %s' % (text, Fs, x.shape, x.dtype))
    fig = plt.figure(figsize=(8, 2))
    ax = fig.add_subplot(111)
    x_flat = np.array(x).reshape((1,-1)).squeeze()
    start_y = min(x_flat)
    heigh_y = max(x_flat) - min(x_flat)
    for i in range(len(label)):
        start_x = label[i][0]
        width_x = (label[i][1] - label[i][0])
        rect = Rectangle((start_x,start_y),width_x,heigh_y,linewidth=1,edgecolor='r',facecolor='none')
        ax.add_patch(rect)
    for j in range(3):
        plt.plot(x[j])
    plt.xlim([0, len(x[0])])
    plt.xlabel('Samples')
    plt.ylabel('Amplitude')
    plt.title(title)
    plt.tight_layout()
    plt.show()

def read_data(file_path):
    timestamp = np.loadtxt(os.path.join(file_path, 'timestamp.txt'), dtype=int)
    start_time = timestamp[0:len(timestamp):2]
    end_time = timestamp[1:len(timestamp):2]
    # print('time stamp len:', len(timestamp), 'start time len:', len(start_time), 'end time len:', len(end_time))
    accel_list = np.loadtxt(os.path.join(file_path,'accel.txt'), dtype=float)
    w_list = np.loadtxt(os.path.join(file_path,'angular_speed.txt'), dtype=float)
    res_list = []
    ex_len = 0
    for i in range(len(start_time)):
        res = []
        for j in range(3):
            a = accel_list[j][start_time[i]-ex_len:end_time[i]+ex_len]
            res.extend(pad_data(a))
        for j in range(3):
            w = w_list[j][start_time[i]-ex_len:end_time[i]+ex_len]
            res.extend(pad_data(w))
        res_list.append(res)
    return res_list


def auto_seg_data(file_path):
    accel_list = np.loadtxt(os.path.join(file_path, 'accel.txt'), dtype=float)
    w_list = np.loadtxt(os.path.join(file_path, 'angular_speed.txt'), dtype=float)
    res_list = []
    is_start = False
    start_idx = -1
    label_list = []
    i = 0
    win_len = 5
    while i < len(accel_list[0]) - win_len:
        if not is_start:
            is_peak = False
            for j in range(3):
                if abs(w_list[j][i+win_len] - w_list[j][i]) > 15:
                    is_peak = True
                    break
            if is_peak:
                is_start = True
                start_idx = i
                i += win_len
                # print('enter1')
        else:
            is_static = True
            for j in range(3):
                if np.mean(abs(w_list[j][i:i+win_len])) > 5:
                    is_static = False
                    break
            if is_static:
                # print('enter2')
                label_list.append([start_idx,i+win_len])
                is_start = False
                # i += win_len
                res = []
                for j in range(3):
                    a = accel_list[j][start_idx:i+win_len]
                    res.extend(pad_data(a))
                for j in range(3):
                    w = w_list[j][start_idx:i+win_len]
                    res.extend(pad_data(w))
                res_list.append(res)
        i += 1
    plot_data_with_label(w_list,label_list)
    return res_list


def data_split_one_user(label_set, feature_set, user_idx, round_num):
    feature_set_flat = []
    label_set_flat = []
    test_idx_list = []
    training_idx_list = []

    feature_list = np.array(feature_set[0,user_idx-1])
    label_list = np.array(label_set[0,user_idx-1])
    print(feature_list.shape)
    label_num = feature_list.shape[0]
    sample_num = feature_list.shape[1]
    feature_num = feature_list.shape[2]
    # print(feature_list.shape,label_list.shape)
    for i in range(label_num):
        np.random.shuffle(feature_list[i])
    feature_list = feature_list.swapaxes(1,0)
    # print('after swapaxes',feature_list.shape)
    label_list = label_list.T
    feature_list = feature_list.reshape((1,-1,feature_num))
    feature_set_flat = feature_list[0]
    label_list = label_list.reshape((1,-1))
    label_set_flat = label_list[0]
    group_num = int(len(label_set_flat)/round_num)
    for i in range(round_num):
        test_idx = np.arange(i*group_num, min((i+1)*group_num,len(label_set_flat)))
        training_idx = np.delete(np.arange(len(label_set_flat)),test_idx)
        test_idx_list.append(test_idx)
        training_idx_list.append(training_idx)

    test_idx_list = np.array(test_idx_list)
    training_idx_list = np.array(training_idx_list)
    print(feature_set_flat.shape, label_set_flat.shape, test_idx_list.shape, training_idx_list.shape)
    return feature_set_flat, label_set_flat, test_idx_list, training_idx_list


def data_split_cross_user(label_set, feature_set, user_num):
    feature_set_flat = []
    label_set_flat = []
    test_idx_list = []
    training_idx_list = []

    sample_num_total = 0
    feature_num = np.array(feature_set[0,0]).shape[-1]
    # print(feature_num)
    for i in range(user_num):
        feature_list = np.array(feature_set[0,i])
        label_list = np.array(label_set[0,i])

        # print(feature_list.shape[-1])

        feature_list = feature_list.reshape((1,-1,feature_num))
        print(feature_list.shape)
        feature_set_flat.extend(feature_list[0])
        label_list = label_list.reshape((1,-1))
        label_set_flat.extend(label_list[0])
        sample_num = label_list.shape[1]
        test_idx_list.append(np.arange(sample_num_total, sample_num_total+sample_num))
        sample_num_total += sample_num

    for i in range(user_num):
        training_idx_list.append(np.delete(np.arange(sample_num_total),test_idx_list[i]))

    return np.array(feature_set_flat), np.array(label_set_flat), np.array(test_idx_list), np.array(training_idx_list)


if __name__ == '__main__':
    user_num = 3
    motion_num = 10
    motion_list = [0,1,2,3,4,5,10,11,12,13]
    label_set = np.empty((1, user_num), dtype=object)
    feature_set = np.empty((1, user_num), dtype=object)
    max_len_list = []  # 59

    f1_score_list = []
    acc_score_list = []
    auto_seg = False
    save_model = False
    for user_idx in range(1, user_num+1):
        root_path = './data/' + str(user_idx) + '/'
        label_list = [[] for i in range(motion_num)]
        feature_list = [[] for i in range(motion_num)]
        for idx in range(len(motion_list)):
            motion_type = motion_list[idx]
            folder = os.path.join(root_path, str(motion_type))
            if (os.path.exists(folder)):
                files = os.listdir(folder)
                for file in files:
                    file_path = os.path.join(folder,file)
                    # print(file_path)
                    if os.path.isdir(file_path):
                        if auto_seg:
                            res_list = auto_seg_data(file_path)
                        else:
                            res_list = read_data(file_path)
                        if motion_type >=10:
                            feature_list[idx].extend(res_list)
                            label_list[idx].extend([motion_type]*len(res_list))
                        else:
                            feature_list[idx].extend(res_list[0:len(res_list):2])
                            feature_list[idx + (-1)**(idx%2)].extend(res_list[1:len(res_list):2])
                            label_list[idx].extend([motion_type]*int(len(res_list)/2))
                            label_list[idx + (-1)**(idx%2)].extend([motion_type+(-1)**(idx%2)]*int(len(res_list)/2))
                            # print(idx,len(feature_list[idx]), idx + (-1)**(idx%2),len(feature_list[idx + (-1)**(idx%2)]), len(res_list))
        feature_set[0,user_idx-1] = feature_list
        label_set[0, user_idx-1] = label_list

        print(len(feature_list[0]),len(label_list[6]), len(feature_list[0][0]))


    round_num = 10
    feature_set_flat, label_set_flat, test_idx_list, training_idx_list = data_split_one_user(label_set, feature_set, 1, round_num)
    # feature_set_flat, label_set_flat, test_idx_list, training_idx_list = data_split_cross_user(label_set, feature_set, user_num)
    # print(feature_set_flat.shape, label_set_flat.shape, test_idx_list.shape, training_idx_list.shape)

    for i in range(round_num):
        print('------leave {} as test set------'.format(i))
        training_set_feature = feature_set_flat[training_idx_list[i]]
        test_set_feature = feature_set_flat[test_idx_list[i]]
        training_set_label = label_set_flat[training_idx_list[i]]
        test_set_label = label_set_flat[test_idx_list[i]]

        scale = StandardScaler()

        scale_fit = scale.fit(training_set_feature)
        training_set_feature = scale_fit.transform(training_set_feature)
        test_set_feature = scale_fit.transform(test_set_feature)

        svm_model = svm.SVC()
        svm_model.fit(training_set_feature, training_set_label)
        pred_label = svm_model.predict(test_set_feature)
        tmp_f1_score = f1_score(test_set_label, pred_label, average='macro')
        tmp_acc = accuracy_score(test_set_label, pred_label)
        print('f1_score:', tmp_f1_score, 'acc:', tmp_acc)
        f1_score_list.append(tmp_f1_score)
        acc_score_list.append(tmp_acc)
        print(confusion_matrix(test_set_label, pred_label))

    print('avg_f1_score:', np.mean(np.array(f1_score_list)), 'avg_acc:', np.mean(np.array(acc_score_list)))

    if save_model:
        save_path = './model'
        if not os.path.exists(save_path):
            os.mkdir(save_path)
        now = datetime.datetime.now()
        now_str = now.strftime('%Y-%m-%d-%H-%M-%S')
        folder = os.path.join(save_path, now_str)
        if not os.path.exists(folder):
            os.mkdir(folder)
        pickle.dump(scale, open(os.path.join(folder, 'scale.pkl'), 'wb'))
        pickle.dump(svm_model, open(os.path.join(folder, 'svm_model.pkl'), 'wb'))
        print('Save model success!')

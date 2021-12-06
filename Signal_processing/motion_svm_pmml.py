import datetime
import os
import numpy as np

from scipy import interpolate
from sklearn.metrics import f1_score, accuracy_score, confusion_matrix
from sklearn import svm
from sklearn.preprocessing import StandardScaler
import pickle

from sklearn2pmml import sklearn2pmml
from sklearn2pmml.pipeline import PMMLPipeline
from pypmml import Model


from motion_svm import read_data, data_split_one_user

if __name__ == '__main__':
    user_num = 1
    motion_num = 10
    motion_list = [0, 1, 2, 3, 4, 5, 10, 11, 12, 13]
    label_set = np.empty((1, user_num), dtype=object)
    feature_set = np.empty((1, user_num), dtype=object)
    f1_score_list = []
    acc_score_list = []

    for user_idx in range(1, user_num + 1):
        root_path = './data/' + str(user_idx) + '/'
        label_list = [[] for i in range(motion_num)]
        feature_list = [[] for i in range(motion_num)]
        for idx in range(len(motion_list)):
            motion_type = motion_list[idx]
            folder = os.path.join(root_path, str(motion_type))
            if (os.path.exists(folder)):
                files = os.listdir(folder)
                for file in files:
                    file_path = os.path.join(folder, file)
                    # print(file_path)
                    if os.path.isdir(file_path):
                        res_list = read_data(file_path)
                        if motion_type >= 10:
                            feature_list[idx].extend(res_list)
                            label_list[idx].extend([motion_type] * len(res_list))
                        else:
                            feature_list[idx].extend(res_list[0:len(res_list):2])
                            feature_list[idx + (-1) ** (idx % 2)].extend(res_list[1:len(res_list):2])
                            label_list[idx].extend([motion_type] * int(len(res_list) / 2))
                            label_list[idx + (-1) ** (idx % 2)].extend(
                                [motion_type + (-1) ** (idx % 2)] * int(len(res_list) / 2))
        feature_set[0, user_idx - 1] = feature_list
        label_set[0, user_idx - 1] = label_list

        print(len(feature_list[0]), len(label_list[6]), len(feature_list[0][0]))

    round_num = 10
    feature_set_flat, label_set_flat, test_idx_list, training_idx_list = data_split_one_user(label_set, feature_set, 1, round_num)

    for i in range(round_num):
        print('------leave {} as test set------'.format(i))
        training_set_feature = feature_set_flat[training_idx_list[i]]
        test_set_feature = feature_set_flat[test_idx_list[i]]
        training_set_label = label_set_flat[training_idx_list[i]]
        test_set_label = label_set_flat[test_idx_list[i]]

        if i == round_num-1:
            scale = StandardScaler()
            svm_model = svm.SVC()
            # scale = PMMLPipeline([("scale", StandardScaler())])
            # svm_model = PMMLPipeline([("scale", scale),('classifier', svm_model)])
            svm_model = PMMLPipeline([('classifier', svm_model)])
        else:
            scale = StandardScaler()
            svm_model = svm.SVC()

            # scale_fit = scale.fit(training_set_feature)
            # training_set_feature = scale_fit.transform(training_set_feature)
            # test_set_feature = scale_fit.transform(test_set_feature)

        svm_model.fit(training_set_feature, training_set_label)
        pred_label = svm_model.predict(test_set_feature)
        tmp_f1_score = f1_score(test_set_label, pred_label, average='macro')
        tmp_acc = accuracy_score(test_set_label, pred_label)
        print('f1_score:', tmp_f1_score, 'acc:', tmp_acc)
        f1_score_list.append(tmp_f1_score)
        acc_score_list.append(tmp_acc)
        print(confusion_matrix(test_set_label, pred_label))

    print('avg_f1_score:', np.mean(np.array(f1_score_list)), 'avg_acc:', np.mean(np.array(acc_score_list)))

    save_model = True
    folder = ''
    if save_model:
        save_path = './model'
        if not os.path.exists(save_path):
            os.mkdir(save_path)
        now = datetime.datetime.now()
        now_str = now.strftime('%Y-%m-%d-%H-%M-%S')
        folder = os.path.join(save_path, now_str)
        if not os.path.exists(folder):
            os.mkdir(folder)
        # sklearn2pmml(scale, os.path.join(folder, 'scale.pmml'), with_repr=True)
        sklearn2pmml(svm_model, os.path.join(folder, 'svm_model.pmml'), with_repr=True)
        print('Save model success!')

    test = save_model
    if test:
        # scale = Model.fromFile(os.path.join(folder, 'scale.pmml'))
        model = Model.fromFile(os.path.join(folder, 'svm_model.pmml'))
        test_set_feature = feature_set_flat[test_idx_list[-1]]
        test_set_label = label_set_flat[test_idx_list[-1]]
        # test_set_feature = scale.fit_transform(test_set_feature)
        pred_label = model.predict(test_set_feature)
        print(model.outputNames)
        print(type(pred_label))
        pred_label = np.array(pred_label)[:,0]
        tmp_f1_score = f1_score(test_set_label, pred_label, average='macro')
        tmp_acc = accuracy_score(test_set_label, pred_label)
        print('f1_score:', tmp_f1_score, 'acc:', tmp_acc)
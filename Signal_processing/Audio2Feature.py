import datetime
import os
import pickle

import numpy as np

class Audio2Feature:

    def __init__(self, config, name=''):

        self.config = config
        self.frame_rate = config.frame_rate
        self.chunk_size = config.chunk_size
        self.name = name
        self.counter = 0
        self.chunks = []


    def push(self, data):

        self.chunks.append(data)
        self.counter += 1

    def save(self, folder):
        """
        Save state for offline processing.
        """
        if not os.path.exists(folder):
            os.mkdir(folder)

        np.array(self.chunks).dump(folder + '/chunks.npy')
        print(f'files saved in folder: {folder}')

    def get_timestamp(self):
        timestamp_item = datetime.datetime.now().timestamp() * 1000
        counter = self.counter
        return counter, timestamp_item

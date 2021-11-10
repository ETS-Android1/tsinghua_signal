import json

class Config:
    def __init__(self):
        # Audio config
        self.frame_rate = 48000
        self.sample_width = 2
        self.n_channels = 2 #CHANGE HERE
        self.total_channels = 2
        self.chunk_size = 2048
        
        self.input_device_id = [1] # For earbud mic connected to laptop
    
    def save(self, filename):
        with open(filename, 'w') as f:
            json.dump(self.__dict__, f, indent=4)

    def load(self, filename):
        with open(filename, 'r') as f:
            data = json.load(f)
        for attr in data:
            setattr(self, attr, data[attr])
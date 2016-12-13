from collections import Counter
import re
import subprocess
import os
import matplotlib.pyplot as plt

import pandas as pd


class ConnectedComponent(object):
    def __init__(self, cutoff, desc_string, df, verbose=True):
        self.cutoff = cutoff
        self.desc_string = desc_string
        self.df = df

        #self.parent_filename = None
        self.cc_number = self.df['ConnectedComponents'].unique()[0]


        self.enumerate_species_present()

    def enumerate_species_present(self):
        self.Counter = Counter(self.df['organism'])  # acts like a dict
        self.species_present = self.Counter.keys()
        self.num_species = len(self.species_present) #len(Counter.keys{})

    def __str__(self):
        string = ""
        for s, c in self.Counter.items():
            string += '{}: {}\n'.format(s, c)
        return string


class ConnectedComponentsDB(object):
    def __init__(self, cutoff, desc_string):
        self.cutoff = cutoff
        self.desc_string = desc_string

        # filename of the .tsv with component info
        self.filename = 'db_{}_{}.tsv'.format(desc_string, cutoff)
        print(self.filename)
        self.db_path = '../data_mining_Neo4j_v2_3_2/databases/'
        filepath = os.path.join(self.db_path, self.filename)
        print(filepath)
        self.filepath = filepath
        self.node_df = pd.read_csv(self.filepath, sep='\t')

        self.components = dict()
        self.parse_out_components()

    def parse_out_components(self):
        for key, df in self.node_df.groupby('ConnectedComponents'):
            print('component #: {}.  Shape: {}'.format(key, df.shape))
            self.components[key] = \
                ConnectedComponent(cutoff=self.cutoff,
                                   desc_string=self.desc_string,
                                   df=df)



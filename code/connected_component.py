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
    """
    Store info for all of the connected components in a single database.
    """
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
        self.num_components = len(self.components.keys())

    def parse_out_components(self):
        self.num_cc_2_species = 0

        for key, df in self.node_df.groupby('ConnectedComponents'):
            print('component #: {}.  Shape: {}'.format(key, df.shape))
            cc = ConnectedComponent(cutoff=self.cutoff,
                                    desc_string=self.desc_string, df=df)
            self.components[key] = cc
            if len(cc.Counter.keys()) > 1:
                self.num_cc_2_species += 1

        self.frac_components_cross_species = \
            self.num_cc_2_species/len(self.components)

        self.total_genes_in_connected_components = self.node_df.shape[0]

    def histogram_of_nodes(self):
        print('Cutoff {}: plot # of nodes for each connected component.'
              ''.format(self.cutoff))
        fig, ax = plt.subplots(1, 1, figsize=(5,3))
        #plt.yscale('log', nonposy='clip')
        plot_series = self.node_df.groupby('ConnectedComponents')['ConnectedComponents'].count()
        n_bins = plot_series.max()
        if n_bins > 50:
            n_bins = int(n_bins/2.)
        elif n_bins > 100:
            n_bins = int(n_bins/10.)
        plot_series.plot.hist(bins=n_bins, ax=ax)
        ax.set_xlabel('# genes(nodes) in connected component')
        ax.set_ylabel('# of components')
        plt.tight_layout()
        return fig

    def histogram_of_species(self):
        print('Cutoff {}: plot # of different species for each connected component.'
              ''.format(self.cutoff))
        fig, ax = plt.subplots(1, 1, figsize=(5,3))
        #plt.yscale('log', nonposy='clip')
        plot_series = self.node_df.groupby('ConnectedComponents')['organism'].nunique()
        n_bins = plot_series.max()
        if n_bins > 50:
            n_bins = int(n_bins/2.)
        plot_series.plot.hist(bins=n_bins, ax=ax)
        ax.set_xlabel('# species in connected component')
        ax.set_ylabel('# of components')
        plt.tight_layout()
        return fig

    def print_cross_species_connected_component_summaries(self):
        s = self.filename + ': {} components'.format(self.num_components) + '\n'
        for c_num, c in self.components.items():
            if c.num_species > 1:
                s += '--- component {}: --- \n'.format(c_num)
                s += str(c)
        return s

    def __str__(self):
        s = self.filename + ': {} components'.format(self.num_components) + '\n'
        for c_num, c in self.components.items():
            s += '--- component {}: --- \n'.format(c_num)
            s += str(c)
        return s



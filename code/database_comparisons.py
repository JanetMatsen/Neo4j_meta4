import re
import subprocess
import os
import matplotlib.pyplot as plt

import pandas as pd


class Database:
    def __init__(self, cutoff, desc_string, verbose=True):
        self.verbose=verbose
        self.previously_generated = False # written over if db is loaded

        self.cutoff = cutoff
        self.desc_string = desc_string # e.g. 'binary' or '50M'
        if desc_string == 'binary':
            self.build_jar_path='../jars/ConstructBinaryNetwork.jar'
            self.cc_jar_path='../jars/ConnectedComponentsFinderBinary.jar'
        elif desc_string == '50M':
            self.build_jar_path='../jars/ConstructNetwork50M.jar'
            self.cc_jar_path='../jars/ConnectedComponentsFinder50M.jar'
        self.db_path = '../data_mining_Neo4j_v2_3_2/databases/db_{}_{}'.format(
            desc_string, cutoff)

        self.db_construction_stdout = None
        self.db_construction_time = None
        self.nodes = None
        self.edges = None # aka relationships
        self.density = None # num edges/num nodes

        self.db_connected_components_stdout = None
        self.connected_components = None
        self.cc_time = None

        self.create_or_load_db()
        self.find_connected_components()



    def create_subprocess_path(self, type, method):
        # E.g.
        path = os.path.join(self.db_path, type + '_' + method + '.txt')
        dirname = os.path.dirname(path)
        if os.path.exists(dirname):
            print("path {} already exists".format(dirname))
        else:
            print('make path {}'.format(dirname))
            os.mkdir(dirname)
        assert os.path.exists(dirname), 'error for {}'.format(path)
        return path

    def create_db(self):
        command = ['java', '-jar', self.build_jar_path, str(self.cutoff)]
        stdout_path = self.create_subprocess_path(type='stdout',
                                                  method='build')
        stderr_path = self.create_subprocess_path(type='stderr',
                                                  method='build')

        print('save stdout, stderr for create_db() to {}, {}'.format(
            stdout_path, stderr_path))
        stdout_file = open(stdout_path, 'w+')
        stderr_file = open(stderr_path, 'w+')

        if self.verbose:
            print('command: \n {}'.format(" ".join(command)))
        result = subprocess.check_output(command, stderr=stderr_file)
        result = result.decode('utf-8')

        stdout_file.write(result)
        stdout_file.close()
        stderr_file.close()

        self.db_construction_stdout = result
        self.parse_create_db_output()

        # TODO: write stdout to file in the right dir.

    def parse_create_db_output(self):
        stdout = self.db_construction_stdout
        assert stdout is not None, 'stdout is None'
        nodes_edges = re.findall('after network construction: (\d+), (\d+)',
                                 stdout)
        print(nodes_edges)
        assert len(nodes_edges) == 1, 'expected one count for nodes and one for edges'
        nodes, edges = nodes_edges[0]

        if len(re.findall('Graph density: NaN', str(stdout))) == 1:
            # indicates zero edges.  Divide by zero --> NaN from Java
            print('Graph density is NaN')
            self.density = None
        else:
            density = re.findall('Graph density: (\d*\.\d+|[-+]?\d+)',
                                 str(stdout))
            assert len(density) == 1, 'should only report one density.  ' \
                                      'Found {}'.format(density)
            print(density)
            self.density = density[0]

        # find db construction time
        seconds = re.findall('Network construction time \(seconds\): (\d+)',
                             stdout)
        assert len(seconds) == 1, 'expected one match for seconds; ' \
                             'got {}'.format(seconds)
        seconds = seconds[0]
        seconds = float(seconds)
        self.construction_seconds = seconds

        self.nodes = int(nodes)
        self.edges = int(edges) # aka relationships

    def find_connected_components(self):
        command = ['java', '-jar', self.cc_jar_path,
                   '../data_mining_Neo4j_v2_3_2/databases/db_{}_{}'.format(
                   self.desc_string, self.cutoff)]

        stdout_path = self.create_subprocess_path(type='stdout',
                                                  method='cc')
        stderr_path = self.create_subprocess_path(type='stderr',
                                                  method='cc')
        stdout_file = open(stdout_path, 'w+')
        stderr_file = open(stderr_path, 'w+')

        result = subprocess.check_output(command)
        result = result.decode('utf-8')

        stdout_file.write(result)
        stdout_file.close()
        stderr_file.close()

        self.db_connected_components_stdout = result
        self.parse_connected_components_stdout()

    def parse_connected_components_stdout(self):
        results = self.db_connected_components_stdout
        result_sentence = re.findall(r'There are \d+ different connected '
                                     'components for cutoff \d+.\d+', results)[0]
        cc = re.findall('(\d+) different', result_sentence)
        assert len(cc) == 1, 'expected one count of connected components; ' \
                             'got {}'.format(cc)
        cc = cc[0]
        cc = int(cc)
        self.connected_components = cc

        print(results)
        time = re.search(r'Connected Components time \(seconds\): ([\d]+)',
                          results)
        assert len(time.groups()) == 1, 'needed to match one time; found {}'.format(time.groups())
        time = float(time.groups()[0])
        self.cc_time = time

    def load_existing_db(self):
        print('Loading prevoiusly created db: {}'.format(self.db_path))
        assert os.path.exists(self.db_path), \
            "database {} doesn't exist.".format(self.db_path)

        stdout_build = self.create_subprocess_path(type='stdout', method='build')
        with open(stdout_build, 'r') as myfile:
            build_string = myfile.read()
        self.db_construction_stdout = build_string
        self.parse_create_db_output()

        # do same for connected components
        stdout_cc = self.create_subprocess_path(type='stdout', method='cc')
        with open(stdout_cc, 'r') as myfile:
            cc_string = myfile.read()

        self.db_connected_components_stdout = cc_string
        self.parse_connected_components_stdout()

        self.previously_generated = True

    def create_or_load_db(self):
        # todo: first check if db exists; create it if not.
        if os.path.exists(self.db_path):
            print('database {} exists.'.format(self.db_path))
            self.load_existing_db()
        else:
            print('create database (b/c not found)')
            self.create_db()

    def load_or_find_cc_info(self):
        # TODO: only find cc if hasn't already been run.
        self.find_connected_components()
        self.parse_connected_components_stdout()

    def summary_df(self):
        info = {}
        info['cutoff'] = self.cutoff
        info['nodes'] = self.nodes
        info['edges'] = self.edges
        info['density'] = self.density
        info['connected components'] = self.connected_components
        info['connected components time'] = self.cc_time
        info['db path'] = self.db_path
        info['construction seconds'] = self.construction_seconds
        info['previously_generated'] = self.previously_generated
        return pd.DataFrame({k:[v] for k, v in info.items()})

class DatabaseComparison:
    """
    Written specifically for the 50M version of the database.
    Eventually consider refactoring to a more general structure.
    """
    def __init__(self, desc_string):
        self.desc_string = desc_string
        self.databases = dict()
        self.database_count = 0
        self.summary = pd.DataFrame()

    def make_db(self, cutoff, verbose=True):
        if (self.summary.shape[0]) > 0 and \
                (cutoff in self.summary['cutoff'].tolist()):
            print('cutoff {} already represented in object.'.format(cutoff))
            return

        self.database_count += 1
        db = Database(cutoff=cutoff,
                      desc_string=self.desc_string,
                      verbose=verbose)
        self.databases[self.database_count] = db
        results_row = db.summary_df()
        self.summary = pd.concat([self.summary, results_row], axis=0)

    def make_dbs(self, cutoff_list, verbose=False):
        for c in cutoff_list:
            self.make_db(cutoff=c, verbose=verbose)

    def plot_base(self, x, y, color, df=None, title=None,
                  figsize=(3, 2.5), filename=None):
        if df is None:
            plot_data = self.summary
        else:
            plot_data = df

        fig, ax = plt.subplots(1, 1, figsize=figsize)
        plt.plot(plot_data[x], plot_data[y],
                 linestyle='--', marker='o', c='g')
        plt.xlabel(x)
        plt.ylabel(y)
        ax.set_ylim(bottom=0)

        if title is not None:
            plt.title(title)
        #ax.legend(loc='center left', bbox_to_anchor=(1, 0.5))

        plt.tight_layout()
        if filename is not None:
            fig.savefig(filename)

        return fig

    def plot_cc_vs_cutoff(self, figsize=None):
        return self.plot_base(x='cutoff', y='connected components',
                              color='#756bb1', figsize=figsize)

    def plot_db_construction_time_vs_cutoff(self, figsize=None):
        return self.plot_base(x='cutoff', y='construction seconds',
                              color='#756bb1', figsize=figsize)

    def plot_db_construction_time_vs_n_nodes(self, figsize=None):
        return self.plot_base(x='nodes', y='construction seconds',
                              color='#756bb1', figsize=figsize)

    def plot_cc_time_vs_cutoff(self, figsize=None):
        return self.plot_base(x='cutoff', y='connected components time',
                              color='#756bb1', figsize=figsize)


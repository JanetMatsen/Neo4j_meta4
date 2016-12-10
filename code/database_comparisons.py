import re
import subprocess
import os

import pandas as pd


class Database:
    def __init__(self, cutoff, desc_string, verbose=True):
        self.verbose=verbose

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
        self.connected_components = None # TODO

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

        self.nodes = int(nodes)
        self.edges = int(edges) # aka relationships

    def find_connected_components(self):
        command = ['java', '-jar', self.cc_jar_path,
                   '../data_mining_Neo4j_v2_3_2/databases/db_{}_{}'.format(
                   self.desc_string, self.cutoff)]
        result = subprocess.check_output(command)
        result_stdout_string = result.decode('utf-8')
        #print(result_stdout_string)
        self.db_connected_components_stdout = result_stdout_string
        print(result.decode('utf-8'))

        self.parse_connected_components_stdout()

    def parse_connected_components_stdout(self):
        results = self.db_connected_components_stdout
        result_sentence = re.findall(r'There are \d+ different connected '
                                     'components for cutoff \d+.\d+', results)[0]
        print(result_sentence)
        cc = re.findall('(\d+) different', result_sentence)
        assert len(cc) == 1, 'expected one count of connected components; ' \
                             'got {}'.format(cc)
        cc = cc[0]
        cc = int(cc)
        self.connected_components = cc

    def load_existing_db(self):
        print('Loading {}'.format(self.db_path))

    def create_or_load_db(self):
        # todo: first check if db exists; create it if not.
        if os.path.exists(self.db_path):
            print('database {} exists.'.format(self.db_path))
            self.load_existing_db()
        else:
            self.create_db()
            self.parse_create_db_output()

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
        info['db path'] = self.db_path
        return pd.DataFrame({k:[v] for k, v in info.items()})

class DatabaseComparison:
    """
    Written specifically for the 50M version of the database.
    Eventually consider refactoring to a more general structure.
    """
    def __init__(self):
        self.databases = None
        pass

    def run_threshold(self):
        pass

    def parse_db_building_stdout(stdout):
        #print(stdout)
        nodes_edges = re.findall(
            'after network construction: (\d+), (\d+)',
            str(stdout))
        print(nodes_edges)
        assert len(nodes_edges) == 1, \
            'expected one count for nodes and one for edges'
        nodes, edges = nodes_edges[0]

        density = re.findall('Graph density: (\d*\.\d+|[-+]?\d+)', str(stdout))
        assert len(density) == 1, 'should only report one density.  Found {}'.format(density)
        print(density)
        return nodes, edges, density[0]
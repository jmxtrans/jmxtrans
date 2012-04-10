#!/usr/bin/env python2.6
# -*- coding: latin-1 -*-
# vim:ai:expandtab:ts=4 sw=4
#
# yaml2jmxtrans.py: Generate jmxtrans config from YAML format
#    Copyright (C) 2012  Florian Thiel <f.thiel@tarent.de>, tarent solutions GmbH
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as
#    published by the Free Software Foundation, either version 3 of the
#    License, or (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

import yaml, json, sys

class Queries(object):
    """
    Generate query object snippets suitable for consumption by jmxtrans
    """
    def __init__(self, queries, query_attributes, graphite_host, graphite_port):
        """
        Initialize Queries configuration with data from YAML structure, making
        named queries accessible by name
        """
        self.queries = {}
        self.query_attributes = query_attributes
        self.graphite_host = graphite_host
        self.graphite_port = graphite_port

        for query in queries:
            queryentry = {}
            for attribute in query_attributes:
                queryentry[attribute] = query[attribute]
                self.queries[query['name']] = queryentry

    def create_query_entry(self, query_name):
        """
        Create a query snippet for the query referenced by 'query_name',
        including Graphite writer configuration
        """
        queryentry = { 'outputWriters' : self.create_output_writers() }
        for attr in self.query_attributes:
            queryentry[attr] = self.queries[query_name][attr]
        return queryentry

    def create_host_entry(self, host_name, query_names, query_port):
        """
        Create a query snippet for 'host_name' with all queries given
        by 'query_names'
        """
        hostentry = { 'host' : host_name,
                      'port' : query_port,
                      'queries' : [] }
        for query_name in query_names:
            hostentry['queries'].append(self.create_query_entry(query_name))
        hostentry['numQueryThreads'] = len(hostentry['queries'])
        return hostentry

    def create_host_set_configuration(self, host_names, query_names, query_port):
        """
        Return the full jmxtrans compatible configuration for all 'host_names',
        each using all queries given by 'query_names'. 'query_port' is used to
        contact the JMX server on all hosts
        """
        root = {'servers' : [] }
        for host_name in host_names:
            root['servers'].append(self.create_host_entry(host_name, query_names, query_port))
        return root

    def create_output_writers(self):
        """
        Graphite output writer snippet template
        """
        writer = {
            '@class' : 'com.googlecode.jmxtrans.model.output.GraphiteWriter',
            'settings' : {
                'port' : self.graphite_port,
                'host' : self.graphite_host,
                'typeNames' : [ "name" ],
                }
            }
        return [ writer ]

class HostSets(object):
    """
    Simple access to host/query entries given in custom YAML format
    """
    
    def __init__(self, host_sets):
        """
        Initialize host sets from YAML format
        """
        
        self.host_sets = {}
        
        for host_set in host_sets:
            set_entry = {'query_names' : host_set['query_names'],
                         'hosts' : host_set['hosts'] }
            self.host_sets[host_set['setname']] = set_entry

    def set_names(self):
        """
        Return names of all sets loaded into HostSets
        """
        return self.host_sets.keys()

    def get_set(self, set_name):
        """
        Get dict{'hosts': [], 'query_names': []} for the named set
        """
        return self.host_sets[set_name]

if __name__ == '__main__':
    # query attributes to copy
    query_attributes = ["obj", "resultAlias", "attr"]

    infile = open('jmxtrans.yaml', 'r')
    yf = yaml.load(infile)
    query_port = yf['query_port']

    q = Queries(yf['queries'], query_attributes, yf['graphite_host'], yf['graphite_port'])
    h = HostSets(yf['sets'])

    for set_name in h.set_names():
        outfile = open(set_name + ".json", 'w')
        s = h.get_set(set_name)
        servers = q.create_host_set_configuration(s['hosts'],s['query_names'], query_port)
        json.dump(servers,outfile, indent=1)
        outfile.close()

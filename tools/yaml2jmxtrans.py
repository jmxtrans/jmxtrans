#!/usr/bin/env python2.6
# -*- coding: latin-1 -*-
# vim:ai:expandtab:ts=4 sw=4

# yaml2jmxtrans.py: Generate jmxtrans config from YAML format
# Copyright 2012 Florian Thiel <f.thiel@tarent.de>, tarent solutions GmbH
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import yaml, json, sys
from string import Template

class Queries(object):
    """
    Generate query object snippets suitable for consumption by jmxtrans
    """
    def __init__(self, queries, query_attributes, monitor_host, monitor_port):
        """
        Initialize Queries configuration with data from YAML structure, making
        named queries accessible by name
        """
        self.queries = {}
        self.query_attributes = query_attributes
        self.monitor_host = monitor_host
        self.monitor_port = monitor_port

        for query in queries:
            queryentry = {}
            for attribute in query_attributes:
                if attribute in query:
                  queryentry[attribute] = query[attribute]
                else:
                  queryentry[attribute] = None
            self.queries[query['name']] = queryentry

    def create_query_entry(self, query_name):
        """
        Create a query snippet for the query referenced by 'query_name',
        including Graphite writer configuration
        """
        queryentry = {}

        # This supplies the default 'typeName' of 'name' to the graphite_output_writer
        # if an alternative is not explicitly specified in a query definition
        if self.queries[query_name]["typeName"] == None:
            typeName = "name"
        else:
            typeName = self.queries[query_name]["typeName"]

        for attr in self.query_attributes:
            # Ignore typeName so it doesn't also appear in the query section
            if attr <> "typeName":
                queryentry[attr] = self.queries[query_name][attr]
        # If we did not specify an "attr", don't pass it to the query. JMXTrans will 
        # poll ALL attributes in the MBEAN
        if queryentry["attr"] == None:
           del queryentry["attr"] 
        queryentry['outputWriters'] = self.create_graphite_output_writer(typeName)
        return queryentry

    def create_host_entry(self, host_name, query_names, query_port, username, password, urlTemplate):
        """
        Create a query snippet for 'host_name' with all queries given
        by 'query_names'
        """
        hostentry = { 'host' : host_name,
                      'port' : query_port,
                      'queries' : [] }
        for query_name in query_names:
            hostentry['queries'].append(self.create_query_entry(query_name))

        if username:
            hostentry['username'] = username

        if password:
            hostentry['password'] = password

        if urlTemplate:
            url = Template(urlTemplate)
            hostentry['url'] = url.substitute(hostname=host_name, query_port=query_port)

        hostentry['numQueryThreads'] = len(hostentry['queries'])
        return hostentry

    def create_host_set_configuration(self, host_names, query_names, query_port, username, password, urlTemplate):
        """
        Return the full jmxtrans compatible configuration for all 'host_names',
        each using all queries given by 'query_names'. 'query_port' is used to
        contact the JMX server on all hosts
        """
        root = {'servers' : [] }
        for host_name in host_names:
            root['servers'].append(self.create_host_entry(host_name, query_names, query_port, username, password, urlTemplate))
        return root

    def create_graphite_output_writer(self, typeName):
        """
        Graphite output writer snippet template
        """
        writer = {
            '@class' : 'com.googlecode.jmxtrans.model.output.GraphiteWriter',
            'settings' : {
                'port' : self.monitor_port,
                'host' : self.monitor_host,
                'typeNames' : [ typeName ],
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
            if 'username' in host_set:
                set_entry['username'] = host_set['username']
            else:
                set_entry['username'] = None

            if 'password' in host_set:
                set_entry['password'] = host_set['password']
            else:
                set_entry['password'] = None

            if 'urlTemplate' in host_set:
                set_entry['urlTemplate'] = host_set['urlTemplate']
            else:
                set_entry['urlTemplate'] = None

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

def usage():
    print "Usage: " + sys.argv[0] + " INPUT.yaml"

if __name__ == '__main__':
    # query attributes to copy
    query_attributes = ["obj", "resultAlias", "attr", "typeName"]

    if len(sys.argv) != 2:
        usage()
        sys.exit(1)

    infile = open(sys.argv[1], 'r')
    yf = yaml.load(infile)
    query_port = yf['query_port']

    q = Queries(yf['queries'], query_attributes, yf['graphite_host'], yf['graphite_port'])
    h = HostSets(yf['sets'])

    for set_name in h.set_names():
        outfile = open(set_name + ".json", 'w')
        s = h.get_set(set_name)
        servers = q.create_host_set_configuration(s['hosts'],s['query_names'], query_port, s['username'], s['password'], s['urlTemplate'])
        json.dump(servers,outfile, indent=1)
        outfile.close()

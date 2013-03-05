# yaml2jmxtrans.py #
_yaml2jmxtrans.py_ allows you to write _jmxtrans_ query configurations
using a simple [YAML][YAML] format.

The custom YAML format is more concise through letting you specify
your quieries in one place and the hosts that use that use the queries
in another one ([DRY][DRY] principle).

YAML format
----------
Look at the _jmxtrans-demo.yaml_ file which gives an example of the
format.

Usage
----------
    yaml2jmxtrans.py INFILE.yaml
    
Generates JSON jmxtrans configuration files, one for each host set
defined in _INFILE.yaml_.

Dependencies
----------
* python 2.6
* [PyYAML][PyYAML]

Limitations
----------
* _yaml2trans.py_ only supports [Graphite][Graphite] as an output
  writer as that's what I use. Please file issues if you require
  other output writers.
* EXPERIMENTAL: The "outputWriters:" map in the YAML configuration file supports other
  output writers. Please file issues if there are problems.

[YAML]: http://yaml.org/
[PyYAML]: http://pyyaml.org/
[DRY]: http://de.wikipedia.org/wiki/Don%E2%80%99t_repeat_yourself
[Graphite]: http://graphite.wikidot.com/

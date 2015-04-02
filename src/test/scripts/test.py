#!/usr/bin/env python

from urllib2 import urlopen
from sys import argv

if __name__ == '__main__':
	n_iter = 1
	if len(argv) > 1:
		n_iter = int(argv[1])
	for i in range(0,n_iter):
		for j in range(1,11):
			f = urlopen('http://localhost:1234/stuff/stuff%d.html' % j)
			f.read()


#!/bin/bash

if [ $# -eq "0" ]; then
	iterations=10
else
	iterations=$1
fi

for i in `seq 1 $iterations`; do
	for j in `seq 1 10`; do
		echo "Fetching http://localhost:1234/stuff/stuff$j.html"
		curl -s -S http://localhost:1234/stuff/stuff$j.html > /dev/null
	done
done


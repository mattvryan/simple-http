# simple-http
A simple file-based HTTP server, for learning purposes.

### About
I can't imagine you would use this in any sort of a production environment with so many other, more full-featured HTTP servers out there, but this one seems to work just fine as a file-based HTTP server.

Supported methods are GET and HEAD.  Supported HTTP version is 1.1 only.

#### Configuration Settings
Configuration can be placed in /etc/simple_http/config.properties.  Configuration is comprised of simple key/value pairs separated by "=".

Configuration options:
* *org.mvryan.simple-http.document-root* - Location to look for files.  The default is a directory named "public_html" in the running user's home directory.
* *org.mvryan.simple-http.allow-directory-index* - Set to "True" if you want to enable the generation of a directory index page for directories not containing a default file (e.g. index.htm[l], default.htm[l]).  The default is False.
* *org.mvryan.simple-http.cache-enabled* - Set to "True" if you want to enable temporary caching of responses.  The default is False.
* 

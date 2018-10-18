#!/usr/local/bin/perl
# hello.pl - My first CGI program

print "Content-Type: text/html\n\n";
# Note there is a newline between 
# this header and Data

# Simple HTML code follows

print "<html> <head>\n";
print "<title>Hello, Perl CGI!</title>";
print "</head>\n";
print "<body>\n";
print "<h1>Hello, Perl CGI!</h1>\n";
print "</body> </html>\n";
#!/usr/bin/perl use strict; 
use warnings; 
use CGI; 
use CGI::Carp qw(fatalsToBrowser warningsToBrowser); 
use File::Tail; 

	my $cgi = CGI->new; 
	my $fpath = $cgi->param('fpath');
	print $cgi->header, $cgi->start_html;
	warningsToBrowser(1); 
	$|++;
	my $file = File::Tail->new($fpath); 
	while ( my $line = $file->read ) { 
		print $cgi->escapeHTML($line); 
	} 
	$cgi->end_html;

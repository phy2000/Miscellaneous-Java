#!/usr/bin/perl use strict; 
use warnings; 
use CGI; 
use CGI::Carp qw(fatalsToBrowser warningsToBrowser); 
#use CGI qw [ -nph :standard ];
use File::Tail; 

	my $cgi = CGI->new; 
	my $filepath = $cgi->param("fpath");
	print $cgi->header, $cgi->start_html;
	warningsToBrowser(1); 
	$|++;
	if ($pid = fork()) {
	print $cgi->escapeHTML($pid), "<br>";
		print $cgi->end_html;
		close(STDOUT);
		close(STDERR);
		# exit(0);
	} else {
		if (1) {
			open(my $fh, '<:encoding(UTF-8)', $filepath)
  			or die "Could not open file '$filepath' $!";
		 
			while (my $row = <$fh>) {
 				chomp $row;
 				print "\n";
				print $cgi->escapeHTML($row);
				print ("<br>");
				sleep 1;
		  	}
	  	} else {
			my $file = File::Tail->new($filepath); 
		 
			while ( my $line = $file->read ) { 
				print $cgi->escapeHTML($line); 
				print ("<br>");
			}
		}
 	} 
	print $cgi->end_html;

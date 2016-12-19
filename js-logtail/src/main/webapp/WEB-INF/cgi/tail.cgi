#!/usr/bin/perl 
use strict; 
use warnings; 
use CGI; 

# parameters: fpath, start, endName, textName
	my $cgi = CGI->new; 
	my $fpath = $cgi->param("fpath");
	my $start = $cgi->param("start");
	my $endName = $cgi->param("endName");
	my $textName = $cgi->param("textName");
	my $end = -s $fpath;
	my $inbuf;

	open(my $fh, '<:encoding(UTF-8)', $fpath)
		or die "Could not open file '$fpath' $!";

	print $cgi->header;

	if ($start > $end) {
		$start = 0;
	}
		
	my $length = $end - $start;
	
	if ($length != 0) {
		if ($start != 0) {
			seek($fh, $start, 0);
		}
		read ($fh, $inbuf, $length);
		$inbuf =~ s/\r//g;
		$inbuf =~ s/\n/\\n/g;
		print "$textName=\"$inbuf\";\n";
	}
	print ("$endName=$end;\n");
	
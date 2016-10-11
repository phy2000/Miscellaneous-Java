use File::Tail;
my $ref=tie *FH,"File::Tail",(name=>$name);
while (<FH>) {
    print "$_";
}
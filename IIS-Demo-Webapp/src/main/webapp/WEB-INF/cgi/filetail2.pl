use File::Tail;
  $file=File::Tail->new(name=>$name, maxinterval=>300, adjustafter=>7);
  while (defined($line=$file->read)) {
      print "$line";
  }
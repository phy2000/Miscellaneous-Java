use File::Tail;
  $file=File::Tail->new("/some/log/file");
  while (defined($line=$file->read)) {
      print "$line";
  }

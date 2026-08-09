#!/bin/bash

# ImageMagick's `convert` is not installed in this environment,
# so the PDF is rendered with ghostscript instead.
# -g400x300 sets the output size (NEMU screen); -dFIXEDMEDIA -dPDFFitPage
# scale the whole page into that size (equivalent to `-resize 400x300!`;
# without them gs renders at 72dpi and crops the bottom-left quarter).
gs -q -dNOPAUSE -dBATCH -dFIXEDMEDIA -dPDFFitPage -sDEVICE=bmp16m -g400x300 \
   -sOutputFile=slides-%d.bmp slides.pdf

# gs numbers pages from 1, while nslider expects slides-0.bmp, slides-1.bmp, ...
shopt -s nullglob
for f in slides-*.bmp; do
  n=${f#slides-}; n=${n%.bmp}
  mv "$f" "$(printf 'slides-%d.bmp' $((n - 1)))"
done
shopt -u nullglob

mkdir -p $NAVY_HOME/fsimg/share/slides/
rm -f $NAVY_HOME/fsimg/share/slides/*
mv *.bmp $NAVY_HOME/fsimg/share/slides/

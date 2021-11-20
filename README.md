# Frenda

Compile large FIRRTL files incrementally.

## Getting Started

Frenda is a tool for incremental compilation of FIRRTL. It can split FIRRTL into modules, and then compile all the modules incrementally.

For building and using Frenda, you can run the following command lines:

```sh
# build Frenda
sbt assembly
# show help messages
utils/bin/frenda --help
# compile FIRRTL into Verilog files
utils/bin/frenda -i input.fir -td output_dir
```

## Pros and Cons

Frenda can effectively reduce the compilation time of FIRRTL. but at the cost that Frenda cannot do cross-module optimization for the input circuit, such as global DCE.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Copyright and License

Copyright (C) 2010-2021 MaxXing. License GPLv3.

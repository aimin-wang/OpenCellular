# included by kernel recipes if they want to build/provide
# perf functionality from their tree.
do_compile_perf_libc-uclibc () {
	:
}
do_install_perf_libc-uclibc () {
	:
}
do_compile_perf() {
	oe_runmake -C ${S}/tools/perf CC="${CC}" LD="${LD}" prefix=${prefix} NO_NEWT=1 NO_DWARF=1
}

do_install_perf() {
	oe_runmake -C ${S}/tools/perf CC="${CC}" LD="${LD}" prefix=${prefix} DESTDIR=${D} install NO_NEWT=1 NO_DWARF=1
}


# perf tasks
# TODO: enable with newer kernels
#addtask compile_perf after do_compile before do_install
#addtask install_perf after do_install before do_package


PERFDEPENDS = "virtual/libc:do_populate_sysroot elfutils:do_populate_sysroot"
PERFDEPENDS_libc-uclibc = ""
PERFRDEPENDS = "python perl elfutils"
PERFRDEPENDS_libc-uclibc = ""
do_compile_perf[depends] = "${PERFDEPENDS}"
RDEPENDS_perf += "${PERFRDEPENDS}"

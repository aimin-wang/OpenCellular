SUMMARY = "Network Time Protocol daemon and utilities"
DESCRIPTION = "The Network Time Protocol (NTP) is used to \
synchronize the time of a computer client or server to \
another server or reference time source, such as a radio \
or satellite receiver or modem."
HOMEPAGE = "http://support.ntp.org"
SECTION = "console/network"
LICENSE = "NTP"
LIC_FILES_CHKSUM = "file://COPYRIGHT;md5=f41fedb22dffefcbfafecc85b0f79cfa"

DEPENDS = "pps-tools libevent"

SRC_URI = "http://www.eecis.udel.edu/~ntp/ntp_spool/ntp4/ntp-4.2/ntp-${PV}.tar.gz \
           file://ntp-4.2.4_p6-nano.patch \
           file://ntpd \
           file://ntp.conf \
           file://ntpdate \
           file://ntpdate.default \
           file://ntpdate.service \
           file://ntpd.service \
           file://sntp.service \
           file://sntp \
           file://ntpd.list \
           file://ntpdate-cron \
           file://ntpdate.cron \
"

PR = "r9"

SRC_URI[md5sum] = "60049f51e9c8305afe30eb22b711c5c6"
SRC_URI[sha256sum] = "583d0e1c573ace30a9c6afbea0fc52cae9c8c916dbc15c026e485a0dda4ba048"

inherit autotools update-rc.d useradd systemd pkgconfig

# The ac_cv_header_readline_history is to stop ntpdc depending on either
# readline or curses
EXTRA_OECONF += "--with-net-snmp-config=no \
                 --without-ntpsnmpd \
                 ac_cv_header_readline_history_h=no \
                 --with-yielding_select=yes \
                 --with-locfile=redhat \
		 --enable-ATOM \
                 "
CFLAGS_append = " -DPTYS_ARE_GETPT -DPTYS_ARE_SEARCHED"

USERADD_PACKAGES = "${PN}"
NTP_USER_HOME ?= "/var/lib/ntp"
USERADD_PARAM_${PN} = "--system --home-dir ${NTP_USER_HOME} \
                       --no-create-home \
                       --shell /bin/false --user-group ntp"

# NB: debug is default-enabled by NTP; keep it default-enabled here.
PACKAGECONFIG ??= "cap debug"
PACKAGECONFIG[openssl] = "--with-openssl-libdir=${STAGING_LIBDIR} \
                          --with-openssl-incdir=${STAGING_INCDIR} \
                          --with-crypto, \
                          --without-openssl --without-crypto, \
                          openssl"
PACKAGECONFIG[cap] = "--enable-linuxcaps,--disable-linuxcaps,libcap"
PACKAGECONFIG[readline] = "--with-lineeditlibs,--without-lineeditlibs,readline"
PACKAGECONFIG[debug] = "--enable-debugging,--disable-debugging"

do_install_append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 644 ${WORKDIR}/ntp.conf ${D}${sysconfdir}
    install -m 755 ${WORKDIR}/ntpd ${D}${sysconfdir}/init.d
    install -d ${D}${bindir}
    install -m 755 ${WORKDIR}/ntpdate ${D}${bindir}/ntpdate-sync
    install -m 755 ${WORKDIR}/ntpdate-cron ${D}${bindir}/ntpdate-cron
    install -D -m 0644 ${WORKDIR}/ntpdate.cron ${D}${sysconfdir}/cron.d/ntpdate

    install -m 755 -d ${D}${NTP_USER_HOME}
    chown ntp:ntp ${D}${NTP_USER_HOME}

    # Fix hardcoded paths in scripts
    sed -i 's!/usr/sbin/!${sbindir}/!g' ${D}${sysconfdir}/init.d/ntpd ${D}${bindir}/ntpdate-sync
    sed -i 's!/usr/bin/!${bindir}/!g' ${D}${sysconfdir}/init.d/ntpd ${D}${bindir}/ntpdate-sync
    sed -i 's!/etc/!${sysconfdir}/!g' ${D}${sysconfdir}/init.d/ntpd ${D}${bindir}/ntpdate-sync
    sed -i 's!/var/!${localstatedir}/!g' ${D}${sysconfdir}/init.d/ntpd ${D}${bindir}/ntpdate-sync
    sed -i 's!^PATH=.*!PATH=${base_sbindir}:${base_bindir}:${sbindir}:${bindir}!' ${D}${bindir}/ntpdate-sync
    sed -i 's!__bindir__!${bindir}!g' ${D}${bindir}/ntpdate-cron
    sed -i 's!__bindir__!${bindir}!g' ${D}${sysconfdir}/cron.d/ntpdate
    sed -i '1s,#!.*perl -w,#! ${bindir}/env perl,' ${D}${sbindir}/ntptrace
    sed -i '/use/i use warnings;' ${D}${sbindir}/ntptrace
    sed -i '1s,#!.*perl,#! ${bindir}/env perl,' ${D}${sbindir}/ntp-wait
    sed -i '/use/i use warnings;' ${D}${sbindir}/ntp-wait
    sed -i '1s,#!.*perl -w,#! ${bindir}/env perl,' ${D}${sbindir}/calc_tickadj
    sed -i '/use/i use warnings;' ${D}${sbindir}/calc_tickadj

    install -d ${D}/${sysconfdir}/default
    install -m 644 ${WORKDIR}/ntpdate.default ${D}${sysconfdir}/default/ntpdate
    install -m 0644 ${WORKDIR}/sntp ${D}${sysconfdir}/default/

    install -d ${D}/${sysconfdir}/network/if-up.d
    ln -s ${bindir}/ntpdate-sync ${D}/${sysconfdir}/network/if-up.d

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/ntpdate.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${WORKDIR}/ntpd.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${WORKDIR}/sntp.service ${D}${systemd_system_unitdir}/

    install -d ${D}${systemd_unitdir}/ntp-units.d
    install -m 0644 ${WORKDIR}/ntpd.list ${D}${systemd_unitdir}/ntp-units.d/60-ntpd.list

    if [ `ls -A ${D}${libexecdir} | wc -l` -eq 0 ]; then
        rm -rf ${D}${libexecdir}
    fi
}

PACKAGES += "ntpdate sntp ${PN}-tickadj ${PN}-utils ${PN}-perl"
# NOTE: you don't need ntpdate, use "ntpd -q -g -x"

# ntp originally includes tickadj. It's split off for inclusion in small firmware images on platforms
# with wonky clocks (e.g. OpenSlug)
RDEPENDS_${PN} = "${PN}-tickadj"
# Handle move from bin to utils package
RPROVIDES_${PN}-utils = "${PN}-bin"
RREPLACES_${PN}-utils = "${PN}-bin"
RCONFLICTS_${PN}-utils = "${PN}-bin"

SYSTEMD_PACKAGES = "${PN} ntpdate sntp"
SYSTEMD_SERVICE_${PN} = "ntpd.service"
SYSTEMD_SERVICE_ntpdate = "ntpdate.service"
SYSTEMD_SERVICE_sntp = "sntp.service"

RPROVIDES_${PN} += "${PN}-systemd"
RREPLACES_${PN} += "${PN}-systemd"
RCONFLICTS_${PN} += "${PN}-systemd"

RPROVIDES_ntpdate += "ntpdate-systemd"
RREPLACES_ntpdate += "ntpdate-systemd"
RCONFLICTS_ntpdate += "ntpdate-systemd"

RSUGGESTS_${PN} = "iana-etc"

FILES_${PN} = "${sbindir}/ntpd ${sysconfdir}/ntp.conf ${sysconfdir}/init.d/ntpd ${libdir} \
    ${NTP_USER_HOME} \
    ${systemd_unitdir}/ntp-units.d/60-ntpd.list \
"
FILES_${PN}-tickadj = "${sbindir}/tickadj"
FILES_${PN}-utils = "${sbindir}/ntptime ${sbindir}/ntpq"
FILES_${PN}-perl = "${sbindir} ${datadir}/ntp/lib"
RDEPENDS_${PN}-perl += "perl"
FILES_ntpdate = "${sbindir}/ntpdate \
    ${sysconfdir}/network/if-up.d/ntpdate-sync \
    ${bindir}/ntpdate-sync \
    ${sysconfdir}/default/ntpdate \
    ${systemd_system_unitdir}/ntpdate.service \
    ${bindir}/ntpdate-cron \
    ${sysconfdir}/cron.d/ntpdate \
"
FILES_sntp = "${sbindir}/sntp \
              ${sysconfdir}/default/sntp \
              ${systemd_system_unitdir}/sntp.service \
             "

CONFFILES_${PN} = "${sysconfdir}/ntp.conf"
CONFFILES_ntpdate = "${sysconfdir}/default/ntpdate ${sysconfdir}/cron.d/ntpdate"

INITSCRIPT_NAME = "ntpd"
# No dependencies, so just go in at the standard level (20)
INITSCRIPT_PARAMS = "defaults"

pkg_postinst_ntpdate() {
    if grep -q -s ntpdate $D/var/spool/cron/root; then
        echo "comment the old crontab entry"
        sed -i -e "/^[^#]/ s/\(.*\)\(.*ntpdate-sync.*$\)/#\1\2/g" $D/var/spool/cron/root
    fi
    if [ "x$D" != "x" ]; then
	exit 0
    fi
    systemctl status crond > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        systemctl restart crond
    else
        /etc/init.d/cron restart
    fi
}

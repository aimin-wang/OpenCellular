SUMMARY = "A full-featured SSL VPN solution via tun device."
HOMEPAGE = "http://openvpn.sourceforge.net"
SECTION = "console/network"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=5aac200199fde47501876cba7263cb0c"
DEPENDS = "lzo openssl iproute2 ${@bb.utils.contains('DISTRO_FEATURES', 'pam', 'libpam', '', d)}"

inherit autotools

PR = "r3"

SRC_URI = "http://swupdate.openvpn.org/community/releases/openvpn-${PV}.tar.gz \
           file://openvpn \
           file://openvpn-generator \
           file://openvpn@.service \
           file://openvpn.service"

SRC_URI[md5sum] = "6ca03fe0fd093e0d01601abee808835c"
SRC_URI[sha256sum] = "7baed2ff39c12e1a1a289ec0b46fcc49ff094ca58b8d8d5f29b36ac649ee5b26"

CFLAGS += "-fno-inline"

# I want openvpn to be able to read password from file (hrw)
EXTRA_OECONF += "--enable-password-save --enable-iproute2"
EXTRA_OECONF += "${@bb.utils.contains('DISTRO_FEATURES', 'pam', '', '--disable-plugin-auth-pam', d)}"

# Explicitly specify IPROUTE to bypass the configure-time check for /sbin/ip on the host.
EXTRA_OECONF += "IPROUTE=/sbin/ip"

do_install_append() {
    install -d ${D}/${sysconfdir}/init.d
    install -d ${D}/${sysconfdir}/openvpn
    install -m 755 ${WORKDIR}/openvpn ${D}/${sysconfdir}/init.d

    # systemd files
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}${systemd_unitdir}/system-generators
    install -m 0644 ${WORKDIR}/openvpn.service ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/openvpn@.service ${D}${systemd_system_unitdir}
    install -m 0755 ${WORKDIR}/openvpn-generator ${D}${systemd_unitdir}/system-generators
}

RDEPENDS_${PN} += "update-rc.d"
RRECOMMENDS_${PN} = "kernel-module-tun"

FILES_${PN}-dbg += "${libdir}/openvpn/plugins/.debug"

# Don't go through the systemd.bbclass as we do not want magic to happen
# during install and upgrade. Simply ship the files.
FILES_${PN} += "${systemd_unitdir}"

pkg_postinst_${PN} () {
    if [ "x$D" != "x" ]; then
        exit 1
    fi

    if [ -L /etc/rc2.d/S*openvpn ]; then
        update-rc.d -f openvpn remove
        if [ ! -L /etc/systemd/system/multi-user.target.wants/openvpn.service ]; then
            ln -s '/lib/systemd/system/openvpn.service' '/etc/systemd/system/multi-user.target.wants/openvpn.service'
        fi
    fi
}
